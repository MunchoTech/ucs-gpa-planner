package ucs.gpa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ucs.gpa.core.calc.CourseStatus
import ucs.gpa.core.calc.GpaCalculator
import ucs.gpa.core.format.Format
import ucs.gpa.core.model.CourseLoad
import ucs.gpa.core.validation.CourseInput
import ucs.gpa.core.validation.CourseInputValidator
import ucs.gpa.ui.ConfirmDialog
import ucs.gpa.ui.ScreenContext
import ucs.gpa.ui.ScreenList
import ucs.gpa.ui.components.Banner
import ucs.gpa.ui.components.BannerKind
import ucs.gpa.ui.components.GradePicker
import ucs.gpa.ui.components.Overline
import ucs.gpa.ui.components.PrimaryButton
import ucs.gpa.ui.components.SecondaryButton
import ucs.gpa.ui.components.SegmentedControl
import ucs.gpa.ui.components.TextAction
import ucs.gpa.ui.components.TopBar
import ucs.gpa.ui.components.UcsCard
import ucs.gpa.ui.rememberDialogState
import ucs.gpa.ui.theme.Radii
import ucs.gpa.ui.theme.Ucs

@Composable
fun CourseEditorScreen(ctx: ScreenContext, yearId: String, courseId: String?) {
    val year = ctx.record.year(yearId)
    if (year == null) {
        ScreenList(topBar = { TopBar("Course", onBack = { ctx.nav.back() }) }) {
            item { Banner("This year no longer exists.", kind = BannerKind.WARNING) }
        }
        return
    }
    val existing = courseId?.let { id -> year.courses.firstOrNull { it.id == id } }
    val id = remember(courseId) { courseId ?: ctx.store.newCourseId() }
    var input by remember(id) { mutableStateOf(existing?.let { CourseInput.from(it) } ?: CourseInput()) }
    var attempted by remember(id) { mutableStateOf(false) }
    val confirmDelete = rememberDialogState<Boolean>()
    val rules = ctx.rules

    val otherCodes = year.courses.filter { it.id != id }.map { it.code }
    val check = CourseInputValidator.validate(input, rules, otherCodes)
    val preview = GpaCalculator.course(CourseInputValidator.toCourse(input, id), yearId, rules)

    fun save() {
        attempted = true
        // Validate the current input at the moment of saving, not a value captured at composition.
        if (!CourseInputValidator.validate(input, rules, otherCodes).isValid) return
        ctx.store.saveCourse(yearId, CourseInputValidator.toCourse(input, id, existing?.plannedGrade))
        ctx.nav.back()
    }

    ScreenList(
        topBar = {
            TopBar(
                if (existing == null) "Add course" else "Edit course",
                subtitle = year.label,
                onBack = { ctx.nav.back() },
                actions = { TextAction("Save", ::save, modifier = Modifier.testTag("save-top")) },
            )
        },
    ) {
        item {
            UcsCard {
                Overline("Course")
                Spacer(Modifier.height(10.dp))
                Field(
                    value = input.code,
                    onChange = { input = input.copy(code = it.take(CourseInputValidator.MAX_CODE)) },
                    label = "Course code",
                    placeholder = "CSC 2000",
                    tag = "field-code",
                    capitalization = KeyboardCapitalization.Characters,
                    mono = true,
                )
                Spacer(Modifier.height(10.dp))
                Field(
                    value = input.title,
                    onChange = { input = input.copy(title = it.take(CourseInputValidator.MAX_TITLE)) },
                    label = "Title",
                    placeholder = "Computer Programming",
                    tag = "field-title",
                    capitalization = KeyboardCapitalization.Words,
                    error = if (attempted) check.nameError else null,
                )
            }
        }
        item {
            UcsCard {
                Overline("Weight")
                Spacer(Modifier.height(10.dp))
                val index = when (input.load) { CourseLoad.FULL -> 0; CourseLoad.HALF -> 1; CourseLoad.CUSTOM -> 2 }
                SegmentedControl(
                    listOf("Full · ${rules.fullCourseCredits}", "Half · ${rules.halfCourseCredits}", "Other"),
                    index,
                    onSelect = { input = input.copy(load = listOf(CourseLoad.FULL, CourseLoad.HALF, CourseLoad.CUSTOM)[it]) },
                    modifier = Modifier.testTag("field-load"),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (input.load) {
                        CourseLoad.FULL -> "A full course is ${rules.fullCourseCredits} credits and counts twice as much as a half course."
                        CourseLoad.HALF -> "A half course is ${rules.halfCourseCredits} credits."
                        CourseLoad.CUSTOM -> "For courses with a different credit value. Use 0 for a non-credit course."
                    },
                    style = Ucs.type.caption,
                    color = Ucs.colors.ink3,
                )
                if (input.load == CourseLoad.CUSTOM) {
                    Spacer(Modifier.height(10.dp))
                    Field(
                        value = input.customCredits,
                        onChange = { v -> input = input.copy(customCredits = v.filter { it.isDigit() }.take(3)) },
                        label = "Credits",
                        placeholder = "e.g. 20",
                        tag = "field-credits",
                        keyboard = KeyboardType.Number,
                        error = if (attempted || input.customCredits.isNotEmpty()) check.creditsError else null,
                        mono = true,
                    )
                }
            }
        }
        item {
            UcsCard {
                Overline("Grade")
                Spacer(Modifier.height(10.dp))
                GradePicker(rules, input.grade, onSelect = { input = input.copy(grade = it) }, modifier = Modifier.testTag("grade-picker"))
            }
        }
        item {
            UcsCard(color = Ucs.colors.surfaceSubtle, borderColor = Ucs.colors.surfaceSubtle) {
                Overline("Effect on your GPA")
                Spacer(Modifier.height(6.dp))
                val credits = preview.credits
                val g = preview.grade
                val line = when (preview.status) {
                    CourseStatus.COUNTED -> "$credits credits × ${Format.decimals(g!!.points, 2)} = ${Format.points(preview.weightedHundredths)} grade points"
                    CourseStatus.NOT_GRADED -> "Not counted until it has a grade. The planner treats it as still to come."
                    CourseStatus.TEMPORARY -> "${g?.code} is a temporary grade, so the course is left out until it is resolved."
                    CourseStatus.EXCLUDED -> if (credits == 0) "A non-credit course is left out of the GPA." else "Marked as not counted, so it is left out."
                    CourseStatus.REPEAT_EXCLUDED -> "A repeat attempt does not contribute to the GPA under UNZA rules."
                    CourseStatus.INVALID -> "Fix the details above to include this course."
                }
                Text(line, style = if (preview.status == CourseStatus.COUNTED) Ucs.type.mono else Ucs.type.bodySmall, color = Ucs.colors.ink, modifier = Modifier.testTag("effect"))
                if (g != null && preview.status == CourseStatus.COUNTED) {
                    Text("${g.code}: ${g.description}", style = Ucs.type.caption, color = Ucs.colors.ink3)
                }
            }
        }
        item {
            UcsCard {
                ToggleRow(
                    "Counts towards GPA",
                    "Turn off for courses that carry no credits, such as Industrial Training.",
                    input.countsTowardGpa,
                    { input = input.copy(countsTowardGpa = it) },
                )
                Spacer(Modifier.height(12.dp))
                ToggleRow(
                    "Repeat attempt",
                    "A course you are taking again. UNZA rules say a repeat does not contribute to the GPA.",
                    input.isRepeat,
                    { input = input.copy(isRepeat = it) },
                )
            }
        }
        check.warnings.forEach { w -> item { Banner(w, kind = BannerKind.WARNING) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                PrimaryButton(if (existing == null) "Add course" else "Save changes", ::save, modifier = Modifier.fillMaxWidth().testTag("save-course"))
                if (existing != null) {
                    SecondaryButton(
                        "Delete course",
                        onClick = { confirmDelete.value = true },
                        destructive = true,
                        icon = Icons.Outlined.DeleteOutline,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (confirmDelete.value == true && existing != null) {
        ConfirmDialog(
            title = "Delete ${existing.displayName}?",
            text = "The course and its grade will be removed from ${year.label}.",
            confirm = "Delete",
            destructive = true,
            onConfirm = {
                ctx.store.deleteCourse(yearId, existing.id)
                ctx.nav.back()
            },
            onDismiss = { confirmDelete.value = null },
        )
    }
}

@Composable
internal fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    tag: String? = null,
    error: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    mono: Boolean = false,
) {
    val c = Ucs.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = c.ink3) } },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        textStyle = if (mono) Ucs.type.mono.copy(color = c.ink) else Ucs.type.body.copy(color = c.ink),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = capitalization),
        shape = Radii.control,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary,
            unfocusedBorderColor = c.lineStrong,
            focusedLabelColor = c.primary,
            unfocusedLabelColor = c.ink3,
            cursorColor = c.primary,
            focusedContainerColor = c.surface,
            unfocusedContainerColor = c.surface,
        ),
        modifier = modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}

@Composable
internal fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Ucs.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Ucs.type.label, color = c.ink)
            Text(subtitle, style = Ucs.type.caption, color = c.ink3)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onPrimary,
                checkedTrackColor = c.primary,
                uncheckedThumbColor = c.ink3,
                uncheckedTrackColor = c.surfaceSubtle,
                uncheckedBorderColor = c.lineStrong,
            ),
        )
    }
}
