package ucs.gpa.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import ucs.gpa.data.AppStore
import ucs.gpa.data.InMemoryRecordStore
import ucs.gpa.data.StoredState
import ucs.gpa.ui.NoopPlatform
import ucs.gpa.ui.UcsApp
import ucs.gpa.ui.nav.Navigator
import ucs.gpa.ui.theme.BrandAssets
import ucs.gpa.ui.theme.UcsFonts

/** Loads the app's real fonts and logo from app/src/main/res and renders the real UI headlessly. */
object Harness {
    private val res = File(System.getProperty("ucs.res") ?: "../../app/src/main/res")
    val out: File = File(System.getProperty("ucs.screenshots") ?: "build/screenshots").apply { mkdirs() }

    val fonts: UcsFonts by lazy {
        fun f(name: String) = File(res, "font/$name.ttf").also { require(it.exists()) { "Missing font $it" } }
        UcsFonts(
            sans = FontFamily(
                Font(f("geist_regular"), FontWeight.Normal),
                Font(f("geist_medium"), FontWeight.Medium),
                Font(f("geist_semibold"), FontWeight.SemiBold),
                Font(f("geist_bold"), FontWeight.Bold),
            ),
            mono = FontFamily(
                Font(f("geist_mono_regular"), FontWeight.Normal),
                Font(f("geist_mono_medium"), FontWeight.Medium),
                Font(f("geist_mono_semibold"), FontWeight.SemiBold),
            ),
        )
    }

    val brand: BrandAssets by lazy {
        val bytes = File(res, "drawable-nodpi/ucs_mark.png").readBytes()
        BrandAssets(BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap()))
    }

    fun store(state: StoredState = StoredState()): Pair<AppStore, InMemoryRecordStore> {
        val backing = InMemoryRecordStore(state)
        val store = AppStore(backing, CoroutineScope(Dispatchers.Unconfined))
        store.start()
        return store to backing
    }

    @Composable
    fun App(store: AppStore, nav: Navigator, dark: Boolean = false) {
        UcsApp(store, NoopPlatform, nav, systemDark = dark, fonts = fonts, brand = brand)
    }

    /** Renders [content] at a phone-like size and writes a PNG. Animations are run to completion. */
    fun shoot(name: String, widthDp: Int = 393, heightDp: Int = 852, density: Float = 2.75f, content: @Composable () -> Unit) {
        // Compose desktop applies snapshot changes on the AWT event thread; rendering there too
        // keeps composition, animation and state changes on one thread.
        onEdt { render(name, widthDp, heightDp, density, content) }
    }

    fun onEdt(block: () -> Unit) {
        var error: Throwable? = null
        javax.swing.SwingUtilities.invokeAndWait { try { block() } catch (t: Throwable) { error = t } }
        error?.let { throw it }
    }

    private fun render(name: String, widthDp: Int, heightDp: Int, density: Float, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(
            width = (widthDp * density).toInt(),
            height = (heightDp * density).toInt(),
            density = Density(density),
            content = content,
        )
        try {
            var t = 0L
            repeat(40) { scene.render(t).close(); t += 50_000_000L }
            val image = scene.render(t + 1_000_000_000L)
            val data = image.encodeToData(EncodedImageFormat.PNG)!!
            File(out, "$name.png").writeBytes(data.bytes)
            data.close()
            image.close()
        } finally {
            scene.close()
        }
    }
}
