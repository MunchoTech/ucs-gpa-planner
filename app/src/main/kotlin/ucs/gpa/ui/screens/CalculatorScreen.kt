package ucs.gpa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ucs.gpa.core.calc.Severity
import ucs.gpa.core.calc.YearResult
import ucs.gpa.core.format.Format
import ucs.gpa.data.Catalogue
import ucs.gpa.ui.ConfirmDialog
import ucs.gpa.ui.ScreenContext
import ucs.gpa.ui.ScreenList
import ucs.gpa.ui.components.BandChip
import ucs.gpa.ui.components.Banner
import ucs.gpa.ui.components.BannerKind
import ucs.gpa.ui.components.ClickableText
import ucs.gpa.ui.components.CourseRow
import ucs.gpa.ui.components.EmptyState
import ucs.gpa.ui.components.Hairline
import ucs.gpa.ui.components.Overline
import ucs.gpa.ui.components.PrimaryButton
import ucs.gpa.ui.components.SecondaryButton
import ucs.gpa.ui.components.SelectChip
import ucs.gpa.ui.components.Stat
import ucs.gpa.ui.components.TopBar
import ucs.gpa.ui.components.UcsCard
import ucs.gpa.ui.nav.Route
import ucs.gpa.ui.rememberDialogState
import ucs.gpa.ui.theme.Spacing
import ucs.gpa.ui.theme.Ucs

@Composable
fun CalculatorScreen(ctx: ScreenContext) {
    val result = ctx.derived.result
    val selected = result.years.firstOrNull { it.year.id == ctx.nav.selectedYearId } ?: defaultYear(result, ctx.record.profile)
    val confirmRemove = rememberDialogState<YearResult>()

    ScreenList(
        topBar = {
            TopBar(
                "Calculator",
                subtitle = "Your GPA updates as you enter grades",
                actions = {
                    IconButton(onClick = { ctx.nav.navigate(Route.Breakdown(null)) }, modifier = Modifier.testTag("explain-cumulative")) {
                        Icon(Icons.Outlined.Functions, contentDescription = "How the cumulative GPA is calculated", tint = Ucs.colors.ink2)
                    }
                },
            )
        },
    ) {
        if (ctx.state.isDemo) item { DemoBanner(onStartOwn = { ctx.nav.navigate(Route.Setup) }) }
        item {
            YearTabs(
                years = result.years,
                selectedId = selected?.year?.id,
                onSelect = { ctx.nav.selectedYearId = it },
                onAdd = { ctx.store.addYear()?.let { ctx.nav.selectedYearId = it } },
            )
        }
        if (selected == null) {
            item {
                EmptyState(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    "No years yet",
                    "Add your first year of study, then add its courses and grades.",
                    primary = "Add Year 1" to { ctx.store.addYear()?.let { ctx.nav.selectedYearId = it } },
                )
            }
            return@ScreenList
        }

        item { YearSummary(selected, onExplain = { ctx.nav.navigate(Route.Breakdown(selected.year.id)) }) }

        val yearIssues = selected.issues.filter { it.severity != Severity.INFO }
        yearIssues.take(3).forEach { issue ->
            item(key = "issue-${issue.code}-${issue.courseId}") {
                Banner(issue.message, kind = if (issue.severity == Severity.ERROR) BannerKind.ERROR else BannerKind.WARNING)
            }
        }

        val missingFromCatalogue = Catalogue.forYear(selected.year.yearOfStudy).count { cat ->
            selected.courses.none { it.course.code.replace(" ", "").equals(cat.code.replace(" ", ""), ignoreCase = true) }
        }

        if (selected.courses.isEmpty()) {
            item {
                UcsCard {
                    EmptyState(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        "No courses in ${selected.year.label}",
                        if (missingFromCatalogue > 0) "Add your courses one by one, or start from the Computer Science course list for this year and fill in your grades."
                        else "Add each course with its weight (full or half) and your grade.",
                        primary = "Add course" to { ctx.nav.navigate(Route.EditCourse(selected.year.id, null)) },
                        secondary = if (missingFromCatalogue > 0) "Use course list" to { ctx.store.addCatalogueCourses(selected.year.id) } else null,
                    )
                }
            }
        } else {
            item {
                UcsCard(padding = PaddingValues(0.dp), modifier = Modifier.testTag("course-list")) {
                    selected.courses.forEachIndexed { i, course ->
                        if (i > 0) Hairline(Modifier.padding(start = 16.dp))
                        CourseRow(course, ctx.rules, onClick = { ctx.nav.navigate(Route.EditCourse(selected.year.id, course.course.id)) })
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        "Add course",
                        onClick = { ctx.nav.navigate(Route.EditCourse(selected.year.id, null)) },
                        icon = Icons.Outlined.Add,
                        modifier = Modifier.fillMaxWidth().testTag("add-course"),
                    )
                    if (missingFromCatalogue > 0) {
                        SecondaryButton(
                            "Add ${plural(missingFromCatalogue, "course")} from the CS list",
                            onClick = { ctx.store.addCatalogueCourses(selected.year.id) },
                            icon = Icons.Outlined.LibraryAdd,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ClickableText("Remove ${selected.year.label}", onClick = { confirmRemove.value = selected }, color = Ucs.colors.ink3)
            }
        }
    }

    confirmRemove.value?.let { y ->
        ConfirmDialog(
            title = "Remove ${y.year.label}?",
            text = if (y.courses.isEmpty()) "This year has no courses." else "Its ${plural(y.courses.size, "course")} and grades will be deleted from this record.",
            confirm = "Remove",
            destructive = true,
            onConfirm = {
                ctx.store.removeYear(y.year.id)
                ctx.nav.selectedYearId = null
            },
            onDismiss = { confirmRemove.value = null },
        )
    }
}

@Composable
private fun YearTabs(years: List<YearResult>, selectedId: String?, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    val c = Ucs.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("year-tabs"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        years.forEach { y ->
            SelectChip(
                y.year.label,
                selected = y.year.id == selectedId,
                onClick = { onSelect(y.year.id) },
                sub = y.gpa?.let { Format.gpa(it) },
                modifier = Modifier.testTag("year-tab-${y.year.yearOfStudy}"),
            )
        }
        if (years.size < 7) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).border(1.dp, c.line, CircleShape).background(c.surface)
                    .clickable(role = Role.Button, onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add a year", tint = c.ink2, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun YearSummary(y: YearResult, onExplain: () -> Unit) {
    val c = Ucs.colors
    val pending = y.courses.count { it.isPending }
    UcsCard(modifier = Modifier.testTag("year-summary")) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Overline("${y.year.label} GPA" + (y.year.session?.let { " · $it" } ?: ""))
                AnimatedGpa(y.gpa, Ucs.type.bigNumber, c.ink, modifier = Modifier.testTag("year-gpa"))
                if (y.hasGrades) BandChip(y.band, prefix = null)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("Credits counted", "${y.summary.countedCredits}", modifier = Modifier.weight(1f))
            Stat("Grade points", Format.points(y.summary.weightedHundredths), modifier = Modifier.weight(1f))
            Stat("Graded", "${y.summary.gradedCourses}", sub = "of ${y.courses.size}", modifier = Modifier.weight(1f))
        }
        if (pending > 0 || !y.hasGrades) {
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    y.courses.isEmpty() -> "Add courses to calculate this year."
                    !y.hasGrades -> "No grades yet. Courses without a grade are not counted, and the planner treats them as still to come."
                    else -> "${plural(pending, "course")} still to be graded. They are not counted until they have a grade."
                },
                style = Ucs.type.caption,
                color = c.ink3,
            )
        }
        if (y.hasGrades) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Functions, null, tint = c.primary, modifier = Modifier.size(16.dp))
                ClickableText("See how ${Format.gpa(y.gpa)} is calculated", onExplain, modifier = Modifier.testTag("explain-year"))
            }
        }
    }
}
