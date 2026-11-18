package ucs.gpa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ucs.gpa.core.format.Format
import ucs.gpa.core.rules.Provenance
import ucs.gpa.data.Mode
import ucs.gpa.data.ThemePreference
import ucs.gpa.ui.ConfirmDialog
import ucs.gpa.ui.ScreenContext
import ucs.gpa.ui.ScreenList
import ucs.gpa.ui.components.BrandMark
import ucs.gpa.ui.components.Hairline
import ucs.gpa.ui.components.ListRow
import ucs.gpa.ui.components.Overline
import ucs.gpa.ui.components.Pill
import ucs.gpa.ui.components.PrimaryButton
import ucs.gpa.ui.components.SegmentedControl
import ucs.gpa.ui.components.TopBar
import ucs.gpa.ui.components.UcsCard
import ucs.gpa.ui.components.bandColor
import ucs.gpa.ui.nav.Route
import ucs.gpa.ui.rememberDialogState
import ucs.gpa.ui.theme.Spacing
import ucs.gpa.ui.theme.Ucs

private enum class Confirm { RESET_DEMO, DELETE_RECORD, IMPORT_REPLACE }

@Composable
fun SettingsScreen(ctx: ScreenContext) {
    val stored = ctx.state.stored
    val profile = ctx.record.profile
    val dialog = rememberDialogState<Confirm>()
    val c = Ucs.colors

    ScreenList(topBar = { TopBar("Settings", onBack = { ctx.nav.back() }) }) {
        item {
            Group("Profile") {
                ListRow(
                    profile.name.ifBlank { "Your profile" },
                    subtitle = profileLine(profile) + if (ctx.state.isDemo) " · demo" else "",
                    icon = Icons.Outlined.Person,
                    onClick = { ctx.nav.navigate(Route.Profile) },
                )
            }
        }
        item {
            Group("Record") {
                if (ctx.state.isDemo) {
                    if (stored.personal != null) {
                        ListRow("Switch to my record", subtitle = stored.personal!!.profile.name.ifBlank { "Your own results" }, icon = Icons.Outlined.SwapHoriz, onClick = { ctx.store.switchMode(Mode.PERSONAL) })
                    } else {
                        ListRow("Start my own record", subtitle = "Kept separate from the demo", icon = Icons.Outlined.Person, onClick = { ctx.nav.navigate(Route.Setup) })
                    }
                    Hairline(Modifier.padding(start = 50.dp))
                    ListRow("Reset demo data", subtitle = "Undo any changes to the demo", icon = Icons.Outlined.Restore, onClick = { dialog.value = Confirm.RESET_DEMO })
                } else {
                    ListRow("View the demo", subtitle = "Your record stays as it is", icon = Icons.Outlined.Science, onClick = { ctx.store.switchMode(Mode.DEMO) })
                }
            }
        }
        item {
            Group("Appearance") {
                Column(Modifier.padding(Spacing.md)) {
                    SegmentedControl(
                        listOf("System", "Light", "Dark"),
                        when (stored.theme) { ThemePreference.SYSTEM -> 0; ThemePreference.LIGHT -> 1; ThemePreference.DARK -> 2 },
                        onSelect = { ctx.store.setTheme(ThemePreference.entries[it]) },
                        modifier = Modifier.testTag("theme"),
                    )
                }
            }
        }
        item {
            Group("Backup") {
                ListRow(
                    "Export a backup",
                    subtitle = if (stored.personal == null) "Available once you have your own record" else "Save your record as a file",
                    icon = Icons.Outlined.FileDownload,
                    onClick = if (stored.personal == null) null else ({ ctx.store.exportBackup()?.let(ctx.platform::exportBackup) }),
                    titleColor = if (stored.personal == null) c.ink3 else c.ink,
                )
                Hairline(Modifier.padding(start = 50.dp))
                ListRow(
                    "Restore from a backup",
                    subtitle = "Replace your record with a saved file",
                    icon = Icons.Outlined.FileUpload,
                    onClick = { if (stored.personal != null) dialog.value = Confirm.IMPORT_REPLACE else ctx.platform.requestBackupImport() },
                )
                if (stored.personal != null) {
                    Hairline(Modifier.padding(start = 50.dp))
                    ListRow(
                        "Delete my record",
                        subtitle = "Removes your results from this phone",
                        icon = Icons.Outlined.DeleteOutline,
                        titleColor = c.danger,
                        onClick = { dialog.value = Confirm.DELETE_RECORD },
                    )
                }
            }
        }
        item {
            UcsCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Lock, null, tint = c.primary, modifier = Modifier.size(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Your data stays on this phone", style = Ucs.type.titleSmall, color = c.ink)
                        Text(
                            "Results are saved in the app's private storage, which other apps and other students cannot read. " +
                                "There are no accounts and nothing is uploaded. A backup is a file you choose where to keep. " +
                                "Uninstalling the app deletes your record, so export a backup first.",
                            style = Ucs.type.bodySmall,
                            color = c.ink2,
                        )
                    }
                }
            }
        }
        item {
            Group("About") {
                ListRow("How GPA is calculated", subtitle = "UNZA rules, sources and open questions", icon = Icons.Outlined.Functions, onClick = { ctx.nav.navigate(Route.Methodology) })
                Hairline(Modifier.padding(start = 50.dp))
                Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrandMark(28.dp)
                    Column(Modifier.weight(1f)) {
                        Text("UCS GPA Planner", style = Ucs.type.label, color = c.ink)
                        Text("Version ${ctx.platform.appVersion} · University of Zambia Computer Science Society", style = Ucs.type.caption, color = c.ink3)
                    }
                }
            }
        }
        item {
            Text(
                "An unofficial planning tool. Your official results are on the UNZA Student Information System.",
                style = Ucs.type.caption,
                color = c.ink3,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    when (dialog.value) {
        Confirm.RESET_DEMO -> ConfirmDialog(
            "Reset the demo?", "Chanda Mwila's results go back to how they started. Your own record is not affected.", "Reset",
            onConfirm = { ctx.store.resetDemo() }, onDismiss = { dialog.value = null },
        )
        Confirm.DELETE_RECORD -> ConfirmDialog(
            "Delete your record?", "Your courses and grades are removed from this phone. This cannot be undone unless you have a backup.", "Delete",
            destructive = true, onConfirm = { ctx.store.deletePersonalRecord() }, onDismiss = { dialog.value = null },
        )
        Confirm.IMPORT_REPLACE -> ConfirmDialog(
            "Replace your record?", "Restoring a backup replaces the record on this phone with the one in the file.", "Choose file",
            onConfirm = { ctx.platform.requestBackupImport() }, onDismiss = { dialog.value = null },
        )
        null -> {}
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Overline(title, modifier = Modifier.padding(start = 4.dp))
        UcsCard(padding = PaddingValues(0.dp)) { content() }
    }
}

@Composable
fun ProfileScreen(ctx: ScreenContext) {
    val profile = ctx.record.profile
    var name by remember { mutableStateOf(profile.name) }
    var programme by remember { mutableStateOf(profile.programme) }
    var years by remember { mutableStateOf(profile.programmeYears) }
    var current by remember { mutableStateOf(profile.currentYearOfStudy) }
    ScreenList(topBar = { TopBar("Profile", subtitle = if (ctx.state.isDemo) "Demo record" else null, onBack = { ctx.nav.back() }) }) {
        item { ProfileFields(name, { name = it }, programme, { programme = it }, years, { years = it; if (current > it) current = it }, current, { current = it }) }
        item {
            PrimaryButton("Save", onClick = {
                ctx.store.updateProfile(profile.copy(name = name.trim(), programme = programme.trim(), programmeYears = years, currentYearOfStudy = current))
                ctx.nav.back()
            }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun ProfileFields(
    name: String, onName: (String) -> Unit,
    programme: String, onProgramme: (String) -> Unit,
    years: Int, onYears: (Int) -> Unit,
    current: Int, onCurrent: (Int) -> Unit,
) {
    UcsCard {
        Field(name, { onName(it.take(60)) }, "Name (optional)", placeholder = "Shown only on this phone", tag = "field-name")
        Spacer(Modifier.height(10.dp))
        Field(programme, { onProgramme(it.take(80)) }, "Programme", placeholder = "BSc Computer Science", tag = "field-programme")
        Spacer(Modifier.height(16.dp))
        Overline("Programme length")
        Spacer(Modifier.height(8.dp))
        SegmentedControl(listOf("4 years", "5 years"), if (years == 5) 1 else 0, onSelect = { onYears(if (it == 1) 5 else 4) })
        Spacer(Modifier.height(16.dp))
        Overline("Current year of study")
        Spacer(Modifier.height(8.dp))
        SegmentedControl((1..years).map { "Year $it" }, (current - 1).coerceIn(0, years - 1), onSelect = { onCurrent(it + 1) }, modifier = Modifier.testTag("field-current-year"))
    }
}

@Composable
fun MethodologyScreen(ctx: ScreenContext) {
    val rules = ctx.rules
    val c = Ucs.colors
    ScreenList(topBar = { TopBar("How GPA is calculated", onBack = { ctx.nav.back() }) }) {
        item {
            UcsCard {
                Overline("The formula")
                Spacer(Modifier.height(8.dp))
                Text("GPA = Σ (credits × grade points) ÷ Σ credits", style = Ucs.type.mono.copy(fontSize = Ucs.type.label.fontSize), color = c.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Each course's credits are multiplied by its grade points. The products are added up and divided by the total credits. " +
                        "Your degree class comes from this GPA over every course in every year.",
                    style = Ucs.type.bodySmall, color = c.ink2,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column { Text("Full course", style = Ucs.type.caption, color = c.ink3); Text("${rules.fullCourseCredits} credits", style = Ucs.type.mono, color = c.ink) }
                    Column { Text("Half course", style = Ucs.type.caption, color = c.ink3); Text("${rules.halfCourseCredits} credits", style = Ucs.type.mono, color = c.ink) }
                    Column { Text("A year", style = Ucs.type.caption, color = c.ink3); Text("~${rules.creditsPerYear}", style = Ucs.type.mono, color = c.ink) }
                }
            }
        }
        item {
            UcsCard {
                Overline("Grade points")
                Spacer(Modifier.height(6.dp))
                rules.grades.forEachIndexed { i, g ->
                    if (i > 0) Hairline()
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(g.code, style = Ucs.type.titleSmall, color = c.ink, modifier = Modifier.width(44.dp))
                        Text(Format.decimals(g.points, 2), style = Ucs.type.mono, color = c.ink, modifier = Modifier.width(56.dp))
                        Text(g.description, style = Ucs.type.bodySmall, color = c.ink2, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            UcsCard {
                Overline("Degree classes")
                Spacer(Modifier.height(6.dp))
                rules.bands.forEachIndexed { i, b ->
                    if (i > 0) Hairline()
                    val upper = if (i == 0) null else rules.bands[i - 1].minGpa - 0.01
                    val range = when {
                        i == 0 -> "${Format.decimals(b.minGpa, 2)} and above"
                        b.minHundredths == 0 -> "below ${Format.decimals(rules.bands[i - 1].minGpa, 2)}"
                        else -> "${Format.decimals(b.minGpa, 2)} – ${Format.decimals(upper!!, 2)}"
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ucs.gpa.ui.components.Dot(bandColor(b, c))
                        Text(b.name, style = Ucs.type.label, color = c.ink, modifier = Modifier.weight(1f))
                        Text(range, style = Ucs.type.mono, color = c.ink2)
                    }
                }
            }
        }
        item { Overline("Each rule and where it comes from", modifier = Modifier.padding(start = 4.dp, top = 6.dp)) }
        rules.notes.forEach { note ->
            item(key = note.id) {
                UcsCard(modifier = Modifier.testTag("rule-${note.id}")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(note.title, style = Ucs.type.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
                        ProvenancePill(note.provenance)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(note.detail, style = Ucs.type.bodySmall, color = c.ink2)
                    note.source?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("Source: $it", style = Ucs.type.caption, color = c.ink3)
                    }
                }
            }
        }
        item {
            Text(
                "Rules: ${rules.institution}, ${rules.revision}. Rules marked Inferred or Assumption are not stated in an official document and can be changed in one place in the app's code once confirmed.",
                style = Ucs.type.caption, color = c.ink3, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun ProvenancePill(p: Provenance) {
    val c = Ucs.colors
    when (p) {
        Provenance.OFFICIAL -> Pill("Official", c.primarySoft, c.primary)
        Provenance.INFERRED -> Pill("Inferred", c.accentSoft, c.onAccentSoft)
        Provenance.ASSUMPTION -> Pill("Assumption", c.warningSoft, c.warning)
    }
}
