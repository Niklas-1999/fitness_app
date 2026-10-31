package dev.niklas.ironcore.ui.quest

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.domain.C25K
import dev.niklas.ironcore.domain.Rating
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.GlitchText
import dev.niklas.ironcore.ui.components.NeonBar
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import kotlinx.coroutines.delay

private data class RunPhase(val label: String, val seconds: Int, val run: Boolean)

/** NHS C25K run: 5 min warm-up walk, the run/walk segments, 5 min cool-down walk (RESEARCH §7). */
private fun phases(index: Int): List<RunPhase> {
    val r = C25K.run(index)
    val out = mutableListOf(RunPhase("WARM-UP WALK", 300, false))
    r.segments.forEach { (run, walk) ->
        out += RunPhase("RUN", run, true)
        if (walk > 0) out += RunPhase("WALK", walk, false)
    }
    out += RunPhase("COOL-DOWN WALK", 300, false)
    return out
}

private fun vibrate(ctx: Context, run: Boolean) {
    val v = if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(VibratorManager::class.java)).defaultVibrator
    else @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val pattern = if (run) longArrayOf(0, 400, 150, 400, 150, 400) else longArrayOf(0, 700)
    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
}

/** Interval timer with vibration on every phase change (phone can stay in the pocket). */
@Composable
fun RunTimer(index: Int, onClose: () -> Unit, onDone: (Rating) -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val run = remember { C25K.run(index) }
    val ph = remember { phases(index) }
    val total = ph.sumOf { it.seconds }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }    // accumulated while running
    var current by remember { mutableIntStateOf(0) }
    var skipOffsetMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = System.currentTimeMillis()
        while (running) {
            val now = System.currentTimeMillis()
            elapsedMs += now - last; last = now
            val e = (elapsedMs + skipOffsetMs) / 1000
            var acc = 0L
            var idx = ph.lastIndex
            for (i in ph.indices) { acc += ph[i].seconds; if (e < acc) { idx = i; break } }
            if (e >= total) { running = false; finished = true; fx.levelUp(); vibrate(ctx, false); break }
            if (idx != current) { current = idx; vibrate(ctx, ph[idx].run); if (ph[idx].run) fx.streak() else fx.boot() }
            delay(200)
        }
    }

    val e = ((elapsedMs + skipOffsetMs) / 1000).coerceAtMost(total.toLong())
    val phaseStart = ph.take(current).sumOf { it.seconds }
    val phaseLeft = (phaseStart + ph[current].seconds - e).coerceAtLeast(0)
    val cur = ph[current]

    RunnerFrame("R", run.label.replace(' ', '_'), "${total / 60} MIN TOTAL", onClose) {
        Txt(run.summary, T.mono(11.sp, p.textDim), Modifier.padding(bottom = 10.dp))
        if (finished) {
            GlitchText("RUN COMPLETE", T.head(30.sp, CyberPalette.UP, glow = 20f, weight = 900))
            Spacer(Modifier.height(12.dp))
            RatePanel(
                listOf(
                    "EASY" to "Could have run more",
                    "SOLID" to "Finished every run segment",
                    "HARD" to "Finished, but barely",
                    "FAILED" to "Had to walk during run segments (week repeats)"
                )
            ) { onDone(it) }
            return@RunnerFrame
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("PHASE ${current + 1}/${ph.size}", T.mono(11.sp, p.textDim, .3.em))
            GlitchText(cur.label, T.head(if (cur.run) 54.sp else 30.sp, if (cur.run) CyberPalette.UP else Color.White, glow = 24f, weight = 900))
            Txt("%d:%02d".format(phaseLeft / 60, phaseLeft % 60), T.head(64.sp, Color.White, glow = 26f, weight = 900, spacing = .02.em))
            NeonBar(1f - phaseLeft / cur.seconds.toFloat(), height = 8.dp, color = if (cur.run) CyberPalette.UP else p.accent, durationMs = 300)
            Spacer(Modifier.height(8.dp))
            ph.getOrNull(current + 1)?.let { Txt("NEXT // ${it.label} ${it.seconds / 60}:%02d".format(it.seconds % 60), T.mono(12.sp, p.textDim)) }
            Spacer(Modifier.height(14.dp))
            Txt("TOTAL ${e / 60}:%02d / ${total / 60}:%02d".format(e % 60, total % 60), T.mono(11.sp, p.textMute))
            NeonBar(e / total.toFloat(), height = 4.dp, durationMs = 300)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CyberButton(if (running) "PAUSE" else if (e == 0L) "START RUN" else "RESUME", Modifier.weight(2f), if (running) BtnStyle.OUTLINE else BtnStyle.SOLID) {
                    running = !running
                }
                CyberButton("SKIP >", Modifier.weight(1f), BtnStyle.OUTLINE) {
                    skipOffsetMs += phaseLeft * 1000L
                    if (!running) { // apply immediately when paused
                        val ee = (elapsedMs + skipOffsetMs) / 1000
                        if (ee >= total) finished = true
                        else { var acc = 0L; for (i in ph.indices) { acc += ph[i].seconds; if (ee < acc) { current = i; break } } }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Txt("Phone vibrates at every phase change. Screen stays on during the run.", T.body(13.sp, p.textMute, FontWeight.Medium))
        }
    }
}
