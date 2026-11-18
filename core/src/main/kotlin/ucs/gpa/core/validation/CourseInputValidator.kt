package ucs.gpa.core.validation

import ucs.gpa.core.model.Course
import ucs.gpa.core.model.CourseLoad
import ucs.gpa.core.rules.AcademicRules

/** Raw values from the course form, before they become a [Course]. */
data class CourseInput(
    val code: String = "",
    val title: String = "",
    val load: CourseLoad = CourseLoad.HALF,
    val customCredits: String = "",
    val grade: String? = null,
    val countsTowardGpa: Boolean = true,
    val isRepeat: Boolean = false,
) {
    companion object {
        fun from(course: Course) = CourseInput(
            code = course.code,
            title = course.title,
            load = course.load,
            customCredits = course.customCredits?.toString() ?: "",
            grade = course.grade,
            countsTowardGpa = course.countsTowardGpa,
            isRepeat = course.isRepeat,
        )
    }
}

data class CourseInputCheck(
    val nameError: String? = null,
    val creditsError: String? = null,
    val gradeError: String? = null,
    val warnings: List<String> = emptyList(),
) {
    val isValid: Boolean get() = nameError == null && creditsError == null && gradeError == null
}

object CourseInputValidator {
    const val MAX_CODE = 16
    const val MAX_TITLE = 90

    fun validate(input: CourseInput, rules: AcademicRules, otherCodesInYear: Collection<String> = emptyList()): CourseInputCheck {
        val code = input.code.trim()
        val title = input.title.trim()
        val nameError = when {
            code.isEmpty() && title.isEmpty() -> "Enter a course code or title."
            code.length > MAX_CODE -> "Course codes are at most $MAX_CODE characters."
            title.length > MAX_TITLE -> "Titles are at most $MAX_TITLE characters."
            else -> null
        }

        val creditsError = if (input.load == CourseLoad.CUSTOM) {
            val n = input.customCredits.trim().toIntOrNull()
            when {
                input.customCredits.isBlank() -> "Enter the credit value."
                n == null -> "Credits must be a whole number."
                n < 0 || n > rules.maxCustomCredits -> "Credits must be between 0 and ${rules.maxCustomCredits}."
                else -> null
            }
        } else null

        val gradeError = input.grade?.takeIf { it.isNotBlank() }?.let { g ->
            if (rules.grade(g) == null) "\"$g\" is not a UNZA grade." else null
        }

        val warnings = buildList {
            val normalized = code.uppercase().replace(" ", "")
            if (normalized.isNotEmpty() && !input.isRepeat &&
                otherCodesInYear.any { it.trim().uppercase().replace(" ", "") == normalized }
            ) add("$code is already in this year. If this is a second attempt, mark it as a repeat.")
            if (input.load == CourseLoad.CUSTOM && input.customCredits.trim() == "0") add("A 0-credit course is left out of the GPA.")
        }
        return CourseInputCheck(nameError, creditsError, gradeError, warnings)
    }

    /** Builds the course from valid input. Call [validate] first. */
    fun toCourse(input: CourseInput, id: String, plannedGrade: String? = null): Course = Course(
        id = id,
        code = input.code.trim().take(MAX_CODE),
        title = input.title.trim().take(MAX_TITLE),
        load = input.load,
        customCredits = if (input.load == CourseLoad.CUSTOM) input.customCredits.trim().toIntOrNull() else null,
        grade = input.grade?.trim()?.takeIf { it.isNotEmpty() },
        countsTowardGpa = input.countsTowardGpa,
        isRepeat = input.isRepeat,
        plannedGrade = if (input.grade.isNullOrBlank()) plannedGrade else null,
    )
}
