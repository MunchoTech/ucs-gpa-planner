package ucs.gpa.data

import ucs.gpa.core.model.AcademicYear
import ucs.gpa.core.model.PlannerTarget
import ucs.gpa.core.model.StudentProfile
import ucs.gpa.core.model.StudentRecord

/**
 * A fictional student for the demo. Chanda Mwila is not a real person and these are not real
 * results. Years 1 and 2 are complete, Year 3 is in progress and Year 4 is planned, so every part
 * of the app has something to show.
 */
object DemoData {
    const val STUDENT_NAME = "Chanda Mwila"

    private val grades: Map<Int, Map<String, String>> = mapOf(
        1 to mapOf("PHY 1010" to "B+", "CHE 1000" to "B", "MAT 1100" to "A", "BIO 1400" to "B+"),
        2 to mapOf(
            "CSC 2000" to "A+", "CSC 2101" to "B+", "CSC 2111" to "B", "CSC 2202" to "B+",
            "CSC 2702" to "A", "CSC 2901" to "A", "CSC 2912" to "C+",
        ),
    )

    private val sessions = mapOf(1 to "2023/24", 2 to "2024/25", 3 to "2025/26", 4 to "2026/27")

    fun record(): StudentRecord = StudentRecord(
        id = "demo-chanda-mwila",
        isDemo = true,
        profile = StudentProfile(
            name = STUDENT_NAME,
            programme = Catalogue.PROGRAMME,
            currentYearOfStudy = 3,
            programmeYears = 4,
        ),
        target = PlannerTarget(bandId = "distinction"),
        years = (1..4).map { n ->
            AcademicYear(
                id = "demo-y$n",
                yearOfStudy = n,
                session = sessions[n],
                courses = Catalogue.forYear(n).map { c ->
                    c.toCourse("demo-y$n-${c.code.replace(" ", "").lowercase()}")
                        .copy(grade = grades[n]?.get(c.code))
                },
            )
        },
    )
}
