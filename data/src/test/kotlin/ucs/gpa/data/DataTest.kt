package ucs.gpa.data

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ucs.gpa.core.calc.GpaCalculator
import ucs.gpa.core.model.Course
import ucs.gpa.core.model.CourseLoad
import ucs.gpa.core.model.StudentProfile
import ucs.gpa.core.rules.UnzaRules

class DataTest {
    private val rules = UnzaRules.current

    private fun tempFile(): File = Files.createTempDirectory("ucs-gpa").toFile().resolve("state.json")

    private fun withStore(file: File, block: suspend (AppStore) -> Unit) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val store = AppStore(FileRecordStore(file), scope)
            store.start()
            withTimeout(10_000) { store.state.first { it is AppState.Ready } }
            block(store)
            withTimeout(10_000) { store.awaitSaved() }
        } finally {
            scope.cancel()
        }
    }

    private fun AppStore.ready() = state.value as AppState.Ready

    @Test fun `demo student is fictional, flagged and believable`() {
        val demo = DemoData.record()
        assertTrue(demo.isDemo)
        assertEquals("Chanda Mwila", demo.profile.name)
        val r = GpaCalculator.record(demo, rules)
        assertEquals(240, r.overall.countedCredits)
        // Year 1: 420 / 120 = 3.50; Year 2: 455.55 / 120 = 3.79625; overall 875.55 / 240
        assertEquals(3.5, r.years[0].gpa!!, 1e-9)
        assertEquals(3.79625, r.years[1].gpa!!, 1e-9)
        assertEquals(875.55 / 240, r.gpa!!, 1e-9)
        assertEquals("merit", r.band!!.id)
        assertTrue(r.years[2].courses.none { it.course.isGraded })
    }

    @Test fun `first launch starts in demo mode with onboarding pending`() = withStore(tempFile()) { store ->
        val s = store.ready()
        assertTrue(s.isDemo)
        assertFalse(s.stored.onboardingComplete)
        assertEquals("Chanda Mwila", s.record.profile.name)
    }

    @Test fun `personal record persists across restarts`() {
        val file = tempFile()
        withStore(file) { store ->
            store.createPersonalRecord(StudentProfile(name = "Test Student", programme = "CS", currentYearOfStudy = 2), useCatalogue = true)
            val y1 = store.ready().record.sortedYears.first()
            val course = y1.courses.first().copy(grade = "A")
            store.saveCourse(y1.id, course)
        }
        withStore(file) { store ->
            val s = store.ready()
            assertFalse(s.isDemo)
            assertEquals("Test Student", s.record.profile.name)
            assertEquals(4, s.record.years.size)
            assertEquals("A", s.record.sortedYears.first().courses.first().grade)
            assertEquals(4.0, s.derived.result.gpa!!, 1e-9)
        }
    }

    @Test fun `demo and personal records stay separate`() = withStore(tempFile()) { store ->
        store.createPersonalRecord(StudentProfile(name = "Me"), useCatalogue = false)
        val yearId = store.ready().record.years.first().id
        store.saveCourse(yearId, Course(id = "x", code = "TST 1000", load = CourseLoad.FULL, grade = "B"))
        store.switchMode(Mode.DEMO)
        assertEquals("Chanda Mwila", store.ready().record.profile.name)
        assertTrue(store.ready().record.allCourses().none { it.second.id == "x" })
        store.switchMode(Mode.PERSONAL)
        assertEquals(3.0, store.ready().derived.result.gpa!!, 1e-9)
    }

    @Test fun `grading a course clears its hypothetical grade`() = withStore(tempFile()) { store ->
        val y3 = store.ready().record.sortedYears[2]
        val course = y3.courses.first()
        store.setPlannedGrade(course.id, "A")
        assertNotNull(store.ready().derived.projection)
        store.saveCourse(y3.id, course.copy(grade = "B+", plannedGrade = "A"))
        val saved = store.ready().record.year(y3.id)!!.courses.first { it.id == course.id }
        assertNull(saved.plannedGrade)
    }

    @Test fun `reset demo restores the original demo`() = withStore(tempFile()) { store ->
        val y1 = store.ready().record.sortedYears.first()
        store.deleteCourse(y1.id, y1.courses.first().id)
        assertEquals(3, store.ready().record.sortedYears.first().courses.size)
        store.resetDemo()
        assertEquals(4, store.ready().record.sortedYears.first().courses.size)
    }

    @Test fun `catalogue courses are only added once`() = withStore(tempFile()) { store ->
        store.createPersonalRecord(StudentProfile(), useCatalogue = false)
        val y2 = store.ready().record.sortedYears[1]
        assertEquals(7, store.addCatalogueCourses(y2.id))
        assertEquals(0, store.addCatalogueCourses(y2.id))
    }

    @Test fun `backup round trip and validation`() = withStore(tempFile()) { store ->
        assertNull(store.exportBackup(), "the demo is never exported")
        store.createPersonalRecord(StudentProfile(name = "Mwansa Tembo"), useCatalogue = true)
        val y1 = store.ready().record.sortedYears.first()
        store.saveCourse(y1.id, y1.courses.first().copy(grade = "A+"))
        val export = assertNotNull(store.exportBackup())
        assertTrue(export.fileName.startsWith("ucs-gpa-mwansa-tembo-"))

        store.deletePersonalRecord()
        assertTrue(store.ready().isDemo)
        assertTrue(store.importBackup(export.json))
        val restored = store.ready()
        assertFalse(restored.isDemo)
        assertEquals("A+", restored.record.sortedYears.first().courses.first().grade)

        assertFalse(store.importBackup("{\"hello\":1}"))
        assertTrue(store.ready().notice!!.isError)
        assertFalse(store.importBackup(export.json.replace("\"version\": 1", "\"version\": 99")))
        assertIs<BackupResult.Failure>(BackupCodec.decode(""))
    }

    @Test fun `unreadable saved data is kept aside, not deleted`(): Unit = runBlocking {
        val file = tempFile()
        file.parentFile.mkdirs()
        file.writeText("{ not json")
        val result = FileRecordStore(file).load()
        assertIs<LoadResult.Unreadable>(result)
        assertNotNull(result.keptAt)
        assertTrue(File(file.parentFile, result.keptAt!!).exists())
    }

    @Test fun `data from a newer app version is not silently overwritten`(): Unit = runBlocking {
        val file = tempFile()
        file.parentFile.mkdirs()
        file.writeText(StateCodec.encode(StoredState()).replace("\"schemaVersion\":1", "\"schemaVersion\":9"))
        assertIs<LoadResult.Unreadable>(FileRecordStore(file).load())
    }

    @Test fun `unknown fields are ignored for forward compatibility`() {
        val text = StateCodec.encode(StoredState()).replaceFirst("{", "{\"futureField\":true,")
        assertEquals(StoredState().demo, StateCodec.decode(text).demo)
    }

    @Test fun `saves are atomic and leave no temp file`(): Unit = runBlocking {
        val file = tempFile()
        val store = FileRecordStore(file)
        store.save(StoredState(onboardingComplete = true))
        assertTrue(file.exists())
        assertFalse(File(file.parentFile, "${file.name}.tmp").exists())
        assertTrue((store.load() as LoadResult.Loaded).state.onboardingComplete)
    }
}
