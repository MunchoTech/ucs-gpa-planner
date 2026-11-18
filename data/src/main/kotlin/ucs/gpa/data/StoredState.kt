package ucs.gpa.data

import kotlinx.serialization.Serializable
import ucs.gpa.core.model.StudentRecord

@Serializable
enum class Mode { DEMO, PERSONAL }

@Serializable
enum class ThemePreference { SYSTEM, LIGHT, DARK }

/**
 * Everything the app keeps on the device. The demo record and the student's own record are stored
 * separately, so exploring the demo never touches real results.
 */
@Serializable
data class StoredState(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val onboardingComplete: Boolean = false,
    val mode: Mode = Mode.DEMO,
    val personal: StudentRecord? = null,
    val demo: StudentRecord = DemoData.record(),
    val theme: ThemePreference = ThemePreference.SYSTEM,
) {
    val isDemo: Boolean get() = mode == Mode.DEMO || personal == null

    val activeRecord: StudentRecord get() = if (isDemo) demo else personal!!

    fun withActiveRecord(record: StudentRecord): StoredState =
        if (isDemo) copy(demo = record) else copy(personal = record)

    companion object {
        const val CURRENT_SCHEMA = 1
    }
}
