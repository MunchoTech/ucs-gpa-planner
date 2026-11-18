package ucs.gpa.ui.nav

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface Route {
    /** Top-level destinations shown in the bottom bar. */
    sealed interface Tab : Route

    data object Home : Tab
    data object Calculator : Tab
    data object Planner : Tab
    data object Insights : Tab

    data object Settings : Route
    data object Profile : Route
    data object Methodology : Route
    data object Setup : Route
    data class EditCourse(val yearId: String, val courseId: String?) : Route
    data class Breakdown(val yearId: String?) : Route

    companion object {
        val tabs: List<Tab> = listOf(Home, Calculator, Planner, Insights)
    }
}

/**
 * A small typed back stack. Kept in plain Kotlin state (no Android types) so the same navigation
 * runs in the Android app and in the JVM UI tests. The Android activity forwards the system back
 * gesture to [back].
 */
@Stable
class Navigator(initial: Route = Route.Home) {
    var stack: List<Route> by mutableStateOf(listOf(initial))
        private set

    /** +1 forward, -1 back, 0 tab switch. Used to pick the transition. */
    var direction: Int by mutableStateOf(0)
        private set

    /** The year shown in the calculator. Null means "the most relevant year". */
    var selectedYearId: String? by mutableStateOf(null)

    val current: Route get() = stack.last()

    val currentTab: Route.Tab? get() = stack.lastOrNull { it is Route.Tab } as Route.Tab?

    val canGoBack: Boolean get() = stack.size > 1 || current != Route.Home

    fun navigate(route: Route) {
        if (route == current) return
        direction = 1
        stack = stack + route
    }

    fun selectTab(tab: Route.Tab) {
        direction = 0
        stack = listOf(tab)
    }

    fun openYear(yearId: String) {
        selectedYearId = yearId
        selectTab(Route.Calculator)
    }

    /** Returns false when there is nowhere to go back to (the app should close). */
    fun back(): Boolean {
        if (stack.size > 1) {
            direction = -1
            stack = stack.dropLast(1)
            return true
        }
        if (current != Route.Home) {
            direction = -1
            stack = listOf(Route.Home)
            return true
        }
        return false
    }

    fun resetTo(route: Route) {
        direction = 0
        stack = listOf(route)
    }
}
