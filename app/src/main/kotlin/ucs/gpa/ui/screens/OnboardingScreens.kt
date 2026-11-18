package ucs.gpa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ucs.gpa.core.model.StudentProfile
import ucs.gpa.data.AppState
import ucs.gpa.data.AppStore
import ucs.gpa.data.DemoData
import ucs.gpa.ui.ScreenList
import ucs.gpa.ui.components.BrandMark
import ucs.gpa.ui.components.Overline
import ucs.gpa.ui.components.PrimaryButton
import ucs.gpa.ui.components.SecondaryButton
import ucs.gpa.ui.components.TopBar
import ucs.gpa.ui.components.UcsCard
import ucs.gpa.ui.theme.Radii
import ucs.gpa.ui.theme.Spacing
import ucs.gpa.ui.theme.Ucs

/** First launch: a welcome screen, then either the demo or a short setup. */
@Composable
fun OnboardingFlow(state: AppState.Ready, store: AppStore) {
    var setup by remember { mutableStateOf(false) }
    ucs.gpa.ui.LocalBackHandler.current(setup) { setup = false }
    if (setup) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            SetupScreen(store, onBack = { setup = false }, onCreated = {})
        }
    } else {
        WelcomeScreen(onDemo = { store.exploreDemo() }, onOwn = { setup = true })
    }
}

@Composable
fun WelcomeScreen(onDemo: () -> Unit, onOwn: () -> Unit) {
    val c = Ucs.colors
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 520.dp)
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .testTag("welcome"),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrandMark(34.dp)
            Column {
                Text("UCS", style = Ucs.type.titleSmall, color = c.ink)
                Text("University of Zambia Computer Science Society", style = Ucs.type.caption, color = c.ink3)
            }
        }
        Spacer(Modifier.height(40.dp))
        Text("GPA Planner", style = Ucs.type.screenTitle.copy(fontSize = Ucs.type.screenTitle.fontSize * 1.25f), color = c.ink)
        Spacer(Modifier.height(10.dp))
        Text(
            "Work out your GPA the way UNZA does, see where it puts your degree class, and find the grades you need for the class you want.",
            style = Ucs.type.body,
            color = c.ink2,
        )
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Feature(Icons.Outlined.Calculate, "Calculate", "Full and half courses count by their official credits, 30 and 15. Every step is shown.")
            Feature(Icons.Outlined.Timeline, "Track", "Each year's GPA, your cumulative GPA and your class, in one place.")
            Feature(Icons.Outlined.Flag, "Plan", "What you need for Distinction, Merit or Credit, and what different grades would do.")
            Feature(Icons.Outlined.Lock, "Private", "Your record stays on this phone. No account, nothing uploaded.")
        }
        Spacer(Modifier.height(36.dp))
        PrimaryButton("Explore the demo", onDemo, modifier = Modifier.fillMaxWidth().testTag("explore-demo"))
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Start my own record", onOwn, modifier = Modifier.fillMaxWidth().testTag("start-own"))
        Spacer(Modifier.height(14.dp))
        Text(
            "The demo uses ${DemoData.STUDENT_NAME}, a fictional Year 3 student. You can switch to your own record at any time.",
            style = Ucs.type.caption,
            color = c.ink3,
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    val c = Ucs.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Ucs.type.titleSmall, color = c.ink)
            Text(body, style = Ucs.type.bodySmall, color = c.ink2)
        }
    }
}

/** Creates the student's own record. Used on first launch and from Settings. */
@Composable
fun SetupScreen(store: AppStore, onBack: () -> Unit, onCreated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var programme by remember { mutableStateOf("BSc Computer Science") }
    var years by remember { mutableStateOf(4) }
    var current by remember { mutableStateOf(1) }
    var useCatalogue by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize()) {
        ScreenList(topBar = { TopBar("Your record", subtitle = "Kept separate from the demo", onBack = onBack) }) {
            item { ProfileFields(name, { name = it }, programme, { programme = it }, years, { years = it; if (current > it) current = it }, current, { current = it }) }
            item {
                UcsCard {
                    Overline("Courses")
                    Spacer(Modifier.height(8.dp))
                    Choice(
                        "Start with the Computer Science course list",
                        "Adds each year's courses for the Software Engineering stream with their full or half weight. You fill in your grades and edit anything that differs.",
                        selected = useCatalogue,
                        onClick = { useCatalogue = true },
                    )
                    Spacer(Modifier.height(8.dp))
                    Choice(
                        "Start empty",
                        "Add every course yourself. Best for other programmes or streams.",
                        selected = !useCatalogue,
                        onClick = { useCatalogue = false },
                    )
                }
            }
            item {
                PrimaryButton(
                    "Create my record",
                    onClick = {
                        store.createPersonalRecord(
                            StudentProfile(name = name.trim(), programme = programme.trim(), currentYearOfStudy = current, programmeYears = years),
                            useCatalogue,
                        )
                        onCreated()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("create-record"),
                )
            }
            item {
                Text(
                    "Your record is stored only on this phone. You can export a backup from Settings.",
                    style = Ucs.type.caption,
                    color = Ucs.colors.ink3,
                    modifier = Modifier.padding(horizontal = Spacing.xxs),
                )
            }
        }
    }
}

@Composable
private fun Choice(title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    val c = Ucs.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Radii.control)
            .border(1.dp, if (selected) c.primary else c.line, Radii.control)
            .background(if (selected) c.primarySoft.copy(alpha = 0.5f) else c.surface)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.primary, unselectedColor = c.lineStrong))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = Ucs.type.label, color = c.ink)
            Text(body, style = Ucs.type.caption, color = c.ink2)
        }
    }
}
