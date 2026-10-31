package dev.niklas.ironcore.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import dev.niklas.ironcore.R

/** Sound effects (SoundPool, low latency) + haptics. One instance per activity. */
class Fx(context: Context) {
    private val prefs = context.getSharedPreferences("fx", Context.MODE_PRIVATE)
    var soundOn: Boolean = prefs.getBoolean("sound", true)
        set(v) { field = v; prefs.edit().putBoolean("sound", v).apply() }
    var hapticsOn: Boolean = prefs.getBoolean("haptics", true)
        set(v) { field = v; prefs.edit().putBoolean("haptics", v).apply() }

    var view: View? = null

    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val ids = mapOf(
        "tap" to R.raw.sfx_tap, "tick" to R.raw.sfx_tick, "boot" to R.raw.sfx_boot,
        "complete" to R.raw.sfx_complete, "levelup" to R.raw.sfx_levelup, "streak" to R.raw.sfx_streak,
        "error" to R.raw.sfx_error, "glitch" to R.raw.sfx_glitch
    ).mapValues { pool.load(context, it.value, 1) }

    private var lastTick = 0L

    private fun play(name: String, vol: Float = 1f, rate: Float = 1f) {
        if (!soundOn) return
        ids[name]?.let { pool.play(it, vol, vol, 1, 0, rate) }
    }

    private fun haptic(kind: Int) {
        if (hapticsOn) view?.performHapticFeedback(kind)
    }

    fun tap() { play("tap", .7f); haptic(HapticFeedbackConstants.KEYBOARD_TAP) }
    fun tick() {
        val now = System.currentTimeMillis()
        if (now - lastTick < 45) return
        lastTick = now
        play("tick", .5f, 0.9f + (now % 7) * .03f)
        haptic(HapticFeedbackConstants.CLOCK_TICK)
    }
    fun boot() = play("boot", .5f)
    fun complete() {
        play("complete")
        haptic(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
    }
    fun levelUp() { play("levelup"); haptic(HapticFeedbackConstants.LONG_PRESS) }
    fun streak() = play("streak", .8f)
    fun error() {
        play("error", .8f)
        haptic(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
    }
    fun glitch() = play("glitch", .6f)

    fun release() = pool.release()
}

val LocalFx = staticCompositionLocalOf<Fx> { error("Fx not provided") }
