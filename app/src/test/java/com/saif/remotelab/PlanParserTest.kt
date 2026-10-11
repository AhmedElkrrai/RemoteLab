package com.saif.remotelab

import com.saif.remotelab.data.StudyCondition
import com.saif.remotelab.data.TestType
import com.saif.remotelab.data.parsePlanLine
import com.saif.remotelab.data.parsePlanText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PlanParserTest {

    @Test
    fun readsATableLine() {
        val row = parsePlanLine("3  Spectrabact 625 mg  B240512  3M Acc  Assay  12/04/2026")!!

        assertEquals("Spectrabact", row.productName)
        assertEquals("625 mg", row.concentration)
        assertEquals("B240512", row.batchNo)
        assertEquals(LocalDate.of(2026, 4, 12), row.date)
        assertEquals(StudyCondition.Accelerated, row.condition)
        assertEquals(3, row.months)
        assertEquals(TestType.Assay, row.test)
    }

    @Test
    fun readsALabelledBatchAndMultiWordName() {
        val row = parsePlanLine("Thiodio Comp 2026-04-20 Batch No: 2301A Long term 12 M Related substances")!!

        assertEquals("Thiodio Comp", row.productName)
        assertEquals("2301A", row.batchNo)
        assertEquals(LocalDate.of(2026, 4, 20), row.date)
        assertEquals(StudyCondition.LongTerm, row.condition)
        assertEquals(12, row.months)
        assertEquals(TestType.Related, row.test)
    }

    @Test
    fun keepsRowsThatOnlyHaveProductBatchAndDate() {
        val row = parsePlanLine("Wincef 250115 5 May 2026")!!

        assertEquals("Wincef", row.productName)
        assertEquals("250115", row.batchNo)
        assertEquals(LocalDate.of(2026, 5, 5), row.date)
        assertNull(row.condition)
        assertNull(row.test)
        assertEquals(YearMonth.of(2026, 5), row.toRecord().monthPlan)
    }

    @Test
    fun skipsLinesWithoutADateOrAProduct() {
        assertNull(parsePlanLine("Product name   Batch No   Date of analysis"))
        assertNull(parsePlanLine("Issued: 01/04/2026"))
        assertNull(parsePlanLine("Zocilin 4.5 g B77 31/02/2026"))
    }

    @Test
    fun readsEveryMatchingLineOfADocument() {
        val text = """
            Stability Cabinet Plan
            Product  Batch  Date
            Wincef 250115 05/05/2026
            Zocilin 4.5 g B7701 06/05/2026

            Page 1 of 1
        """.trimIndent()

        assertEquals(listOf("Wincef", "Zocilin"), parsePlanText(text).map { it.productName })
    }
}
