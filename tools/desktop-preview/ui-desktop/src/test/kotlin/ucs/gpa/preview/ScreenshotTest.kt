package ucs.gpa.preview

import org.junit.Test
import ucs.gpa.core.model.AcademicYear
import ucs.gpa.core.model.Course
import ucs.gpa.core.model.CourseLoad
import ucs.gpa.core.model.PlannerTarget
import ucs.gpa.core.model.StudentProfile
import ucs.gpa.core.model.StudentRecord
import ucs.gpa.data.DemoData
import ucs.gpa.data.Mode
import ucs.gpa.data.StoredState
import ucs.gpa.data.ThemePreference
import ucs.gpa.ui.nav.Navigator
import ucs.gpa.ui.nav.Route

/**
 * Renders every main screen of the real Compose UI to build/screenshots for visual review:
 * tall captures show a whole scrolling screen, phone captures show what fits on one screen.
 */
class ScreenshotTest {
    private val demo = StoredState(onboardingComplete = true)

    private fun shot(name: String, state: StoredState = demo, tall: Boolean = true, dark: Boolean = false, widthDp: Int = 393, heightDp: Int? = null, density: Float? = null, setup: (Navigator) -> Unit = {}) {
        val (store, _) = Harness.store(state)
        val nav = Navigator().also(setup)
        Harness.shoot(name, widthDp = widthDp, heightDp = heightDp ?: if (tall) 2300 else 852, density = density ?: if (tall) 2f else 2.75f) {
            Harness.App(store, nav, dark)
        }
    }

    @Test fun onboarding() {
        shot("01-welcome", state = StoredState(), tall = false)
        shot("02-setup", setup = { it.navigate(Route.Setup) }, tall = false)
    }

    @Test fun home() {
        shot("10-home-phone", tall = false)
        shot("11-home-full")
        shot("12-home-dark", tall = false, dark = true)
        shot("13-home-compact", tall = false, widthDp = 360, heightDp = 740, density = 3f)
        shot("14-home-tablet", tall = false, widthDp = 800, heightDp = 1280, density = 2f)
    }

    @Test fun calculator() {
        shot("20-calc-year2", setup = { it.selectTab(Route.Calculator); it.selectedYearId = "demo-y2" })
        shot("21-calc-year3-phone", tall = false, setup = { it.selectTab(Route.Calculator) })
        shot("22-calc-dark", tall = false, dark = true, setup = { it.selectTab(Route.Calculator); it.selectedYearId = "demo-y2" })
        shot("23-editor-existing", setup = { it.navigate(Route.EditCourse("demo-y2", "demo-y2-csc2912")) })
        shot("24-editor-new", tall = false, setup = { it.navigate(Route.EditCourse("demo-y3", null)) })
        shot("25-breakdown-year2", setup = { it.navigate(Route.Breakdown("demo-y2")) })
        shot("26-breakdown-cumulative", setup = { it.navigate(Route.Breakdown(null)) })
    }

    @Test fun planner() {
        shot("30-planner-full", setup = { it.selectTab(Route.Planner) })
        shot("31-planner-phone", tall = false, setup = { it.selectTab(Route.Planner) })
        // "A in my full courses and B+ in my half courses"
        val planned = DemoData.record().let { r ->
            r.copy(years = r.years.map { y ->
                y.copy(courses = y.courses.map { c ->
                    if (c.isGraded || (c.customCredits == 0)) c else c.copy(plannedGrade = if (c.load == CourseLoad.FULL) "A" else "B+")
                })
            })
        }
        shot("32-planner-whatif", state = demo.copy(demo = planned), setup = { it.selectTab(Route.Planner) })
        shot("33-planner-dark", tall = false, dark = true, setup = { it.selectTab(Route.Planner) })
        shot("34-planner-out-of-reach", state = personal(lowGrades()), setup = { it.selectTab(Route.Planner) })
        shot("35-planner-no-remaining", state = personal(completeRecord()), setup = { it.selectTab(Route.Planner) })
    }

    @Test fun insightsAndSettings() {
        shot("40-insights-full", setup = { it.selectTab(Route.Insights) })
        shot("41-insights-empty", state = personal(emptyRecord()), tall = false, setup = { it.selectTab(Route.Insights) })
        shot("50-settings", setup = { it.navigate(Route.Settings) })
        shot("51-methodology", setup = { it.navigate(Route.Methodology) })
        shot("52-settings-personal-dark", state = personal(completeRecord()).copy(theme = ThemePreference.DARK), setup = { it.navigate(Route.Settings) })
    }

    @Test fun emptyAndProblemStates() {
        shot("60-home-empty-record", state = personal(emptyRecord()), tall = false)
        shot("61-calc-empty-year", state = personal(emptyRecord()), tall = false, setup = { it.selectTab(Route.Calculator) })
        shot("62-home-with-issues", state = personal(problemRecord()), tall = false)
        shot("63-calc-with-issues", state = personal(problemRecord()), setup = { it.selectTab(Route.Calculator) })
    }

    // ---- fixtures (fictional) ----

    private fun personal(record: StudentRecord) = StoredState(onboardingComplete = true, mode = Mode.PERSONAL, personal = record)

    private fun course(id: String, code: String, title: String, load: CourseLoad, grade: String?) =
        Course(id = id, code = code, title = title, load = load, grade = grade)

    private fun emptyRecord() = StudentRecord(
        id = "p1",
        profile = StudentProfile(name = "Natasha Banda", programme = "BSc Computer Science", currentYearOfStudy = 1),
        years = (1..4).map { AcademicYear("p-y$it", it) },
    )

    private fun lowGrades() = StudentRecord(
        id = "p2",
        profile = StudentProfile(name = "Mutale Zulu", programme = "BSc Computer Science", currentYearOfStudy = 4),
        target = PlannerTarget(bandId = "distinction"),
        years = listOf(
            AcademicYear("l1", 1, courses = listOf(
                course("a", "PHY 1010", "Introductory Physics", CourseLoad.FULL, "C+"),
                course("b", "CHE 1000", "Introductory Chemistry", CourseLoad.FULL, "B"),
                course("c", "MAT 1100", "Foundation Mathematics", CourseLoad.FULL, "C"),
                course("d", "BIO 1400", "Cellular, Molecular Biology and Genetics", CourseLoad.FULL, "B"),
            )),
            AcademicYear("l2", 2, courses = listOf(
                course("e", "CSC 2000", "Computer Programming", CourseLoad.FULL, "B"),
                course("f", "CSC 2101", "Computer Systems", CourseLoad.HALF, "C+"),
                course("g", "CSC 2111", "Computer Architecture", CourseLoad.HALF, "B"),
                course("h", "CSC 2202", "Operating Systems", CourseLoad.HALF, "C+"),
            )),
            AcademicYear("l3", 3, courses = listOf(
                course("i", "CSC 3600", "Software Engineering", CourseLoad.FULL, null),
                course("j", "CSC 3011", "Algorithms and Complexity", CourseLoad.HALF, null),
            )),
        ),
    )

    private fun completeRecord() = StudentRecord(
        id = "p3",
        profile = StudentProfile(name = "Bwalya Tembo", programme = "BSc Computer Science", currentYearOfStudy = 2),
        years = listOf(
            AcademicYear("c1", 1, courses = listOf(
                course("a", "PHY 1010", "Introductory Physics", CourseLoad.FULL, "A"),
                course("b", "CHE 1000", "Introductory Chemistry", CourseLoad.FULL, "B+"),
                course("c", "MAT 1100", "Foundation Mathematics", CourseLoad.FULL, "A+"),
                course("d", "BIO 1400", "Cellular, Molecular Biology and Genetics", CourseLoad.FULL, "B"),
            )),
        ),
    )

    private fun problemRecord() = StudentRecord(
        id = "p4",
        profile = StudentProfile(name = "Test Record", programme = "BSc Computer Science", currentYearOfStudy = 1),
        years = listOf(
            AcademicYear("q1", 1, courses = listOf(
                course("a", "PHY 1010", "Introductory Physics", CourseLoad.FULL, "A"),
                course("b", "CHE 1000", "Introductory Chemistry", CourseLoad.FULL, "IN"),
                Course(id = "c", code = "MAT 1100", title = "Foundation Mathematics", load = CourseLoad.CUSTOM, customCredits = null, grade = "B"),
                course("d", "BIO 1400", "Cellular, Molecular Biology and Genetics", CourseLoad.FULL, "D"),
            )),
        ),
    )
}
