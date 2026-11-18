package ucs.gpa.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ucs.gpa.data.AppState
import ucs.gpa.data.AppStore
import ucs.gpa.data.BackupExport
import ucs.gpa.data.ThemePreference
import ucs.gpa.ui.components.Hairline
import ucs.gpa.ui.nav.Navigator
import ucs.gpa.ui.nav.Route
import ucs.gpa.ui.screens.BreakdownScreen
import ucs.gpa.ui.screens.CalculatorScreen
import ucs.gpa.ui.screens.CourseEditorScreen
import ucs.gpa.ui.screens.HomeScreen
import ucs.gpa.ui.screens.InsightsScreen
import ucs.gpa.ui.screens.MethodologyScreen
import ucs.gpa.ui.screens.OnboardingFlow
import ucs.gpa.ui.screens.PlannerScreen
import ucs.gpa.ui.screens.ProfileScreen
import ucs.gpa.ui.screens.SettingsScreen
import ucs.gpa.ui.screens.SetupScreen
import ucs.gpa.ui.theme.BrandAssets
import ucs.gpa.ui.theme.Radii
import ucs.gpa.ui.theme.Spacing
import ucs.gpa.ui.theme.Ucs
import ucs.gpa.ui.theme.UcsFonts
import ucs.gpa.ui.theme.UcsTheme

/**
 * Lets the platform intercept the system back gesture for in-screen state (e.g. onboarding steps).
 * Android provides androidx.activity.compose.BackHandler; elsewhere it does nothing.
 */
val LocalBackHandler = androidx.compose.runtime.staticCompositionLocalOf<@Composable (Boolean, () -> Unit) -> Unit> { { _, _ -> } }

/** What the UI needs from the platform (Android activity, or a fake in tests). */
interface PlatformBridge {
    val appVersion: String
    fun exportBackup(export: BackupExport)
    fun requestBackupImport()
}

object NoopPlatform : PlatformBridge {
    override val appVersion = "dev"
    override fun exportBackup(export: BackupExport) {}
    override fun requestBackupImport() {}
}

/** Everything a screen needs. */
class ScreenContext(
    val state: AppState.Ready,
    val store: AppStore,
    val nav: Navigator,
    val platform: PlatformBridge,
) {
    val derived get() = state.derived
    val rules get() = store.rules
    val record get() = state.record
}

@Composable
fun UcsApp(
    store: AppStore,
    platform: PlatformBridge,
    navigator: Navigator,
    systemDark: Boolean,
    fonts: UcsFonts,
    brand: BrandAssets,
) {
    val state by store.state.collectAsState()
    val ready = state as? AppState.Ready
    val dark = when (ready?.stored?.theme) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        else -> systemDark
    }
    UcsTheme(dark = dark, fonts = fonts, brand = brand) {
        Box(Modifier.fillMaxSize().background(Ucs.colors.canvas)) {
            if (ready == null) {
                Box(Modifier.fillMaxSize().testTag("loading"), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Ucs.colors.primary, strokeWidth = 2.dp)
                }
            } else if (!ready.stored.onboardingComplete) {
                OnboardingFlow(ready, store)
            } else {
                MainShell(ScreenContext(ready, store, navigator, platform))
            }
        }
    }
}

@Composable
private fun MainShell(ctx: ScreenContext) {
    val nav = ctx.nav
    val snackbar = remember { SnackbarHostState() }
    val notice = ctx.state.notice
    LaunchedEffect(notice?.id) {
        if (notice != null) {
            snackbar.showSnackbar(notice.message)
            ctx.store.dismissNotice()
        }
    }
    val holder = rememberSaveableStateHolder()
    val route = nav.current

    Scaffold(
        containerColor = Ucs.colors.canvas,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(data, containerColor = Ucs.colors.ink, contentColor = Ucs.colors.canvas, shape = Radii.control)
            }
        },
        bottomBar = { if (route is Route.Tab) BottomBar(nav) },
    ) { padding ->
        AnimatedContent(
            targetState = route,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding().fillMaxSize(),
            transitionSpec = {
                val d = nav.direction
                if (d == 0) {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                } else {
                    (slideInHorizontally(tween(240)) { w -> d * w / 10 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(200)) { w -> -d * w / 12 } + fadeOut(tween(160)))
                }
            },
            label = "route",
        ) { r ->
            holder.SaveableStateProvider(r.toString()) {
                RouteContent(r, ctx)
            }
        }
    }
}

@Composable
private fun RouteContent(route: Route, ctx: ScreenContext) {
    when (route) {
        Route.Home -> HomeScreen(ctx)
        Route.Calculator -> CalculatorScreen(ctx)
        Route.Planner -> PlannerScreen(ctx)
        Route.Insights -> InsightsScreen(ctx)
        Route.Settings -> SettingsScreen(ctx)
        Route.Profile -> ProfileScreen(ctx)
        Route.Methodology -> MethodologyScreen(ctx)
        Route.Setup -> SetupScreen(
            store = ctx.store,
            onBack = { ctx.nav.back() },
            onCreated = { ctx.nav.resetTo(Route.Home) },
        )
        is Route.EditCourse -> CourseEditorScreen(ctx, route.yearId, route.courseId)
        is Route.Breakdown -> BreakdownScreen(ctx, route.yearId)
    }
}

private data class TabItem(val tab: Route.Tab, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabItems = listOf(
    TabItem(Route.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    TabItem(Route.Calculator, "Calculator", Icons.Outlined.Calculate, Icons.Filled.Calculate),
    TabItem(Route.Planner, "Planner", Icons.Outlined.Flag, Icons.Filled.Flag),
    TabItem(Route.Insights, "Insights", Icons.Outlined.Insights, Icons.Filled.Insights),
)

@Composable
private fun BottomBar(nav: Navigator) {
    val c = Ucs.colors
    Column {
        Hairline()
        NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
            tabItems.forEach { item ->
                val selected = nav.currentTab == item.tab
                NavigationBarItem(
                    selected = selected,
                    onClick = { nav.selectTab(item.tab) },
                    icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                    label = { Text(item.label, style = Ucs.type.caption) },
                    modifier = Modifier.testTag("tab-${item.label}"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = c.primary,
                        selectedTextColor = c.ink,
                        indicatorColor = c.primarySoft,
                        unselectedIconColor = c.ink3,
                        unselectedTextColor = c.ink3,
                    ),
                )
            }
        }
    }
}

/** Standard scrolling screen: top bar plus a centred column (max 640dp wide on tablets). */
@Composable
fun ScreenList(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        ContentWidth { topBar() }
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 640.dp).testTag("screen-list"),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/** A confirmation dialog with a clear consequence and a single primary action. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val c = Ucs.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = Radii.card,
        title = { Text(title, style = Ucs.type.title, color = c.ink) },
        text = { Text(text, style = Ucs.type.bodySmall, color = c.ink2) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, style = Ucs.type.label, color = if (destructive) c.danger else c.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = Ucs.type.label, color = c.ink2) }
        },
    )
}

/** Holds which confirmation, if any, is open. */
class DialogState<T> {
    var value: T? by mutableStateOf(null)
}

@Composable
fun <T> rememberDialogState(): DialogState<T> = remember { DialogState() }

@Composable
internal fun ContentWidth(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 640.dp)) { content() }
}
