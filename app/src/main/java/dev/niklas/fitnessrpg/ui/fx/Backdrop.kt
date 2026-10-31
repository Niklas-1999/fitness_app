package dev.niklas.fitnessrpg.ui.fx

import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import dev.niklas.fitnessrpg.R
import dev.niklas.fitnessrpg.ui.theme.Cyber
import dev.niklas.fitnessrpg.ui.theme.CyberPalette
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Seconds since first frame. Read `.value` only inside draw lambdas so only drawing invalidates. */
@Composable
fun rememberFrameTime(): State<Float> {
    return produceState(0f) {
        var start = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (start < 0) start = now
                value = (now - start) / 1_000_000_000f
            }
        }
    }
}

private const val GLYPHS = "01ΔΣΩ+×XPLVATK0123456789アイウエオカキクケコ"

private class Drop(var x: Float, var y: Float, val v: Float, val len: Int, var on: Boolean, val seed: Int)
private class Dust(var x: Float, var y: Float, val r: Float, val vx: Float, val vy: Float, var a: Float)

private class World {
    var w = 0f
    var h = 0f
    val rain = ArrayList<Drop>()
    val dust = ArrayList<Dust>()
    var glitchUntil = 0f
    var lastT = 0f

    fun resize(nw: Float, nh: Float, step: Float, density: Float) {
        if (nw == w && nh == h) return
        w = nw; h = nh
        rain.clear(); dust.clear()
        val cols = (w / step).toInt()
        repeat(cols) { i ->
            rain += Drop(i * step + 8f, -Random.nextFloat() * h, (0.6f + Random.nextFloat() * 1.8f) * density, 6 + Random.nextInt(14), Random.nextFloat() < .35f, Random.nextInt(1000))
        }
        repeat((w * h / (26000f * density * density)).toInt()) {
            dust += Dust(Random.nextFloat() * w, Random.nextFloat() * h, (Random.nextFloat() * 1.6f + .3f) * density,
                (Random.nextFloat() - .5f) * .15f * density, (-Random.nextFloat() * .3f - .05f) * density, Random.nextFloat() * 6f)
        }
    }
}

/**
 * Full-screen animated backdrop: radial washes, synthwave sun, perspective grid,
 * data rain, dust and random glitch bars (port of Cryptobro's bg.js), then content,
 * then scanlines / vignette / sweep on top.
 */
@Composable
fun CyberBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = Cyber.c
    val ctx = LocalContext.current
    val world = remember { World() }
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(ctx, R.font.share_tech_mono) ?: Typeface.MONOSPACE
        }
    }
    val time = rememberFrameTime()
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val t = time.value
            val d = density
            world.resize(size.width, size.height, 26.dp.toPx(), d)
            val dt = ((t - world.lastT) * 60f).coerceIn(0f, 3f) // in 60fps frames
            world.lastT = t
            drawRect(p.bg)
            drawRect(Brush.radialGradient(listOf(p.acc(.18f), Color.Transparent), Offset(size.width / 2, -size.height * .1f), size.height * .55f))
            drawRect(Brush.radialGradient(listOf(p.k47.copy(alpha = .25f), Color.Transparent), Offset(size.width, size.height), size.width * .8f))
            grid(p, t)
            rain(p, world, t, dt, paint)
            dust(p, world, dt)
            if (t > world.glitchUntil && Random.nextFloat() < .004f * dt) world.glitchUntil = t + .12f + Random.nextFloat() * .2f
            if (t < world.glitchUntil) repeat(3) {
                drawRect(p.acc(Random.nextFloat() * .12f), Offset(0f, Random.nextFloat() * size.height), Size(size.width, (Random.nextFloat() * 6 + 1) * d))
            }
        }
        content()
        ScreenFx(time)
    }
}

private fun DrawScope.grid(p: CyberPalette, t: Float) {
    val w = size.width
    val h = size.height
    val horizon = h * .62f
    val vx = w / 2
    drawRect(Brush.verticalGradient(0f to Color.Transparent, .8f to p.acc(.10f), 1f to Color.Transparent, startY = horizon - 120.dp.toPx(), endY = horizon + 40.dp.toPx()),
        Offset(0f, horizon - 120.dp.toPx()), Size(w, 160.dp.toPx()))
    // sun ring with scrolling stripes
    val sr = min(w, h) * .16f
    clipRect(bottom = horizon) {
        drawArc(Brush.verticalGradient(listOf(p.light(.08f, .18f), p.tint(.47f, .04f)), horizon - sr, horizon), 180f, 180f, true,
            Offset(vx - sr, horizon - sr), Size(sr * 2, sr * 2))
        val step = sr * .12f
        for (i in 0 until 7) {
            val yy = horizon - sr * .08f - i * step - ((t * 10f) % step)
            drawRect(p.tint(.02f, .9f), Offset(vx - sr, yy), Size(sr * 2, (2f + i * .6f) * density / 1.5f))
        }
    }
    clipRect(top = horizon) {
        val lw = 1f * density / 1.5f
        for (i in -24..24) {
            val xb = vx + i * w * .09f
            drawLine(p.acc(max(.22f - abs(i) * .006f, .04f)), Offset(vx + i * 6f, horizon), Offset(xb, h), lw)
        }
        val speed = (t * .25f) % 1f
        for (i in 0 until 22) {
            val z = (i + speed) / 22f
            val y = horizon + z.pow(2.4f) * (h - horizon)
            drawLine(p.acc(.05f + z * .28f), Offset(0f, y), Offset(w, y), lw)
        }
    }
}

private fun DrawScope.rain(p: CyberPalette, world: World, t: Float, dt: Float, paint: Paint) {
    val step = 16.dp.toPx() * .9f
    paint.textSize = 13.dp.toPx() * .85f
    val canvas = drawContext.canvas.nativeCanvas
    val head = p.light(.78f)
    for (c in world.rain) {
        if (!c.on) { if (Random.nextFloat() < .0008f * dt) { c.on = true; c.y = -c.len * step }; continue }
        c.y += c.v * dt
        for (k in 0 until c.len) {
            val y = c.y - k * step
            if (y < -step || y > size.height) continue
            val a = (1 - k / c.len.toFloat()) * .22f
            val idx = Math.floorMod((c.seed + k * 7 + floor(t * 1000 / 180).toInt()), GLYPHS.length)
            paint.color = (if (k == 0) head.copy(alpha = min(1f, a * 2)) else p.acc(a)).toArgb()
            canvas.drawText(GLYPHS, idx, idx + 1, c.x, y, paint)
        }
        if (c.y - c.len * step > size.height) { c.on = Random.nextFloat() < .5f; c.y = -Random.nextFloat() * size.height * .5f }
    }
}

private fun DrawScope.dust(p: CyberPalette, world: World, dt: Float) {
    for (d in world.dust) {
        d.x += d.vx * dt; d.y += d.vy * dt; d.a += .01f * dt
        if (d.y < -5) { d.y = size.height + 5; d.x = Random.nextFloat() * size.width }
        if (d.x < -5) d.x = size.width + 5
        if (d.x > size.width + 5) d.x = -5f
        drawCircle(p.light(.15f, (.25f + sin(d.a) * .2f).coerceIn(0f, 1f)), d.r, Offset(d.x, d.y))
    }
}

/** Scanlines, vignette and the slow sweep band (`.fx-*` overlays). */
@Composable
private fun ScreenFx(time: State<Float>) {
    val p = Cyber.c
    Canvas(Modifier.fillMaxSize()) {
        val t = time.value
        // sweep: 9 s top -> bottom
        val band = 160.dp.toPx()
        val y = -band + ((t % 9f) / 9f) * (size.height + band * 2)
        drawRect(Brush.verticalGradient(0f to Color.Transparent, .6f to p.acc(.05f), .98f to p.acc(.12f), 1f to Color.Transparent, startY = y, endY = y + band),
            Offset(0f, y), Size(size.width, band))
        // vignette
        drawRect(Brush.radialGradient(.55f to Color.Transparent, 1f to Color.Black.copy(alpha = .65f),
            center = Offset(size.width / 2, size.height / 2), radius = max(size.width, size.height) * .75f))
    }
    // static layer: drawn once, not every frame
    Canvas(Modifier.fillMaxSize()) {
        // scanlines every 3px
        val s = 3f
        var yy = 0f
        val c = Color.Black.copy(alpha = .18f)
        while (yy < size.height) { drawRect(c, Offset(0f, yy), Size(size.width, 1f)); yy += s }
    }
}
