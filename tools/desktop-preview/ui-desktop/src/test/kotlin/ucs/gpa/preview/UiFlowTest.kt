package ucs.gpa.preview

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import ucs.gpa.core.model.CourseLoad
import ucs.gpa.data.AppState
import ucs.gpa.data.AppStore
import ucs.gpa.data.InMemoryRecordStore
import ucs.gpa.data.StoredState
import ucs.gpa.ui.nav.Navigator
import ucs.gpa.ui.nav.Route

/** Drives the real Compose UI (JVM desktop renderer) through the main student journeys. */
@OptIn(ExperimentalTestApi::class)
class UiFlowTest {
    @get:Rule val rule = createComposeRule()

    private lateinit var store: AppStore
    private lateinit var backing: InMemoryRecordStore
    private val nav = Navigator()

    private fun launch(state: StoredState = StoredState(onboardingComplete = true)) {
        val (s, b) = Harness.store(state)
        store = s
        backing = b
        rule.setContent { Harness.App(store, nav) }
        rule.waitForIdle()
    }

    private fun ready() = store.state.value as AppState.Ready

    private fun scrollTo(tagOrText: String, byTag: Boolean = false) {
        rule.onNodeWithTag("screen-list").performScrollToNode(if (byTag) hasTestTag(tagOrText) else hasText(tagOrText))
    }

    @Test fun `first launch explains the app and opens the demo`() {
        launch(StoredState())
        rule.onNodeWithTag("welcome").assertIsDisplayed()
        rule.onNodeWithText("Chanda Mwila, a fictional Year 3 student", substring = true).assertExists()
        rule.onNodeWithTag("explore-demo").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("demo-banner").assertIsDisplayed()
        rule.onNodeWithTag("cumulative-gpa").assert(hasContentDescription("GPA 3.65"))
        assertTrue(ready().stored.onboardingComplete)
    }

    @Test fun `bottom navigation and back reach every main area`() {
        launch()
        rule.onNodeWithTag("tab-Calculator").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("year-tabs").assertIsDisplayed()
        rule.onNodeWithTag("tab-Planner").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("requirement").assertIsDisplayed()
        rule.onNodeWithTag("tab-Insights").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("insight-strongest").assertExists()
        rule.onNodeWithTag("tab-Home").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("standing-card").assertIsDisplayed()

        rule.onNodeWithTag("open-settings").performClick(); rule.waitForIdle()
        assertEquals(Route.Settings, nav.current)
        rule.runOnIdle { nav.back() }
        rule.waitForIdle()
        assertEquals(Route.Home, nav.current)
        rule.runOnIdle { nav.selectTab(Route.Planner); assertTrue(nav.back()) }
        assertEquals(Route.Home, nav.current)
        assertFalse(nav.back(), "back from Home closes the app")
    }

    @Test fun `changing a grade updates the year GPA and is saved`() {
        launch()
        rule.runOnIdle { nav.openYear("demo-y2") }
        rule.waitForIdle()
        rule.onNodeWithTag("year-gpa").assert(hasContentDescription("GPA 3.80"))

        scrollTo("Numerical Analysis")
        rule.onNodeWithText("Numerical Analysis").performClick()
        rule.waitForIdle()
        assertEquals(Route.EditCourse("demo-y2", "demo-y2-csc2912"), nav.current)

        scrollTo("grade-picker", byTag = true)
        rule.onNode(hasContentDescription("A, 4.00 points", substring = true)).performClick()
        scrollTo("effect", byTag = true)
        rule.onNodeWithTag("effect").assert(hasText("15 credits × 4.00 = 60.00 grade points"))
        scrollTo("save-course", byTag = true)
        rule.onNodeWithTag("save-course").performClick()
        rule.waitForIdle()

        // 455.55 − 35.55 + 60.00 = 480.00 over 120 credits
        assertEquals(Route.Calculator, nav.current)
        rule.onNodeWithTag("year-gpa").assert(hasContentDescription("GPA 4.00"))

        // persisted, and a fresh store reads the same result
        val saved = assertNotNull(backing.saved)
        assertEquals("A", saved.demo.year("demo-y2")!!.courses.first { it.id == "demo-y2-csc2912" }.grade)
        val (reloaded, _) = Harness.store(saved)
        assertEquals(4.0, (reloaded.state.value as AppState.Ready).derived.result.years[1].gpa!!, 1e-9)
    }

    @Test fun `invalid course input is rejected with a reason`() {
        launch()
        rule.runOnIdle { nav.navigate(Route.EditCourse("demo-y3", null)) }
        rule.waitForIdle()
        val before = ready().record.year("demo-y3")!!.courses.size

        rule.onNodeWithTag("save-top").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Enter a course code or title.").assertExists()
        assertEquals(before, ready().record.year("demo-y3")!!.courses.size)

        rule.onNodeWithTag("field-title").performTextInput("Seminar")
        rule.onNodeWithText("Other").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("save-top").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Enter the credit value.").assertExists()

        rule.onNodeWithTag("field-credits").performTextInput("2x0")   // non-digits are dropped → "20"
        rule.waitForIdle()
        rule.onNodeWithTag("save-top").performClick()
        rule.waitForIdle()
        val added = ready().record.year("demo-y3")!!.courses.last()
        assertEquals(before + 1, ready().record.year("demo-y3")!!.courses.size)
        assertEquals("Seminar", added.title)
        assertEquals(CourseLoad.CUSTOM, added.load)
        assertEquals(20, added.customCredits)
    }

    @Test fun `planner combinations and what-if grades project the final GPA`() {
        launch()
        rule.onNodeWithTag("tab-Planner").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("required").assert(hasText("3.85"))
        rule.onNodeWithTag("feasibility").assert(hasText("Achievable"))

        scrollTo("Try this in What if")
        rule.onAllNodesWithText("Try this in What if")[0].performClick()
        rule.waitForIdle()
        assertNotNull(ready().derived.projection)
        assertTrue(ready().derived.projection!!.meetsTarget!!)

        // "A in my full courses and B+ in my half courses"
        rule.runOnIdle { store.clearPlannedGrades() }
        scrollTo("set-full", byTag = true)
        rule.onNodeWithTag("set-full").onChildWithText("A").performClick()
        scrollTo("set-half", byTag = true)
        rule.onNodeWithTag("set-half").onChildWithText("B+").performClick()
        rule.waitForIdle()
        val p = assertNotNull(ready().derived.projection)
        assertTrue(p.isComplete)
        // (875.55 + 3×30×4.00 + 11×15×3.50) / 495 = 1813.05 / 495
        assertEquals(1813.05 / 495, p.gpa!!, 1e-9)
        assertFalse(p.meetsTarget!!)

        // planning never touches real grades
        assertTrue(ready().record.allCourses().filter { it.first.yearOfStudy >= 3 }.none { it.second.isGraded })

        scrollTo("target-merit", byTag = true)
        rule.onNodeWithTag("target-merit").performClick()
        rule.waitForIdle()
        scrollTo("required", byTag = true)
        rule.onNodeWithTag("required").assert(hasText("2.88"))
    }

    @Test fun `starting an own record keeps the demo separate`() {
        launch()
        rule.onNodeWithTag("open-settings").performClick(); rule.waitForIdle()
        rule.onNodeWithText("Start my own record").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("field-name").performTextInput("Mwila Test")
        scrollTo("create-record", byTag = true)
        rule.onNodeWithTag("create-record").performClick()
        rule.waitForIdle()

        assertEquals(Route.Home, nav.current)
        assertFalse(ready().isDemo)
        rule.onNodeWithText("Mwila Test").assertExists()
        rule.onNodeWithTag("cumulative-gpa").assert(hasContentDescription("No GPA yet"))
        assertEquals(0, rule.onAllNodesWithText("Demo record").fetchSemanticsNodes().size)
        assertEquals(4, ready().record.years.size)
        assertEquals("Chanda Mwila", ready().stored.demo.profile.name)
        assertNull(ready().record.years.flatMap { it.courses }.firstOrNull { it.isGraded })
    }

    @Test fun `missing information is flagged instead of silently counted`() {
        launch()
        val y1 = ready().record.year("demo-y1")!!
        rule.runOnIdle {
            store.saveCourse(y1.id, y1.courses[0].copy(load = CourseLoad.CUSTOM, customCredits = null))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("provisional").assertExists()
        rule.onNodeWithText("1 course needs attention").assertExists()
        assertTrue(ready().derived.result.hasErrors)
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.onChildWithText(text: String) =
    onChildren().filterToOne(hasContentDescription(text))
