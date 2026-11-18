package ucs.gpa.data

import ucs.gpa.core.model.Course
import ucs.gpa.core.model.CourseLoad

data class CatalogueCourse(
    val code: String,
    val title: String,
    val load: CourseLoad,
    val customCredits: Int? = null,
) {
    fun toCourse(id: String) = Course(id = id, code = code, title = title, load = load, customCredits = customCredits)
}

/**
 * A starting course list for BSc Computer Science (Software Engineering stream) at UNZA, taken from
 * student transcripts. It is a convenience, not an official catalogue: courses and full/half
 * designations can change, and every entry is editable in the app.
 */
object Catalogue {
    const val PROGRAMME = "BSc Computer Science (Software Engineering)"

    val computerScience: Map<Int, List<CatalogueCourse>> = mapOf(
        1 to listOf(
            CatalogueCourse("PHY 1010", "Introductory Physics", CourseLoad.FULL),
            CatalogueCourse("CHE 1000", "Introductory Chemistry", CourseLoad.FULL),
            CatalogueCourse("MAT 1100", "Foundation Mathematics", CourseLoad.FULL),
            CatalogueCourse("BIO 1400", "Cellular, Molecular Biology and Genetics", CourseLoad.FULL),
        ),
        2 to listOf(
            CatalogueCourse("CSC 2000", "Computer Programming", CourseLoad.FULL),
            CatalogueCourse("CSC 2101", "Computer Systems", CourseLoad.HALF),
            CatalogueCourse("CSC 2111", "Computer Architecture", CourseLoad.HALF),
            CatalogueCourse("CSC 2202", "Operating Systems", CourseLoad.HALF),
            CatalogueCourse("CSC 2702", "Database and Information Management Systems", CourseLoad.HALF),
            CatalogueCourse("CSC 2901", "Discrete Structures", CourseLoad.HALF),
            CatalogueCourse("CSC 2912", "Numerical Analysis", CourseLoad.HALF),
        ),
        3 to listOf(
            CatalogueCourse("CSC 3600", "Software Engineering", CourseLoad.FULL),
            CatalogueCourse("CSC 3011", "Algorithms and Complexity", CourseLoad.HALF),
            CatalogueCourse("CSC 3301", "Programming Language Design and Implementation", CourseLoad.HALF),
            CatalogueCourse("CSC 3402", "Fundamentals of Artificial Intelligence", CourseLoad.HALF),
            CatalogueCourse("CSC 3612", "IT Project Management", CourseLoad.HALF),
            CatalogueCourse("CSC 3712", "Advanced Databases", CourseLoad.HALF),
            CatalogueCourse("CSC 3801", "Data Communication and Networks", CourseLoad.HALF),
            CatalogueCourse("CSC 3009", "Industrial Training", CourseLoad.CUSTOM, customCredits = 0),
        ),
        4 to listOf(
            CatalogueCourse("CSC 4004", "Projects", CourseLoad.FULL),
            CatalogueCourse("CSC 4630", "Advanced Software Engineering", CourseLoad.FULL),
            CatalogueCourse("CSC 4035", "Web Programming and Technologies", CourseLoad.HALF),
            CatalogueCourse("CSC 4505", "Graphics and Visual Computing", CourseLoad.HALF),
            CatalogueCourse("CSC 4631", "Software Testing and Maintenance", CourseLoad.HALF),
            CatalogueCourse("CSC 4642", "Software and Quality Assurance", CourseLoad.HALF),
            CatalogueCourse("CSC 4792", "Data Mining and Warehousing", CourseLoad.HALF),
        ),
    )

    fun forYear(yearOfStudy: Int): List<CatalogueCourse> = computerScience[yearOfStudy].orEmpty()
}
