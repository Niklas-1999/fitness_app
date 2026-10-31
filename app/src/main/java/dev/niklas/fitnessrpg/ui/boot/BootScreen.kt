package dev.niklas.fitnessrpg.ui.boot

import dev.niklas.fitnessrpg.ui.components.Txt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.fitnessrpg.audio.LocalFx
import dev.niklas.fitnessrpg.ui.components.GlitchText
import dev.niklas.fitnessrpg.ui.components.NeonBar
import dev.niklas.fitnessrpg.ui.theme.Cyber
import dev.niklas.fitnessrpg.ui.theme.CyberPalette
import dev.niklas.fitnessrpg.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LINES = listOf(
    "BOOTING NEURAL TRAINING KERNEL v0.1",
    "MOUNTING BIOMETRIC INTERFACE",
    "LOADING EXERCISE DATABASE",
    "CALIBRATING PROGRESSION ENGINE",
    "SYNCING NUTRITION MATRIX",
    "DECRYPTING DAILY DIRECTIVE",
    "CHARGING XP CAPACITORS",
    "CALIBRATING HOLO-DISPLAY"
)

/** Terminal boot log with progress bar. Tap to skip. Fades out with blur + flash. */
@Composable
fun BootScreen(title: String, subtitle: String, onDone: () -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    var shown by remember { mutableIntStateOf(0) }
    var finishing by remember { mutableStateOf(false) }
    val out = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun finish() {
        if (finishing) return
        finishing = true
        scope.launch {
            out.animateTo(1f, tween(700))
            onDone()
        }
    }

    LaunchedEffect(Unit) {
        delay(350)
        while (shown < LINES.size) {
            shown++
            fx.boot()
            delay(150L + (0..140).random())
        }
        delay(450)
        finish()
    }
    val blink by rememberInfiniteTransition(label = "b").animateFloat(
        1f, 0f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "b"
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - out.value
                val s = 1f + .06f * out.value; scaleX = s; scaleY = s
            }
            .blur((8 * out.value).dp)
            .background(p.k01)
            .clickable(remember { MutableInteractionSource() }, null) { finish() },
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp)) {
            GlitchText(title, T.head(34.sp, p.accent, glow = 24f, weight = 900, spacing = .12.em), periodMs = 2200)
            Txt(subtitle, T.mono(10.sp, p.textDim, .3.em), Modifier.padding(top = 6.dp, bottom = 22.dp))
            val log = buildAnnotatedString {
                LINES.take(shown).forEach { l ->
                    append("> $l ")
                    append(".".repeat((34 - l.length).coerceAtLeast(2)))
                    append(" ")
                    withStyle(SpanStyle(color = CyberPalette.UP, shadow = Shadow(CyberPalette.UP, blurRadius = 10f))) { append("OK") }
                    append("\n")
                }
            }
            Txt(log, T.mono(11.5.sp, p.accent, 0.sp, glow = p.acc(.6f)).copy(lineHeight = 1.6.em), Modifier.heightIn(min = 190.dp))
            Spacer(Modifier.height(14.dp))
            NeonBar(shown / LINES.size.toFloat(), height = 4.dp, durationMs = 250)
            Txt("[ TAP TO SKIP ]", T.mono(10.sp, p.textMute.copy(alpha = blink), .3.em), Modifier.padding(top = 12.dp))
        }
    }
}
