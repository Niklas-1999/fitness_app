package dev.niklas.ironcore.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.ui.fx.rememberFrameTime
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Everything the upgrade screen shows. Stats are "since the previous level-up". */
data class LevelUpInfo(
    val from: Int,
    val to: Int,
    val statNames: List<String>,
    val before: List<Int>,
    val after: List<Int>,
    val rank: String,
    val nextRank: String?,
    val rankProgress: Float
)

private fun pad2(i: Int) = i.toString().padStart(2, '0')

/**
 * Level-up as a terminal "system upgrade": glitch-in, typed log, glitched level number,
 * attribute diff since the last level-up, rank progress. Tap to acknowledge (after the sequence).
 */
@Composable
fun LevelUpTerminal(info: LevelUpInfo, onDismiss: () -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    val intro = remember { Animatable(0f) }
    var step by remember { mutableIntStateOf(0) }
    val time = rememberFrameTime()

    val log = listOf(
        "SYSTEM UPGRADE DETECTED",
        "VERIFYING TRAINING LOGS",
        "COMPILING OPERATIVE PROFILE",
        "FLASHING FIRMWARE  LV ${pad2(info.from)} > LV ${pad2(info.to)}"
    )
    val stepsTotal = log.size + 3 // log lines, level, table, rank

    LaunchedEffect(info) {
        fx.levelUp()
        intro.animateTo(1f, tween(500))
        while (step < stepsTotal) {
            delay(if (step < log.size) 170 else 320)
            step++
            if (step <= log.size) fx.boot() else fx.tick()
        }
    }
    val blink by rememberInfiniteTransition(label = "lb").animateFloat(1f, 0f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "b")

    Box(
        Modifier
            .fillMaxSize()
            .background(p.k01.copy(alpha = .93f * intro.value))
            .clickable(remember { MutableInteractionSource() }, null) { if (step >= stepsTotal) onDismiss() }
    ) {
        // glitch slices: heavy during intro, rare afterwards
        Canvas(Modifier.fillMaxSize()) {
            val t = time.value
            val heavy = 1f - intro.value
            val seed = (t * 30).toInt()
            val rnd = Random(seed)
            val count = if (heavy > 0f) 14 else if (rnd.nextFloat() < .08f) 3 else 0
            repeat(count) {
                val y = rnd.nextFloat() * size.height
                val h = (rnd.nextFloat() * 18 + 2).dp.toPx()
                val c = when (rnd.nextInt(3)) { 0 -> CyberPalette.CYAN; 1 -> CyberPalette.MAGENTA; else -> p.accent }
                drawRect(c.copy(alpha = .08f + .25f * heavy), Offset(rnd.nextFloat() * size.width * .3f, y), Size(size.width, h))
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    alpha = intro.value
                    translationX = if (intro.value < 1f) (Random.nextFloat() - .5f) * 30f * (1 - intro.value) else 0f
                },
            verticalArrangement = Arrangement.Center
        ) {
            CyberPanel(Modifier.fillMaxWidth()) {
                PanelHeader("SYS", "PATCH_INSTALLER", "ROOT ACCESS", live = true)
                // typed log
                val text = buildAnnotatedString {
                    log.take(step).forEachIndexed { i, l ->
                        append("> $l ")
                        append(".".repeat((36 - l.length).coerceAtLeast(2)))
                        append(" ")
                        withStyle(SpanStyle(color = CyberPalette.UP)) { append(if (i == 0) "!!" else "OK") }
                        append("\n")
                    }
                    if (step < log.size) append("> _")
                }
                Txt(text, T.mono(11.sp, p.accent, 0.sp, glow = p.acc(.5f)).copy(lineHeight = 1.55.em))

                if (step > log.size) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Txt("// ACCESS LEVEL ELEVATED", T.mono(10.sp, p.textDim, .2.em))
                            GlitchText("LEVEL ${pad2(info.to)}", T.head(40.sp, Color.White, glow = 26f, weight = 900, spacing = .04.em), periodMs = 1300)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Txt("PREV", T.mono(9.sp, p.textMute))
                            Txt("LV ${pad2(info.from)}", T.mono(16.sp, p.textDim))
                        }
                    }
                }

                if (step > log.size + 1) {
                    Spacer(Modifier.height(14.dp))
                    Txt("ATTRIBUTE DELTA // SINCE LV ${pad2(info.from)}", T.mono(10.sp, p.textMute, .16.em))
                    Spacer(Modifier.height(6.dp))
                    val maxStat = (info.after.maxOrNull() ?: 1).coerceAtLeast(1)
                    info.statNames.forEachIndexed { i, name ->
                        val d = info.after[i] - info.before[i]
                        StatDiffRow(name, info.before[i], info.after[i], d, maxStat, delayMs = i * 90)
                    }
                }

                if (step > log.size + 2) {
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Txt("RANK // ${info.rank}", T.mono(11.sp, Color.White))
                        Spacer(Modifier.weight(1f))
                        Txt(info.nextRank?.let { "NEXT: $it  ${(info.rankProgress * 100).toInt()}%" } ?: "MAX TIER", T.mono(11.sp, p.textDim))
                    }
                    Spacer(Modifier.height(5.dp))
                    NeonBar(info.rankProgress, height = 6.dp, segments = 20)
                    Spacer(Modifier.height(16.dp))
                    Txt("[ TAP TO ACKNOWLEDGE ]", T.mono(10.sp, p.textMute.copy(alpha = blink), .3.em),
                        Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun StatDiffRow(name: String, before: Int, after: Int, delta: Int, maxStat: Int, delayMs: Int) {
    val p = Cyber.c
    val grow = remember { Animatable(before / maxStat.toFloat()) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        grow.animateTo(after / maxStat.toFloat(), tween(700, easing = FastOutSlowInEasing))
    }
    val up = delta > 0
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(name, T.mono(12.sp, if (up) Color.White else p.textMute), Modifier.width(40.dp))
        Txt(before.toString().padStart(3), T.mono(12.sp, p.textMute), Modifier.width(30.dp))
        Txt(">", T.mono(12.sp, p.textMute), Modifier.width(16.dp))
        Txt(after.toString().padStart(3), T.mono(12.sp, if (up) Color.White else p.textMute), Modifier.width(30.dp))
        Canvas(Modifier.weight(1f).height(6.dp).padding(horizontal = 6.dp)) {
            drawRect(p.accDeep)
            drawRect(p.accDim, size = Size(size.width * before / maxStat, size.height))
            if (up) {
                val x0 = size.width * before / maxStat
                drawRect(CyberPalette.UP, Offset(x0, 0f), Size((size.width * grow.value - x0).coerceAtLeast(0f), size.height))
            }
        }
        Txt(if (up) "+$delta" else "±0", T.mono(12.sp, if (up) CyberPalette.UP else p.textMute, glow = if (up) CyberPalette.UP else null),
            Modifier.width(34.dp))
    }
}
