package ucs.gpa.data

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ucs.gpa.core.model.StudentRecord
import ucs.gpa.core.validation.CourseInputValidator

@Serializable
data class BackupFile(
    val format: String = BackupCodec.FORMAT,
    val version: Int = BackupCodec.VERSION,
    val app: String = "UCS GPA Planner",
    val exportedAt: String,
    val record: StudentRecord,
)

sealed interface BackupResult {
    data class Success(val record: StudentRecord, val exportedAt: String) : BackupResult
    data class Failure(val message: String) : BackupResult
}

/** A backup is a readable JSON file the student saves wherever they like. */
object BackupCodec {
    const val FORMAT = "ucs-gpa-planner-backup"
    const val VERSION = 1
    private const val MAX_YEARS = 10
    private const val MAX_COURSES_PER_YEAR = 40

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
    }

    fun encode(record: StudentRecord, exportedAt: String): String =
        json.encodeToString(BackupFile.serializer(), BackupFile(exportedAt = exportedAt, record = record.copy(isDemo = false)))

    fun fileName(record: StudentRecord, date: String): String {
        val who = record.profile.name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return listOf("ucs-gpa", who.ifEmpty { null }, date).filterNotNull().joinToString("-") + ".json"
    }

    fun decode(text: String): BackupResult {
        if (text.isBlank()) return BackupResult.Failure("The file is empty.")
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            return BackupResult.Failure("This is not a UCS GPA Planner backup, or the file is damaged.")
        }
        if (file.format != FORMAT) return BackupResult.Failure("This is not a UCS GPA Planner backup.")
        if (file.version > VERSION) return BackupResult.Failure("This backup was made by a newer version of the app. Update the app and try again.")
        val years = file.record.years
        if (years.size > MAX_YEARS) return BackupResult.Failure("The backup has more years than the app supports.")
        if (years.any { it.courses.size > MAX_COURSES_PER_YEAR }) return BackupResult.Failure("A year in the backup has too many courses.")
        if (years.any { it.yearOfStudy !in 1..MAX_YEARS }) return BackupResult.Failure("The backup has an invalid year of study.")
        return BackupResult.Success(sanitize(file.record), file.exportedAt)
    }

    /** Gives every year and course a fresh unique id and trims over-long text. */
    private fun sanitize(record: StudentRecord): StudentRecord = record.copy(
        id = UUID.randomUUID().toString(),
        isDemo = false,
        profile = record.profile.copy(
            name = record.profile.name.take(60),
            programme = record.profile.programme.take(80),
            currentYearOfStudy = record.profile.currentYearOfStudy.coerceIn(1, MAX_YEARS),
            programmeYears = record.profile.programmeYears.coerceIn(1, MAX_YEARS),
        ),
        years = record.years.map { y ->
            y.copy(
                id = UUID.randomUUID().toString(),
                courses = y.courses.map { c ->
                    c.copy(
                        id = UUID.randomUUID().toString(),
                        code = c.code.take(CourseInputValidator.MAX_CODE),
                        title = c.title.take(CourseInputValidator.MAX_TITLE),
                    )
                },
            )
        },
    )
}
