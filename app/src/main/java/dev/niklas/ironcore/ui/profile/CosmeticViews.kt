package dev.niklas.ironcore.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.ui.components.GlitchText
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.fx.rememberFrameTime
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ---------------- geometry ---------------- */

/** Hexagon (pointy top) inscribed in a rect, optionally inset. */
fun hexPath(size: Size, inset: Float = 0f, rotationDeg: Float = 0f): Path {
    val cx = size.width / 2; val cy = size.height / 2
    val r = minOf(size.width, size.height) / 2 - inset
    return Path().apply {
        for (i in 0 until 6) {
            val a = Math.toRadians((-90 + 60 * i + rotationDeg).toDouble())
            val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

val HexShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(hexPath(size))
}

private fun DrawScope.glowPath(path: Path, color: Color, width: Float) {
    for ((w, a) in listOf(width * 5 to .10f, width * 2.6f to .22f, width to 1f)) drawPath(path, color.copy(alpha = color.alpha * a), style = Stroke(w))
}

/* ---------------- IRONCORE logo ---------------- */

/**
 * The IRONCORE emblem: outer hex shell, counter-rotated inner hex, an I-beam "core" split into
 * three iron plates, circuit nodes at the vertices. Theme-colored; pulses and spins slowly.
 */
@Composable
fun IroncoreLogo(modifier: Modifier, animated: Boolean = true) {
    val p = Cyber.c
    val tr = rememberInfiniteTransition(label = "logo")
    val spin by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "s")
    val pulse by tr.animateFloat(.55f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "p")
    Canvas(modifier) { drawLogo(p.accent, p.k07, if (animated) spin else 0f, if (animated) pulse else 1f) }
}

fun DrawScope.drawLogo(acc: Color, dark: Color, spin: Float, pulse: Float) {
    val s = size.minDimension
    val c = center
    val stroke = s * .045f
    // halo
    drawCircle(Brush.radialGradient(listOf(acc.copy(alpha = .35f * pulse), Color.Transparent), c, s * .62f), s * .62f, c)
    // outer shell
    val outer = hexPath(size, inset = s * .06f)
    drawPath(outer, dark)
    glowPath(outer, acc, stroke)
    // circuit nodes at vertices
    for (i in 0 until 6) {
        val a = Math.toRadians((-90 + 60 * i).toDouble())
        val r1 = s / 2 - s * .06f; val r2 = s / 2 + s * .02f
        val p1 = Offset(c.x + (r1 * cos(a)).toFloat(), c.y + (r1 * sin(a)).toFloat())
        val p2 = Offset(c.x + (r2 * cos(a)).toFloat(), c.y + (r2 * sin(a)).toFloat())
        drawLine(acc.copy(alpha = .8f), p1, p2, stroke * .6f)
        drawCircle(acc, stroke * .75f, p2)
    }
    // inner counter-rotating hex (dashed)
    rotate(-spin, c) {
        drawPath(hexPath(size, inset = s * .2f, rotationDeg = 30f), acc.copy(alpha = .55f),
            style = Stroke(stroke * .55f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * .08f, s * .04f))))
    }
    // I-beam core: caps + three plates
    val w = s * .2f; val capW = s * .36f; val capH = s * .07f
    val top = c.y - s * .27f; val bottom = c.y + s * .27f
    for (y in listOf(top, bottom - capH)) drawRect(acc, Offset(c.x - capW / 2, y), Size(capW, capH))
    val gap = s * .035f
    val inner = (bottom - capH) - (top + capH) - 2 * gap
    val plate = (inner - 2 * gap) / 3
    for (k in 0 until 3) {
        val y = top + capH + gap + k * (plate + gap)
        drawRect(acc.copy(alpha = .9f), Offset(c.x - w / 2, y), Size(w, plate))
    }
    // reactor line
    drawLine(Color.White.copy(alpha = .9f * pulse), Offset(c.x, top + capH), Offset(c.x, bottom - capH), stroke * .7f)
    drawLine(Color.White.copy(alpha = .25f * pulse), Offset(c.x, top + capH), Offset(c.x, bottom - capH), stroke * 2.4f)
}

/* ---------------- avatar ---------------- */

@Composable
fun rememberImage(path: String?) = remember(path) { path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }

@Composable
fun Avatar(path: String?, frame: String, letter: String, modifier: Modifier) {
    val p = Cyber.c
    val img = rememberImage(path)
    val time = rememberFrameTime()
    Box(
        modifier.drawWithContent {
            drawContent()
            val t = time.value
            val hex = hexPath(size)
            when (frame) {
                "f_double" -> {
                    glowPath(hex, p.accent, 2.dp.toPx())
                    rotate(t * 20f, center) { drawPath(hexPath(size, -5.dp.toPx(), 30f), p.acc(.6f), style = Stroke(1.5.dp.toPx())) }
                }
                "f_circuit" -> {
                    glowPath(hex, p.accent, 2.dp.toPx())
                    for (i in 0 until 6 step 2) {
                        val a = Math.toRadians((-90 + 60 * i).toDouble())
                        val r = size.minDimension / 2
                        val v = Offset(center.x + (r * cos(a)).toFloat(), center.y + (r * sin(a)).toFloat())
                        val e = Offset(center.x + ((r + 12.dp.toPx()) * cos(a)).toFloat(), center.y + ((r + 12.dp.toPx()) * sin(a)).toFloat())
                        drawLine(p.accent, v, e, 1.5.dp.toPx())
                        drawLine(p.accent, e, e + Offset(if (i == 2) 8.dp.toPx() else -8.dp.toPx(), 0f), 1.5.dp.toPx())
                        val pulse = ((t * 1.5f + i) % 1f)
                        drawCircle(Color.White.copy(alpha = 1f - pulse), 2.5.dp.toPx(), v + (e - v) * pulse)
                    }
                }
                "f_glitch" -> {
                    val jitter = ((t * 3).toInt() % 7 == 0)
                    val dx = if (jitter) 3.dp.toPx() else 1.dp.toPx()
                    translate(-dx, 0f) { drawPath(hex, CyberPalette.CYAN.copy(alpha = .8f), style = Stroke(2.dp.toPx())) }
                    translate(dx, 0f) { drawPath(hex, CyberPalette.MAGENTA.copy(alpha = .8f), style = Stroke(2.dp.toPx())) }
                    glowPath(hex, p.accent, 1.5.dp.toPx())
                }
                "f_apex" -> {
                    glowPath(hex, CyberPalette.AMBER, 2.dp.toPx())
                    rotate(t * 30f, center) {
                        drawCircle(CyberPalette.AMBER.copy(alpha = .6f), size.minDimension / 2 + 7.dp.toPx(), center,
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                    }
                    val top = Offset(center.x, 0f)
                    val crown = Path().apply {
                        moveTo(top.x - 16.dp.toPx(), top.y - 2.dp.toPx()); lineTo(top.x - 12.dp.toPx(), top.y - 14.dp.toPx())
                        lineTo(top.x - 5.dp.toPx(), top.y - 6.dp.toPx()); lineTo(top.x, top.y - 18.dp.toPx())
                        lineTo(top.x + 5.dp.toPx(), top.y - 6.dp.toPx()); lineTo(top.x + 12.dp.toPx(), top.y - 14.dp.toPx())
                        lineTo(top.x + 16.dp.toPx(), top.y - 2.dp.toPx()); close()
                    }
                    drawPath(crown, CyberPalette.AMBER)
                }
                else -> glowPath(hex, p.accent, 2.dp.toPx())
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize().padding(2.dp).clip(HexShape).background(p.k07), contentAlignment = Alignment.Center) {
            if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Txt(letter, T.head(28.sp, p.accent, glow = 16f, weight = 900))
        }
    }
}

/* ---------------- banner ---------------- */

@Composable
fun Banner(path: String?, style: String, modifier: Modifier) {
    val p = Cyber.c
    val img = rememberImage(path)
    val time = rememberFrameTime()
    Box(modifier.clip(androidx.compose.ui.graphics.RectangleShape).background(p.k04)) {
        if (img != null) {
            Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            // tint into the theme so photos still look like part of the terminal
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, p.k02.copy(alpha = .85f))))
                drawRect(p.acc(.10f))
            }
        } else Canvas(Modifier.fillMaxSize()) {
            val t = time.value
            when (style) {
                "b_rain" -> {
                    drawRect(p.k02)
                    val cols = 28
                    for (i in 0 until cols) {
                        val x = size.width * i / cols
                        val speed = 40f + (i * 37 % 60)
                        val y = ((t * speed + i * 53) % (size.height + 80f)) - 80f
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, p.acc(.7f)), y, y + 70f), Offset(x, y), Size(3f, 70f))
                        drawRect(Color.White, Offset(x, y + 66f), Size(3f, 4f))
                    }
                }
                "b_static" -> {
                    drawRect(p.k04)
                    val rnd = Random((t * 12).toInt())
                    repeat(220) {
                        drawRect(p.acc(rnd.nextFloat() * .5f), Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height), Size(rnd.nextFloat() * 30f + 2f, 2f))
                    }
                    val y = (t * 60f) % size.height
                    drawRect(Color.White.copy(alpha = .08f), Offset(0f, y), Size(size.width, 10f))
                }
                "b_sunset" -> {
                    drawRect(Brush.verticalGradient(listOf(p.k05, p.k16, p.k47)))
                    val c = Offset(size.width * .5f, size.height * .95f); val r = size.height * .7f
                    drawCircle(Brush.verticalGradient(listOf(CyberPalette.AMBER, p.accent), c.y - r, c.y), r, c)
                    for (k in 0 until 6) {
                        val yy = c.y - r * .1f - k * r * .14f - (t * 8f % (r * .14f))
                        drawRect(p.k05, Offset(c.x - r, yy), Size(r * 2, 3f + k))
                    }
                }
                "b_circuit" -> {
                    drawRect(p.k04)
                    val rnd = Random(77)
                    repeat(14) { k ->
                        val y = size.height * rnd.nextFloat(); val x1 = size.width * rnd.nextFloat() * .4f
                        val x2 = x1 + size.width * (.3f + rnd.nextFloat() * .4f); val y2 = y + (rnd.nextFloat() - .5f) * size.height * .4f
                        val path = Path().apply { moveTo(x1, y); lineTo((x1 + x2) / 2, y); lineTo((x1 + x2) / 2 + 20f, y2); lineTo(x2, y2) }
                        drawPath(path, p.acc(.45f), style = Stroke(2f))
                        drawCircle(p.accent, 4f, Offset(x2, y2))
                        val ph = ((t * .5f + k * .13f) % 1f)
                        val px = x1 + (x2 - x1) * ph
                        drawCircle(Color.White.copy(alpha = .8f), 3f, Offset(px, if (px < (x1 + x2) / 2) y else y2))
                    }
                }
                else -> { // b_grid
                    drawRect(Brush.verticalGradient(listOf(p.k02, p.k11)))
                    val horizon = size.height * .45f
                    for (i in -12..12) drawLine(p.acc(.35f), Offset(size.width / 2 + i * 8f, horizon), Offset(size.width / 2 + i * size.width * .12f, size.height), 1.5f)
                    for (k in 0 until 8) {
                        val z = ((k + (t * .3f % 1f)) / 8f)
                        val y = horizon + z * z * (size.height - horizon)
                        drawLine(p.acc(.15f + z * .5f), Offset(0f, y), Offset(size.width, y), 1.5f)
                    }
                }
            }
            // scanlines
            var yy = 0f
            while (yy < size.height) { drawRect(Color.Black.copy(alpha = .2f), Offset(0f, yy), Size(size.width, 1f)); yy += 3f }
        }
    }
}

/* ---------------- name effects ---------------- */

@Composable
fun StyledName(text: String, fx: String, style: TextStyle, modifier: Modifier = Modifier) {
    when (fx) {
        "n_glitch" -> GlitchText(text, style, modifier, periodMs = 2600)
        "n_pulse" -> {
            val a by rememberInfiniteTransition(label = "np").animateFloat(4f, 26f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
            Txt(text, style.copy(shadow = androidx.compose.ui.graphics.Shadow(Cyber.c.accent, blurRadius = a)), modifier)
        }
        "n_chroma" -> Box(modifier) {
            Txt(text, style.copy(color = CyberPalette.CYAN.copy(alpha = .7f), shadow = null), Modifier.padding(start = 0.dp).drawWithContent { translate(-2.5f, 0f) { this@drawWithContent.drawContent() } })
            Txt(text, style.copy(color = CyberPalette.MAGENTA.copy(alpha = .7f), shadow = null), Modifier.drawWithContent { translate(2.5f, 0f) { this@drawWithContent.drawContent() } })
            Txt(text, style)
        }
        "n_flicker" -> {
            val time = rememberFrameTime()
            Txt(text, style, modifier.drawWithContent {
                val t = time.value
                val off = ((t * 7).toInt() % 23 == 0) || ((t * 13).toInt() % 31 == 0)
                drawContent()
                if (off) drawRect(Color.Black.copy(alpha = .6f))
            })
        }
        else -> Txt(text, style, modifier)
    }
}
