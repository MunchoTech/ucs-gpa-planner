package ucs.gpa.core.model

import kotlinx.serialization.Serializable

/**
 * How much a course weighs. UNZA defines a full course as 30 credits and a half course as 15
 * (see [ucs.gpa.core.rules.UnzaRules]). [CUSTOM] covers courses with a different credit value,
 * including non-credit courses (0 credits).
 */
@Serializable
enum class CourseLoad { FULL, HALF, CUSTOM }

/**
 * One course attempt as the student entered it.
 *
 * @property grade the grade code as shown on the transcript (e.g. "B+"), or null if not graded yet.
 * @property countsTowardGpa false for courses the student or the rules leave out (e.g. Industrial Training).
 * @property isRepeat true when this entry is a repeated attempt of a course taken before.
 * @property plannedGrade a hypothetical grade used only by the planner; never mixed with real grades.
 */
@Serializable
data class Course(
    val id: String,
    val code: String = "",
    val title: String = "",
    val load: CourseLoad = CourseLoad.HALF,
    val customCredits: Int? = null,
    val grade: String? = null,
    val countsTowardGpa: Boolean = true,
    val isRepeat: Boolean = false,
    val plannedGrade: String? = null,
) {
    val displayName: String
        get() = listOf(code.trim(), title.trim()).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "Untitled course" }

    val shortTitle: String
        get() = title.trim().ifEmpty { code.trim() }.ifEmpty { "Untitled course" }

    val isGraded: Boolean get() = !grade.isNullOrBlank()
}

@Serializable
data class AcademicYear(
    val id: String,
    val yearOfStudy: Int,
    val session: String? = null,
    val courses: List<Course> = emptyList(),
) {
    val label: String get() = "Year $yearOfStudy"
}

@Serializable
data class StudentProfile(
    val name: String = "",
    val programme: String = "",
    val currentYearOfStudy: Int = 1,
    val programmeYears: Int = 4,
)

/**
 * The planner target: either a classification band (by id) or a custom GPA.
 * [estimatedRemainingCredits] is used only when no upcoming courses have been entered.
 */
@Serializable
data class PlannerTarget(
    val bandId: String? = "distinction",
    val customGpa: Double? = null,
    val estimatedRemainingCredits: Int? = null,
)

@Serializable
data class StudentRecord(
    val id: String,
    val profile: StudentProfile = StudentProfile(),
    val years: List<AcademicYear> = emptyList(),
    val target: PlannerTarget = PlannerTarget(),
    val isDemo: Boolean = false,
) {
    val sortedYears: List<AcademicYear> get() = years.sortedBy { it.yearOfStudy }

    fun year(id: String): AcademicYear? = years.firstOrNull { it.id == id }

    fun updateYear(id: String, transform: (AcademicYear) -> AcademicYear): StudentRecord =
        copy(years = years.map { if (it.id == id) transform(it) else it })

    fun allCourses(): List<Pair<AcademicYear, Course>> = sortedYears.flatMap { y -> y.courses.map { y to it } }
}
