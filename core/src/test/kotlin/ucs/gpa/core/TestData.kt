package ucs.gpa.core

import ucs.gpa.core.model.AcademicYear
import ucs.gpa.core.model.Course
import ucs.gpa.core.model.CourseLoad
import ucs.gpa.core.model.PlannerTarget
import ucs.gpa.core.model.StudentRecord
import ucs.gpa.core.rules.UnzaRules

val rules = UnzaRules.current

private var counter = 0
private fun nextId() = "c${counter++}"

fun full(grade: String?, code: String = "FUL ${counter}") = Course(nextId(), code = code, title = "Full course", load = CourseLoad.FULL, grade = grade)
fun half(grade: String?, code: String = "HAL ${counter}") = Course(nextId(), code = code, title = "Half course", load = CourseLoad.HALF, grade = grade)
fun custom(grade: String?, credits: Int?) = Course(nextId(), code = "CUS ${counter}", title = "Custom", load = CourseLoad.CUSTOM, customCredits = credits, grade = grade)

fun year(n: Int, vararg courses: Course) = AcademicYear(id = "y$n", yearOfStudy = n, courses = courses.toList())

fun record(vararg years: AcademicYear, target: PlannerTarget = PlannerTarget()) =
    StudentRecord(id = "r", years = years.toList(), target = target)

fun assertClose(expected: Double, actual: Double?, tolerance: Double = 1e-9) {
    kotlin.test.assertNotNull(actual, "expected $expected but was null")
    kotlin.test.assertTrue(kotlin.math.abs(expected - actual) <= tolerance, "expected $expected but was $actual")
}
