package dev.niklas.fitnessrpg.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Theme engine: the whole palette is derived from one accent color
 * (port of Cryptobro's theme.js). Backgrounds are the accent hue scaled toward
 * black, text is the accent hue mixed toward white.
 */
@Immutable
class CyberPalette(val accent: Color) {
    private val r = accent.red
    private val g = accent.green
    private val b = accent.blue

    // brightest version of the hue: keeps backgrounds tinted even for dark picks
    private val norm: FloatArray = run {
        val m = max(r, max(g, b))
        if (m == 0f) floatArrayOf(.5f, .5f, .5f) else floatArrayOf(r / m, g / m, b / m)
    }

    /** accent hue scaled toward black (f = 0..1) */
    fun tint(f: Float, alpha: Float = 1f) = Color(norm[0] * f, norm[1] * f, norm[2] * f, alpha)

    /** accent hue mixed toward white: white * w + hue * k */
    private fun mix(w: Float, k: Float) = Color(
        (w + norm[0] * k).coerceIn(0f, 1f),
        (w + norm[1] * k).coerceIn(0f, 1f),
        (w + norm[2] * k).coerceIn(0f, 1f)
    )

    fun light(w: Float, alpha: Float = 1f) = Color(
        w + norm[0] * (1 - w), w + norm[1] * (1 - w), w + norm[2] * (1 - w), alpha
    )

    fun acc(alpha: Float) = accent.copy(alpha = alpha)

    val k01 = tint(.012f)
    val k02 = tint(.02f)
    val k04 = tint(.04f)
    val k05 = tint(.05f)
    val k07 = tint(.07f)
    val k11 = tint(.11f)
    val k16 = tint(.16f)
    val k23 = tint(.23f)
    val k47 = tint(.47f)
    val k54 = tint(.54f)
    val k77 = tint(.77f)

    val bg = k02
    val bg2 = k05
    val panel = k07.copy(alpha = .78f)
    val panelSolid = k07
    val accDim = k54
    val accDeep = k23
    val line = acc(.22f)
    val line2 = acc(.5f)

    val text = mix(.85f, .15f)
    val textDim = mix(.457f, .233f)
    val textMute = mix(.207f, .22f)

    companion object {
        val AMBER = Color(0xFFFFB000)
        val CYAN = Color(0xFF00E5FF)
        val MAGENTA = Color(0xFFFF00AA)
        val UP = Color(0xFF00FF9C)
        val DOWN = Color(0xFFFF2A4D)
    }
}

/** Same presets as the Cryptobro dashboard. */
val THEME_PRESETS = listOf(
    "CRIMSON" to 0xFFFF1744, "BLOOD" to 0xFFC4001D, "SOLAR" to 0xFFFF6D00, "AMBER" to 0xFFFFB000,
    "GOLD" to 0xFFFFD700, "TOXIC" to 0xFF39FF14, "MATRIX" to 0xFF00FF9C, "CYAN" to 0xFF00E5FF,
    "ICE" to 0xFF7DF9FF, "ULTRAVIOLET" to 0xFF9D4DFF, "NEON PINK" to 0xFFFF2BD6, "GHOST" to 0xFFD8D8FF
).map { (n, c) -> n to Color(c) }

val DEFAULT_ACCENT = Color(0xFFFF1744)

fun Color.toHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

fun parseHex(s: String): Color? {
    val h = s.trim().removePrefix("#")
    if (h.length != 6 || !h.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return Color(0xFF000000 or h.toLong(16))
}

/** Holds the live accent; persisted synchronously so the saved theme applies before first frame. */
class ThemeState(private val context: Context) {
    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
    var palette by mutableStateOf(CyberPalette(
        prefs.getString("accent", null)?.let(::parseHex) ?: DEFAULT_ACCENT
    ))
        private set

    fun apply(color: Color) {
        palette = CyberPalette(color.copy(alpha = 1f))
        prefs.edit().putString("accent", color.toHex()).apply()
    }
}

val LocalPalette = staticCompositionLocalOf { CyberPalette(DEFAULT_ACCENT) }
val LocalThemeState = staticCompositionLocalOf<ThemeState?> { null }

object Cyber {
    val c: CyberPalette @Composable get() = LocalPalette.current
}

@Composable
fun CyberTheme(state: ThemeState, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPalette provides state.palette,
        LocalThemeState provides state,
        content = content
    )
}

/** 0..255 int triplet, handy for debug text. */
fun Color.rgbString() = "${(red * 255).roundToInt()}, ${(green * 255).roundToInt()}, ${(blue * 255).roundToInt()}"
