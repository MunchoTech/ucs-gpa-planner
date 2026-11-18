package ucs.gpa.data

import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ucs.gpa.core.calc.GpaCalculator
import ucs.gpa.core.calc.RecordResult
import ucs.gpa.core.insight.Insight
import ucs.gpa.core.insight.InsightEngine
import ucs.gpa.core.model.AcademicYear
import ucs.gpa.core.model.Course
import ucs.gpa.core.model.PlannerTarget
import ucs.gpa.core.model.StudentProfile
import ucs.gpa.core.model.StudentRecord
import ucs.gpa.core.plan.GradeCombination
import ucs.gpa.core.plan.ResolvedTarget
import ucs.gpa.core.plan.ScenarioProjection
import ucs.gpa.core.plan.Scenarios
import ucs.gpa.core.plan.TargetAnalysis
import ucs.gpa.core.plan.TargetPlanner
import ucs.gpa.core.rules.AcademicRules
import ucs.gpa.core.rules.UnzaRules

/** Everything the screens show, computed once per change from the active record. */
data class Derived(
    val record: StudentRecord,
    val result: RecordResult,
    val target: ResolvedTarget,
    val analysis: TargetAnalysis,
    val combinations: List<GradeCombination>,
    /** Projection from the hypothetical grades saved in the record, or null when none are set. */
    val projection: ScenarioProjection?,
    val insights: List<Insight>,
    /** The analysis for each class the student could aim for, keyed by band id. */
    val bandAnalyses: Map<String, TargetAnalysis>,
) {
    companion object {
        fun of(record: StudentRecord, rules: AcademicRules): Derived {
            val result = GpaCalculator.record(record, rules)
            val target = TargetPlanner.resolve(record.target, rules)
            val estimate = record.target.estimatedRemainingCredits
            val analysis = TargetPlanner.analyse(record, rules, target, estimate)
            val planned = analysis.remaining.mapNotNull { rc -> rc.course.plannedGrade?.let { rc.course.id to it } }.toMap()
            val projection = if (planned.isNotEmpty()) Scenarios.project(analysis.current, analysis.remaining, planned, target, rules) else null
            val bands = rules.targetBands.associate { b ->
                b.id to TargetPlanner.analyse(record, rules, ResolvedTarget(b.minHundredths, b), estimate)
            }
            return Derived(
                record = record,
                result = result,
                target = target,
                analysis = analysis,
                combinations = Scenarios.combinations(analysis, rules),
                projection = projection,
                insights = InsightEngine.generate(result, analysis, rules),
                bandAnalyses = bands,
            )
        }
    }
}

data class Notice(val message: String, val isError: Boolean = false, val id: Long = System.nanoTime())

sealed interface AppState {
    data object Loading : AppState
    data class Ready(val stored: StoredState, val derived: Derived, val notice: Notice? = null) : AppState {
        val record: StudentRecord get() = derived.record
        val isDemo: Boolean get() = stored.isDemo
    }
}

data class BackupExport(val fileName: String, val json: String)

/**
 * The single source of truth for the app. Screens read [state] and call the action methods; every
 * change is saved in the background, latest state first.
 */
class AppStore(
    private val store: RecordStore,
    private val scope: CoroutineScope,
    val rules: AcademicRules = UnzaRules.current,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val today: () -> String = { LocalDate.now().toString() },
) {
    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val saves = Channel<Pair<Long, StoredState>>(Channel.CONFLATED)
    private var requestedVersion = 0L
    private val savedVersion = MutableStateFlow(0L)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            var notice: Notice? = null
            val initial = when (val loaded = runCatching { store.load() }.getOrElse { LoadResult.Unreadable(it.message ?: "Unknown error", null) }) {
                is LoadResult.Loaded -> loaded.state
                LoadResult.Empty -> StoredState()
                is LoadResult.Unreadable -> {
                    notice = Notice(
                        "Your saved data could not be read, so the app started fresh." +
                            (loaded.keptAt?.let { " The old file was kept as $it." } ?: ""),
                        isError = true,
                    )
                    StoredState()
                }
            }
            _state.value = ready(initial, notice)
            for ((version, snapshot) in saves) {
                try {
                    store.save(snapshot)
                } catch (e: Exception) {
                    notify("Changes could not be saved: ${e.message ?: "storage error"}", isError = true)
                }
                savedVersion.value = version
            }
        }
    }

    /** Suspends until everything changed so far has been written. */
    suspend fun awaitSaved() {
        val target = requestedVersion
        savedVersion.first { it >= target }
    }

    fun newCourseId(): String = newId()

    // ---- onboarding and modes ----

    fun exploreDemo() = mutate { it.copy(onboardingComplete = true, mode = Mode.DEMO) }

    fun createPersonalRecord(profile: StudentProfile, useCatalogue: Boolean) = mutate { s ->
        val years = (1..profile.programmeYears.coerceIn(1, 7)).map { n ->
            AcademicYear(
                id = newId(),
                yearOfStudy = n,
                courses = if (useCatalogue) Catalogue.forYear(n).map { it.toCourse(newId()) } else emptyList(),
            )
        }
        s.copy(
            onboardingComplete = true,
            mode = Mode.PERSONAL,
            personal = StudentRecord(id = newId(), profile = profile, years = years),
        )
    }

    fun switchMode(mode: Mode) = mutate { s ->
        if (mode == Mode.PERSONAL && s.personal == null) s else s.copy(mode = mode)
    }

    fun resetDemo() {
        mutate { it.copy(demo = DemoData.record()) }
        notify("Demo data reset.")
    }

    fun deletePersonalRecord() {
        mutate { it.copy(personal = null, mode = Mode.DEMO) }
        notify("Your record was deleted from this device.")
    }

    fun setTheme(theme: ThemePreference) = mutate { it.copy(theme = theme) }

    // ---- record editing ----

    fun updateProfile(profile: StudentProfile) = mutateRecord { it.copy(profile = profile) }

    fun addYear(): String? {
        var created: String? = null
        mutateRecord { r ->
            val next = (r.years.maxOfOrNull { it.yearOfStudy } ?: 0) + 1
            if (next > 7) return@mutateRecord r
            val id = newId()
            created = id
            r.copy(years = r.years + AcademicYear(id = id, yearOfStudy = next))
        }
        return created
    }

    fun removeYear(yearId: String) = mutateRecord { r -> r.copy(years = r.years.filterNot { it.id == yearId }) }

    fun addCatalogueCourses(yearId: String): Int {
        var added = 0
        mutateRecord { r ->
            r.updateYear(yearId) { y ->
                val existing = y.courses.map { normalize(it.code) }.toSet()
                val extra = Catalogue.forYear(y.yearOfStudy).filter { normalize(it.code) !in existing }.map { it.toCourse(newId()) }
                added = extra.size
                y.copy(courses = y.courses + extra)
            }
        }
        return added
    }

    fun saveCourse(yearId: String, course: Course) = mutateRecord { r ->
        r.updateYear(yearId) { y ->
            val exists = y.courses.any { it.id == course.id }
            val cleaned = if (course.isGraded) course.copy(plannedGrade = null) else course
            y.copy(courses = if (exists) y.courses.map { if (it.id == course.id) cleaned else it } else y.courses + cleaned)
        }
    }

    fun deleteCourse(yearId: String, courseId: String) = mutateRecord { r ->
        r.updateYear(yearId) { y -> y.copy(courses = y.courses.filterNot { it.id == courseId }) }
    }

    // ---- planner ----

    fun setTarget(target: PlannerTarget) = mutateRecord { it.copy(target = target) }

    fun setPlannedGrade(courseId: String, grade: String?) = mutateRecord { r ->
        r.copy(years = r.years.map { y ->
            y.copy(courses = y.courses.map { c -> if (c.id == courseId && !c.isGraded) c.copy(plannedGrade = grade) else c })
        })
    }

    fun applyPlannedGrades(assignments: Map<String, String>) = mutateRecord { r ->
        r.copy(years = r.years.map { y ->
            y.copy(courses = y.courses.map { c -> assignments[c.id]?.takeIf { !c.isGraded }?.let { c.copy(plannedGrade = it) } ?: c })
        })
    }

    fun clearPlannedGrades() = mutateRecord { r ->
        r.copy(years = r.years.map { y -> y.copy(courses = y.courses.map { it.copy(plannedGrade = null) }) })
    }

    // ---- backup ----

    /** The student's own record as a backup file, or null in demo mode. */
    fun exportBackup(): BackupExport? {
        val ready = _state.value as? AppState.Ready ?: return null
        val record = ready.stored.personal ?: return null
        val date = today()
        return BackupExport(BackupCodec.fileName(record, date), BackupCodec.encode(record, date))
    }

    fun importBackup(text: String): Boolean = when (val result = BackupCodec.decode(text)) {
        is BackupResult.Success -> {
            mutate { it.copy(personal = result.record, mode = Mode.PERSONAL, onboardingComplete = true) }
            notify("Backup from ${result.exportedAt} restored.")
            true
        }
        is BackupResult.Failure -> {
            notify(result.message, isError = true)
            false
        }
    }

    fun notify(message: String, isError: Boolean = false) {
        val ready = _state.value as? AppState.Ready ?: return
        _state.value = ready.copy(notice = Notice(message, isError))
    }

    fun dismissNotice() {
        val ready = _state.value as? AppState.Ready ?: return
        _state.value = ready.copy(notice = null)
    }

    // ---- internals ----

    private fun ready(stored: StoredState, notice: Notice?): AppState.Ready =
        AppState.Ready(stored, Derived.of(stored.activeRecord, rules), notice)

    private fun mutate(transform: (StoredState) -> StoredState) {
        val current = _state.value as? AppState.Ready ?: return
        val next = transform(current.stored)
        if (next == current.stored) return
        _state.value = ready(next, current.notice)
        requestedVersion++
        saves.trySend(requestedVersion to next)
    }

    private fun mutateRecord(transform: (StudentRecord) -> StudentRecord) =
        mutate { s -> s.withActiveRecord(transform(s.activeRecord)) }

    private fun normalize(code: String) = code.trim().uppercase().replace(" ", "")
}
