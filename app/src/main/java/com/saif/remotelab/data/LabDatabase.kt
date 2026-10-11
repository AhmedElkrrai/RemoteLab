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

    @Query("SELECT * FROM plan_records WHERE productName LIKE '%' || :query || '%' ORDER BY dateOfAnalysis DESC")
    fun search(query: String): Flow<List<PlanRecord>>

    @Query("SELECT * FROM plan_records WHERE monthPlan = :month ORDER BY dateOfAnalysis")
    fun recordsOfMonth(month: YearMonth): Flow<List<PlanRecord>>

    @Query("SELECT * FROM plan_records WHERE dateOfAnalysis = :date ORDER BY productName")
    fun recordsOn(date: LocalDate): Flow<List<PlanRecord>>

    @Insert
    suspend fun insertRecord(record: PlanRecord)

    @Query("DELETE FROM plan_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)
}

@Database(entities = [Instrument::class, Run::class, PlanRecord::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LabDatabase : RoomDatabase() {
    abstract fun dao(): LabDao

    companion object {
        fun create(context: Context): LabDatabase =
            Room.databaseBuilder(context.applicationContext, LabDatabase::class.java, "thabat.db").build()
    }
}
