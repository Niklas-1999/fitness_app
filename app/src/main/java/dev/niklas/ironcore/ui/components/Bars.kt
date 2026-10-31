package dev.niklas.ironcore.ui.components

import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * Progress bar like `.pbar`: dark track, accent gradient fill with glow, a light sheen
 * sweeping across. [onTick] fires while the fill grows (used for the XP tick sound).
 */
@Composable
fun NeonBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = Cyber.c.accent,
    segments: Int = 0,
    durationMs: Int = 1400,
    onTick: (() -> Unit)? = null
) {
    val p = Cyber.c
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        var last = anim.value
        anim.animateTo(progress.coerceIn(0f, 1f), tween(durationMs, easing = FastOutSlowInEasing)) {
            if (onTick != null && value - last > .04f) { onTick(); last = value }
        }
    }
    val shine by rememberInfiniteTransition(label = "shine").animateFloat(
        -.3f, 1.3f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "s"
    )
    Canvas(modifier.fillMaxWidth().height(height)) {
        drawRect(p.accDeep)
        val w = size.width * anim.value
        if (w > 0f) {
            // glow
            drawRect(color.copy(alpha = .25f), Offset(0f, -3.dp.toPx()), Size(w, size.height + 6.dp.toPx()))
            drawRect(Brush.horizontalGradient(listOf(color.copy(alpha = .55f), color), 0f, w), size = Size(w, size.height))
            clipRect(right = w) {
                val sx = size.width * shine
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .6f), Color.Transparent), sx - 60f, sx + 60f),
                    Offset(sx - 60f, 0f), Size(120f, size.height)
                )
            }
            // bright head
            drawRect(Color.White, Offset(w - 2.dp.toPx(), -2.dp.toPx()), Size(2.dp.toPx(), size.height + 4.dp.toPx()))
        }
        if (segments > 1) {
            for (i in 1 until segments) {
                val x = size.width * i / segments
                drawLine(p.bg, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            }
        }
    }
}

/** Number that counts up/down to [target] (like the dashboard's ticking values). */
@Composable
fun CountUpText(target: Int, style: TextStyle, modifier: Modifier = Modifier, format: (Int) -> String = { it.toString() }) {
    val anim = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), tween(900, easing = FastOutSlowInEasing)) }
    Txt(format(anim.value.toInt()), style, modifier)
}

/**
 * Glitch text: base text plus cyan / magenta copies that jump into clipped bands
 * near the end of every 4 s cycle (CSS `.glitch::before/::after`).
 */
@Composable
fun GlitchText(text: String, style: TextStyle, modifier: Modifier = Modifier, periodMs: Int = 4000) {
    val t by rememberInfiniteTransition(label = "glitch").animateFloat(
        0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "t"
    )
    // keyframes from the stylesheet: (start, top%, bottom%, dx)
    val a = listOf(Triple(.87f, .10f to .40f, -4f), Triple(.89f, .55f to .80f, 3f), Triple(.91f, .30f to .55f, -2f), Triple(.93f, .80f to .95f, 4f))
    val b = listOf(Triple(.88f, .65f to .90f, 4f), Triple(.90f, .05f to .25f, -3f), Triple(.92f, .40f to .65f, 2f))
    fun pick(k: List<Triple<Float, Pair<Float, Float>, Float>>) = k.lastOrNull { t >= it.first && t < it.first + .02f }
    val ka = pick(a)
    val kb = pick(b)
    Box(modifier) {
        Txt(text, style)
        if (ka != null) Txt(text, style.copy(color = CyberPalette.CYAN.copy(alpha = .8f), shadow = null),
            Modifier.drawWithContent {
                clipRect(top = size.height * ka.second.first, bottom = size.height * ka.second.second) {
                    translate(ka.third.dp.toPx() / 2) { this@drawWithContent.drawContent() }
                }
            })
        if (kb != null) Txt(text, style.copy(color = CyberPalette.MAGENTA.copy(alpha = .8f), shadow = null),
            Modifier.drawWithContent {
                clipRect(top = size.height * kb.second.first, bottom = size.height * kb.second.second) {
                    translate(kb.third.dp.toPx() / 2) { this@drawWithContent.drawContent() }
                }
            })
    }
}

/** Pentagon radar for the 5 stats; values 0..1. Animates in from the center. */
@Composable
fun StatRadar(values: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val p = Cyber.c
    val anim = remember { Animatable(0f) }
    LaunchedEffect(values) { anim.snapTo(.0f); anim.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    val pulse by rememberInfiniteTransition(label = "rad").animateFloat(
        .6f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "p"
    )
    val labelPaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    Canvas(modifier) {
        val n = values.size
        val c = Offset(size.width / 2, size.height / 2 + 6.dp.toPx())
        val r = size.minDimension / 2 - 26.dp.toPx()
        fun pt(i: Int, f: Float): Offset {
            val ang = -Math.PI / 2 + 2 * Math.PI * i / n
            return Offset(c.x + (r * f * cos(ang)).toFloat(), c.y + (r * f * sin(ang)).toFloat())
        }
        for (ring in 1..4) {
            val path = Path()
            for (i in 0 until n) { val q = pt(i, ring / 4f); if (i == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y) }
            path.close()
            drawPath(path, if (ring == 4) p.line2 else p.line, style = Stroke(1.dp.toPx()))
        }
        for (i in 0 until n) drawLine(p.line, c, pt(i, 1f), 1.dp.toPx())
        val shape = Path()
        for (i in 0 until n) {
            val q = pt(i, values[i].coerceIn(.04f, 1f) * anim.value)
            if (i == 0) shape.moveTo(q.x, q.y) else shape.lineTo(q.x, q.y)
        }
        shape.close()
        drawPath(shape, p.acc(.22f * pulse))
        drawPath(shape, p.acc(.25f), style = Stroke(6.dp.toPx()))
        drawPath(shape, p.accent, style = Stroke(2.dp.toPx()))
        for (i in 0 until n) {
            val q = pt(i, values[i].coerceIn(.04f, 1f) * anim.value)
            drawCircle(Color.White, 3.dp.toPx(), q)
            drawCircle(p.acc(.4f), 6.dp.toPx(), q, style = Stroke(1.dp.toPx()))
        }
        labelPaint.apply {
            color = android.graphics.Color.argb(255, (p.textDim.red * 255).toInt(), (p.textDim.green * 255).toInt(), (p.textDim.blue * 255).toInt())
            textSize = 11.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; letterSpacing = .15f
        }
        for (i in 0 until n) {
            val q = pt(i, 1.22f)
            drawContext.canvas.nativeCanvas.drawText(labels[i], q.x, q.y + 4.dp.toPx(), labelPaint)
        }
    }
}
