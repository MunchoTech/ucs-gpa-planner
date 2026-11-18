package ucs.gpa.data

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed interface LoadResult {
    data class Loaded(val state: StoredState) : LoadResult
    data object Empty : LoadResult
    /** The saved file could not be read. It is kept aside (not deleted) so nothing is lost. */
    data class Unreadable(val reason: String, val keptAt: String?) : LoadResult
}

/**
 * Where the app keeps its data. The app uses [FileRecordStore]; a cloud-synced store could
 * implement the same interface later without touching the UI or the engine.
 */
interface RecordStore {
    suspend fun load(): LoadResult
    suspend fun save(state: StoredState)
}

object StateCodec {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(state: StoredState): String = json.encodeToString(StoredState.serializer(), state)

    fun decode(text: String): StoredState {
        val state = json.decodeFromString(StoredState.serializer(), text)
        if (state.schemaVersion > StoredState.CURRENT_SCHEMA) {
            throw SerializationException("Saved data is from a newer version of the app (schema ${state.schemaVersion}).")
        }
        return state
    }
}

/** Stores the state as one JSON file in app-private storage, written atomically. */
class FileRecordStore(
    private val file: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : RecordStore {

    override suspend fun load(): LoadResult = withContext(io) {
        if (!file.exists()) return@withContext LoadResult.Empty
        val text = try {
            file.readText()
        } catch (e: IOException) {
            return@withContext LoadResult.Unreadable("The saved file could not be opened.", null)
        }
        try {
            LoadResult.Loaded(StateCodec.decode(text))
        } catch (e: Exception) {
            val aside = File(file.parentFile, "${file.name}.unreadable-${System.currentTimeMillis()}")
            val kept = runCatching { file.copyTo(aside, overwrite = false) }.isSuccess
            LoadResult.Unreadable(e.message ?: "The saved data is damaged.", if (kept) aside.name else null)
        }
    }

    override suspend fun save(state: StoredState): Unit = withContext(io) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(StateCodec.encode(state))
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

/** For previews and tests. */
class InMemoryRecordStore(initial: StoredState? = null) : RecordStore {
    @Volatile var saved: StoredState? = initial
        private set
    var saveCount = 0
        private set

    override suspend fun load(): LoadResult = saved?.let { LoadResult.Loaded(it) } ?: LoadResult.Empty

    override suspend fun save(state: StoredState) {
        saved = state
        saveCount++
    }
}
