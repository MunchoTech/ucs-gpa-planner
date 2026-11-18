package ucs.gpa.android

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ucs.gpa.data.AppStore
import ucs.gpa.data.BackupExport
import ucs.gpa.data.FileRecordStore
import ucs.gpa.ui.nav.Navigator

/**
 * Owns the app store for the lifetime of the activity task. Data lives in the app's private files
 * directory, which other apps cannot read; it never leaves the device unless the student exports a
 * backup.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    val store = AppStore(
        store = FileRecordStore(File(app.filesDir, "records/state.json")),
        scope = viewModelScope,
    ).also { it.start() }

    val navigator = Navigator()

    /** The backup waiting for the student to pick a destination file. */
    var pendingExport: BackupExport? = null

    fun writeBackup(resolver: ContentResolver, uri: Uri) {
        val export = pendingExport ?: return
        pendingExport = null
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    resolver.openOutputStream(uri, "wt")!!.use { it.write(export.json.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            if (ok) store.notify("Backup saved.") else store.notify("The backup could not be saved. Try another location.", isError = true)
        }
    }

    fun readBackup(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    resolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBytes()
                        if (bytes.size > MAX_BACKUP_BYTES) null else bytes.toString(Charsets.UTF_8)
                    }
                }.getOrNull()
            }
            if (text == null) store.notify("That file could not be read.", isError = true) else store.importBackup(text)
        }
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 2_000_000
    }
}
