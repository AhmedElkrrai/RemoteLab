package com.saif.remotelab.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

class Converters {
    // ISO strings, so text ordering in SQL matches date ordering.
    @TypeConverter fun dateToString(value: LocalDate): String = value.toString()
    @TypeConverter fun stringToDate(value: String): LocalDate = LocalDate.parse(value)
    @TypeConverter fun monthToString(value: YearMonth): String = value.toString()
    @TypeConverter fun stringToMonth(value: String): YearMonth = YearMonth.parse(value)

    @TypeConverter
    fun monthsToString(value: Set<Int>): String = value.sorted().joinToString(",")

    @TypeConverter
    fun stringToMonths(value: String): Set<Int> =
        value.split(',').filter { it.isNotBlank() }.map { it.toInt() }.toSet()

    @TypeConverter
    fun testsToString(value: Set<TestType>): String = value.sortedBy { it.ordinal }.joinToString(",") { it.name }

    @TypeConverter
    fun stringToTests(value: String): Set<TestType> =
        value.split(',').filter { it.isNotBlank() }.map { TestType.valueOf(it) }.toSet()
}

@Dao
interface LabDao {
    @Query("SELECT * FROM instruments ORDER BY sortOrder")
    fun instruments(): Flow<List<Instrument>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInstruments(instruments: List<Instrument>)

    @Update
    suspend fun updateInstrument(instrument: Instrument)

    @Query("SELECT * FROM runs WHERE state != 'Finished' ORDER BY createdAt")
    fun activeRuns(): Flow<List<Run>>

    @Query(
        "SELECT * FROM runs WHERE state = 'Finished' " +
            "AND (productName LIKE '%' || :query || '%' OR batchNo LIKE '%' || :query || '%') " +
            "ORDER BY finishedAt DESC"
    )
    fun finishedRuns(query: String): Flow<List<Run>>

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun run(id: Long): Run?

    @Insert
    suspend fun insertRun(run: Run)

    @Update
    suspend fun updateRun(run: Run)

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteRun(id: Long)

    @Query("SELECT monthPlan, COUNT(*) AS count FROM plan_records GROUP BY monthPlan ORDER BY monthPlan DESC")
    fun months(): Flow<List<MonthSummary>>

    @Query(
        "SELECT * FROM plan_records " +
            "WHERE productName LIKE '%' || :query || '%' OR batchNo LIKE '%' || :query || '%' " +
            "ORDER BY dateOfAnalysis DESC"
    )
    fun search(query: String): Flow<List<PlanRecord>>

    @Query("SELECT * FROM plan_records WHERE monthPlan = :month ORDER BY dateOfAnalysis")
    fun recordsOfMonth(month: YearMonth): Flow<List<PlanRecord>>

    @Query("SELECT * FROM plan_records WHERE dateOfAnalysis = :date ORDER BY productName")
    fun recordsOn(date: LocalDate): Flow<List<PlanRecord>>

    @Query("SELECT * FROM plan_records WHERE dateOfAnalysis = :date")
    suspend fun recordsOnOnce(date: LocalDate): List<PlanRecord>

    @Insert
    suspend fun insertRecord(record: PlanRecord)

    @Query("DELETE FROM plan_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)
}

@Database(entities = [Instrument::class, Run::class, PlanRecord::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LabDatabase : RoomDatabase() {
    abstract fun dao(): LabDao

    companion object {
        /**
         * runs.months changed from a single nullable number to a comma-separated list, and
         * plan_records.condition / test became nullable. SQLite cannot alter either in place,
         * so both tables are rebuilt.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE runs_new (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`instrumentId` TEXT NOT NULL, `productName` TEXT NOT NULL, `concentration` TEXT NOT NULL, " +
                        "`batchNo` TEXT NOT NULL, `condition` TEXT NOT NULL, `months` TEXT NOT NULL, " +
                        "`tests` TEXT NOT NULL, `state` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`startedAt` INTEGER, `finishedAt` INTEGER)"
                )
                db.execSQL(
                    "INSERT INTO runs_new SELECT id, instrumentId, productName, concentration, batchNo, condition, " +
                        "COALESCE(CAST(months AS TEXT), ''), tests, state, createdAt, startedAt, finishedAt FROM runs"
                )
                db.execSQL("DROP TABLE runs")
                db.execSQL("ALTER TABLE runs_new RENAME TO runs")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_runs_instrumentId` ON `runs` (`instrumentId`)")

                db.execSQL(
                    "CREATE TABLE plan_records_new (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`productName` TEXT NOT NULL, `concentration` TEXT NOT NULL, `batchNo` TEXT NOT NULL, " +
                        "`dateOfAnalysis` TEXT NOT NULL, `condition` TEXT, `months` INTEGER, `test` TEXT, " +
                        "`monthPlan` TEXT NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO plan_records_new SELECT id, productName, concentration, batchNo, dateOfAnalysis, " +
                        "condition, months, test, monthPlan FROM plan_records"
                )
                db.execSQL("DROP TABLE plan_records")
                db.execSQL("ALTER TABLE plan_records_new RENAME TO plan_records")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_records_productName` ON `plan_records` (`productName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_records_monthPlan` ON `plan_records` (`monthPlan`)")
            }
        }

        fun create(context: Context): LabDatabase =
            Room.databaseBuilder(context.applicationContext, LabDatabase::class.java, "thabat.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
