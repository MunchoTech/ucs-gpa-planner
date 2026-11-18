package ucs.gpa.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ucs.gpa.core.model.CourseLoad
import ucs.gpa.core.validation.CourseInput
import ucs.gpa.core.validation.CourseInputValidator

class ValidationTest {

    @Test fun `a course needs a code or a title`() {
        assertNotNull(CourseInputValidator.validate(CourseInput(), rules).nameError)
        assertTrue(CourseInputValidator.validate(CourseInput(code = "CSC 2000"), rules).isValid)
        assertTrue(CourseInputValidator.validate(CourseInput(title = "Projects"), rules).isValid)
    }

    @Test fun `custom credits must be a whole number in range`() {
        fun check(text: String) = CourseInputValidator.validate(CourseInput(code = "X", load = CourseLoad.CUSTOM, customCredits = text), rules)
        assertNotNull(check("").creditsError)
        assertNotNull(check("abc").creditsError)
        assertNotNull(check("-5").creditsError)
        assertNotNull(check("121").creditsError)
        assertNull(check("20").creditsError)
        assertTrue(check("0").warnings.isNotEmpty())
    }

    @Test fun `standard loads ignore the custom credit field`() {
        assertTrue(CourseInputValidator.validate(CourseInput(code = "X", load = CourseLoad.FULL, customCredits = "junk"), rules).isValid)
    }

    @Test fun `unknown grades are rejected`() {
        assertNotNull(CourseInputValidator.validate(CourseInput(code = "X", grade = "E"), rules).gradeError)
        assertNull(CourseInputValidator.validate(CourseInput(code = "X", grade = "b+"), rules).gradeError)
    }

    @Test fun `duplicate codes warn unless marked as a repeat`() {
        val dup = CourseInputValidator.validate(CourseInput(code = "csc 2000"), rules, listOf("CSC 2000"))
        assertTrue(dup.isValid)
        assertEquals(1, dup.warnings.size)
        val repeat = CourseInputValidator.validate(CourseInput(code = "csc 2000", isRepeat = true), rules, listOf("CSC 2000"))
        assertTrue(repeat.warnings.isEmpty())
    }

    @Test fun `building a course trims input and drops planned grade once graded`() {
        val c = CourseInputValidator.toCourse(CourseInput(code = "  CSC 3600 ", title = " Software Engineering ", load = CourseLoad.FULL, grade = "A"), "id", plannedGrade = "B")
        assertEquals("CSC 3600", c.code)
        assertEquals("Software Engineering", c.title)
        assertNull(c.plannedGrade)
        assertFalse(c.isRepeat)
        val pending = CourseInputValidator.toCourse(CourseInput(code = "X"), "id2", plannedGrade = "B")
        assertEquals("B", pending.plannedGrade)
    }
}
