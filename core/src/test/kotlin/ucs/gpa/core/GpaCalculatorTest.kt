package ucs.gpa.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ucs.gpa.core.calc.CourseStatus
import ucs.gpa.core.calc.GpaCalculator
import ucs.gpa.core.calc.IssueCode
import ucs.gpa.core.calc.Severity
import ucs.gpa.core.rules.RepeatAttemptPolicy
import ucs.gpa.core.rules.TemporaryGradePolicy

class GpaCalculatorTest {

    @Test fun `official grade point values`() {
        val expected = mapOf("A+" to 5.0, "A" to 4.0, "B+" to 3.5, "B" to 3.0, "C+" to 2.37, "C" to 1.0, "S" to 1.0, "D+" to 0.0, "D" to 0.0, "LT" to 0.0, "NE" to 0.0)
        expected.forEach { (grade, points) ->
            val y = GpaCalculator.year(year(1, half(grade)), rules)
            assertClose(points, y.gpa)
            assertEquals(15, y.summary.countedCredits, "credits for $grade")
        }
    }

    @Test fun `full course is 30 credits and half course is 15`() {
        val y = GpaCalculator.year(year(1, full("A"), half("A")), rules)
        assertEquals(45, y.summary.countedCredits)
        assertEquals(30, y.courses[0].credits)
        assertEquals(15, y.courses[1].credits)
    }

    @Test fun `full course weighs twice a half course`() {
        // A (4.00) full + B (3.00) half = (120 + 45) / 45 = 3.6667, not the plain average 3.5
        val y = GpaCalculator.year(year(1, full("A"), half("B")), rules)
        assertClose(165.0 / 45.0, y.gpa)
        assertNotEquals(3.5, y.gpa)
    }

    @Test fun `a year with one full course and six half courses`() {
        // 6 half courses (A, B+, B, A, B+, B) + 1 full course (A):
        // halves 15 × 21 = 315, full 30 × 4 = 120 → 435 / 120 = 3.625
        val y = GpaCalculator.year(year(2, half("A"), half("B+"), half("B"), half("A"), half("B+"), half("B"), full("A")), rules)
        assertEquals(120, y.summary.countedCredits)
        assertEquals(43500L, y.summary.weightedHundredths)
        assertClose(3.625, y.gpa)
    }

    @Test fun `C plus is exact without floating point drift`() {
        val y = GpaCalculator.year(year(1, half("C+"), half("C+"), half("C+")), rules)
        assertEquals(3L * 15 * 237, y.summary.weightedHundredths)
        assertClose(2.37, y.gpa)
    }

    @Test fun `fail grades count with zero points and earn no credits`() {
        val y = GpaCalculator.year(year(1, full("A"), full("D")), rules)
        assertClose(2.0, y.gpa)
        assertEquals(60, y.summary.countedCredits)
        assertEquals(30, y.summary.earnedCredits)
    }

    @Test fun `S counts as 1_00 like the official table`() {
        val y = GpaCalculator.year(year(1, half("A"), half("S")), rules)
        assertClose(2.5, y.gpa)
    }

    @Test fun `ungraded courses are pending, not zero`() {
        val y = GpaCalculator.year(year(3, full("B"), half(null), full(null)), rules)
        assertClose(3.0, y.gpa)
        assertEquals(30, y.summary.countedCredits)
        assertEquals(45, y.summary.pendingCredits)
        assertEquals(CourseStatus.NOT_GRADED, y.courses[1].status)
        assertFalse(y.isComplete)
    }

    @Test fun `no graded courses gives no GPA instead of 0_00`() {
        val y = GpaCalculator.year(year(1, full(null), half(null)), rules)
        assertNull(y.gpa)
        assertNull(y.band)
        val empty = GpaCalculator.record(record(), rules)
        assertNull(empty.gpa)
        assertNull(empty.band)
    }

    @Test fun `temporary grades are left out and flagged by default`() {
        val r = GpaCalculator.record(record(year(1, full("A"), half("IN"))), rules)
        assertClose(4.0, r.gpa)
        val course = r.years[0].courses[1]
        assertEquals(CourseStatus.TEMPORARY, course.status)
        assertFalse(course.includedInGpa)
        assertTrue(course.isPending)
        assertTrue(r.issues.any { it.code == IssueCode.TEMPORARY_GRADE && it.severity == Severity.WARNING })
        // If the IN later counted as 0.00: 120 / 45 = 2.6667
        assertClose(120.0 / 45.0, r.gpaIfTemporaryCountedAsZero)
    }

    @Test fun `temporary grades can be counted as zero by policy`() {
        val strict = rules.copy(temporaryGradePolicy = TemporaryGradePolicy.COUNT_AS_ZERO)
        val r = GpaCalculator.record(record(year(1, full("A"), half("DF"))), strict)
        assertClose(120.0 / 45.0, r.gpa)
        assertNull(r.gpaIfTemporaryCountedAsZero)
    }

    @Test fun `excluded and zero-credit courses do not affect the GPA`() {
        val excluded = full("D").copy(countsTowardGpa = false)
        val nonCredit = custom("S", 0)
        val y = GpaCalculator.year(year(3, half("A"), excluded, nonCredit), rules)
        assertClose(4.0, y.gpa)
        assertEquals(15, y.summary.countedCredits)
        assertEquals(CourseStatus.EXCLUDED, y.courses[1].status)
        assertEquals(CourseStatus.EXCLUDED, y.courses[2].status)
    }

    @Test fun `custom credits are used when valid`() {
        val y = GpaCalculator.year(year(1, custom("A", 10), half("B")), rules)
        assertClose((10 * 4.0 + 15 * 3.0) / 25, y.gpa)
    }

    @Test fun `missing or impossible custom credits are reported, never guessed`() {
        val y = GpaCalculator.year(year(1, half("A"), custom("A", null), custom("A", 999)), rules)
        assertClose(4.0, y.gpa)
        assertEquals(15, y.summary.countedCredits)
        assertEquals(CourseStatus.INVALID, y.courses[1].status)
        assertEquals(CourseStatus.INVALID, y.courses[2].status)
        assertTrue(y.issues.any { it.code == IssueCode.MISSING_CREDITS && it.severity == Severity.ERROR })
        assertTrue(y.issues.any { it.code == IssueCode.INVALID_CREDITS })
    }

    @Test fun `unknown grades are errors and left out`() {
        val r = GpaCalculator.record(record(year(1, half("A"), half("E"))), rules)
        assertClose(4.0, r.gpa)
        assertTrue(r.hasErrors)
        assertEquals(CourseStatus.INVALID, r.years[0].courses[1].status)
    }

    @Test fun `grades are matched without case or spaces`() {
        val y = GpaCalculator.year(year(1, half(" b+ ")), rules)
        assertClose(3.5, y.gpa)
    }

    @Test fun `repeat attempts are left out by default`() {
        val original = full("D", code = "MAT 1100")
        val repeat = full("B", code = "MAT 1100").copy(isRepeat = true)
        val y = GpaCalculator.year(year(1, original, repeat, full("A")), rules)
        // The failed attempt stays (0 points, 30 credits); the repeat does not contribute.
        assertClose(120.0 / 60.0, y.gpa)
        assertEquals(CourseStatus.REPEAT_EXCLUDED, y.courses[1].status)
        assertTrue(y.issues.none { it.code == IssueCode.DUPLICATE_CODE })
    }

    @Test fun `repeat attempts can count as zero by policy`() {
        val strict = rules.copy(repeatAttemptPolicy = RepeatAttemptPolicy.COUNT_AS_ZERO)
        val y = GpaCalculator.year(year(1, full("A"), full("B").copy(isRepeat = true)), strict)
        assertClose(120.0 / 60.0, y.gpa)
    }

    @Test fun `duplicate course codes in a year are flagged`() {
        val y = GpaCalculator.year(year(2, half("A", code = "CSC 2000"), half("B", code = "csc2000")), rules)
        assertTrue(y.issues.any { it.code == IssueCode.DUPLICATE_CODE })
    }

    @Test fun `courses without a name are flagged but still count`() {
        val y = GpaCalculator.year(year(1, half("A").copy(code = "", title = "")), rules)
        assertClose(4.0, y.gpa)
        assertTrue(y.issues.any { it.code == IssueCode.MISSING_NAME })
    }

    @Test fun `cumulative GPA weights every credit, not every year`() {
        // Year 1: 120 credits at 4.00 (480 points). Year 2: 135 credits at 3.00 (405 points).
        val y1 = year(1, full("A"), full("A"), full("A"), full("A"))
        val y2 = year(2, full("B"), full("B"), half("B"), half("B"), half("B"), half("B"), half("B"))
        val r = GpaCalculator.record(record(y1, y2), rules)
        assertEquals(255, r.overall.countedCredits)
        assertClose(885.0 / 255.0, r.gpa)
        assertNotEquals(3.5, r.gpa!!, 1e-6) // the naive average of 4.00 and 3.00
    }

    @Test fun `cumulative after each year follows year order`() {
        val r = GpaCalculator.record(record(year(2, full("B")), year(1, full("A"))), rules)
        assertEquals(listOf(1, 2), r.years.map { it.year.yearOfStudy })
        assertClose(4.0, r.cumulativeAfterYear[0])
        assertClose(3.5, r.cumulativeAfterYear[1])
    }

    @Test fun `four-year record worked by hand`() {
        // Year 1: four full courses B+, B, A, B+ → (105+90+120+105)/120 = 3.50
        // Year 2: halves A, B+, B, A, B+, C+ and full A+ → 15×(4+3.5+3+4+3.5+2.37) + 150 = 455.55/120 = 3.79625
        // Year 3: full A, halves A, B+, B+, A, B, A, non-credit S → (120 + 15×22)/120 = 3.75
        // Year 4: fulls A and B+, halves A, B+, B+, B, A → (120+105 + 15×18)/135 = 495/135 = 3.6667
        val r = GpaCalculator.record(
            record(
                year(1, full("B+"), full("B"), full("A"), full("B+")),
                year(2, half("A"), half("B+"), half("B"), half("A"), half("B+"), half("C+"), full("A+")),
                year(3, full("A"), half("A"), half("B+"), half("B+"), half("A"), half("B"), half("A"), custom("S", 0)),
                year(4, full("A"), full("B+"), half("A"), half("B+"), half("B+"), half("B"), half("A")),
            ),
            rules,
        )
        assertClose(3.5, r.years[0].gpa)
        assertClose(3.79625, r.years[1].gpa)
        assertClose(3.75, r.years[2].gpa)
        assertClose(495.0 / 135.0, r.years[3].gpa)
        assertEquals(495, r.overall.countedCredits)
        // (420 + 455.55 + 450 + 495) / 495 = 1820.55 / 495 = 3.677878...
        assertClose(1820.55 / 495.0, r.gpa)
        assertEquals("merit", r.band!!.id)
    }
}
