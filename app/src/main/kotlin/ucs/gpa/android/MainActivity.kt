package ucs.gpa.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ucs.gpa.data.AppState
import ucs.gpa.data.BackupExport
import ucs.gpa.data.ThemePreference
import ucs.gpa.ui.LocalBackHandler
import ucs.gpa.ui.PlatformBridge
import ucs.gpa.ui.UcsApp
import ucs.gpa.ui.theme.BrandAssets
import ucs.gpa.ui.theme.UcsFonts

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.writeBackup(contentResolver, uri)
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.readBackup(contentResolver, uri)
    }

    private val platform = object : PlatformBridge {
        override val appVersion: String = BuildConfig.VERSION_NAME

        override fun exportBackup(export: BackupExport) {
            viewModel.pendingExport = export
            exportLauncher.launch(export.fileName)
        }

        override fun requestBackupImport() {
            importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.store.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when ((state as? AppState.Ready)?.stored?.theme) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                else -> systemDark
            }
            // Keep status and navigation bar icons readable when the in-app theme differs from the system's.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }

            val navigator = viewModel.navigator
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }

            CompositionLocalProvider(LocalBackHandler provides { enabled, onBack -> BackHandler(enabled, onBack) }) {
                UcsApp(
                    store = viewModel.store,
                    platform = platform,
                    navigator = navigator,
                    systemDark = systemDark,
                    fonts = fonts,
                    brand = BrandAssets(painterResource(R.drawable.ucs_mark)),
                )
            }
        }
    }

    private companion object {
        val fonts = UcsFonts(
            sans = FontFamily(
                Font(R.font.geist_regular, FontWeight.Normal),
                Font(R.font.geist_medium, FontWeight.Medium),
                Font(R.font.geist_semibold, FontWeight.SemiBold),
                Font(R.font.geist_bold, FontWeight.Bold),
            ),
            mono = FontFamily(
                Font(R.font.geist_mono_regular, FontWeight.Normal),
                Font(R.font.geist_mono_medium, FontWeight.Medium),
                Font(R.font.geist_mono_semibold, FontWeight.SemiBold),
            ),
        )
    }
}
