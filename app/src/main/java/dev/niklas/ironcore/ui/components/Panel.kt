package dev.niklas.ironcore.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T

/** Polygon with the top-right and bottom-left corners cut off (the dashboard's clip-path). */
fun chamferPath(size: Size, cut: Float) = Path().apply {
    moveTo(0f, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height)
    lineTo(cut, size.height)
    lineTo(0f, size.height - cut)
    close()
}

fun ChamferShape(cut: Dp): Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(chamferPath(size, with(density) { cut.toPx() }))
}

/** Neon frame: thin border on the chamfer, glowing corner brackets and cut diagonals. */
fun DrawScope.neonFrame(p: CyberPalette, cut: Float, bracket: Float = 26.dp.toPx(), bright: Float = 1f) {
    val w = size.width
    val h = size.height
    drawPath(chamferPath(size, cut), p.line.copy(alpha = p.line.alpha * bright), style = Stroke(1.dp.toPx()))
    val acc = p.accent
    val segs = listOf(
        Offset(0f, 0f) to Offset(bracket, 0f), Offset(0f, 0f) to Offset(0f, bracket),
        Offset(w, h) to Offset(w - bracket, h), Offset(w, h) to Offset(w, h - bracket),
        Offset(w - cut, 0f) to Offset(w, cut), Offset(0f, h - cut) to Offset(cut, h)
    )
    // glow passes, then the hard core line
    for ((sw, a) in listOf(9.dp.toPx() to .10f, 5.dp.toPx() to .22f, 2.dp.toPx() to 1f)) {
        segs.forEach { (s, e) -> drawLine(acc.copy(alpha = a * bright), s, e, strokeWidth = sw) }
    }
}

/**
 * Glass panel with chamfered corners and neon frame. Slides/blur-fades in on first show,
 * like `.panel { animation: panelIn }`.
 */
@Composable
fun CyberPanel(
    modifier: Modifier = Modifier,
    cut: Dp = 14.dp,
    delayMs: Int = 0,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = Cyber.c
    val cutPx = with(LocalDensity.current) { cut.toPx() }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        appear.animateTo(1f, tween(800, easing = CubicBezierEasing(.2f, .8f, .2f, 1f)))
    }
    Box(
        modifier
            .graphicsLayer {
                alpha = appear.value
                translationY = (1 - appear.value) * 20.dp.toPx()
                scaleX = .98f + .02f * appear.value; scaleY = scaleX
            }
            .drawWithContent {
                drawContent()
                neonFrame(p, cutPx)
            }
            .clip(ChamferShape(cut))
            .background(p.panel)
            .padding(start = padding, end = padding, top = (padding - 2.dp).coerceAtLeast(0.dp), bottom = padding)
    ) {
        Column(content = content)
    }
}

/** Panel header: [ID] TITLE_TEXT ............ meta, dashed underline with a scanning light. */
@Composable
fun PanelHeader(id: String, title: String, meta: String? = null, live: Boolean = false) {
    val p = Cyber.c
    val scan by rememberInfiniteTransition(label = "ph").animateFloat(
        -.3f, 1f, infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart), label = "scan"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .drawBehind {
                val y = size.height + 6.dp.toPx()
                drawLine(p.line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                val x = size.width * scan
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, p.accent, Color.Transparent), x, x + size.width * .3f),
                    Offset(x, y), Offset(x + size.width * .3f, y), 1.5.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(
            id, T.mono(10.sp, Color.Black, 0.sp),
            Modifier
                .drawBehind { drawRect(p.acc(.35f), Offset(-3f, -3f), Size(size.width + 6f, size.height + 6f)) }
                .background(p.accent)
                .padding(horizontal = 5.dp, vertical = 1.dp)
        )
        Spacer(Modifier.width(10.dp))
        Txt(title, T.head(13.sp), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (live) LiveDot()
        if (meta != null) {
            Spacer(Modifier.width(6.dp))
            Txt(meta, T.mono(10.sp, p.textMute))
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun LiveDot(color: Color = Cyber.c.accent) {
    val a by rememberInfiniteTransition(label = "dot").animateFloat(
        1f, .4f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a"
    )
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .width(7.dp).height(7.dp)
            .drawBehind {
                drawCircle(color.copy(alpha = .3f * a), radius = size.minDimension)
                drawCircle(color.copy(alpha = a), radius = size.minDimension / 2 * (0.6f + 0.4f * a))
            }
    )
}

enum class BtnStyle { SOLID, OUTLINE }

/** Chamfered terminal button. Solid = filled accent with black text (like `.on` buttons). */
@Composable
fun CyberButton(
    text: String,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.SOLID,
    enabled: Boolean = true,
    sound: Boolean = true,
    onClick: () -> Unit
) {
    val p = Cyber.c
    val fx = LocalFx.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val cut = 8.dp
    val cutPx = with(LocalDensity.current) { cut.toPx() }
    val solid = style == BtnStyle.SOLID && enabled
    Box(
        modifier
            .graphicsLayer { val s = if (pressed) .96f else 1f; scaleX = s; scaleY = s }
            .drawBehind {
                if (solid) {
                    // outer glow
                    for ((g, a) in listOf(10f to .12f, 5f to .25f)) {
                        drawPath(chamferPath(size, cutPx), p.acc(a), style = Stroke(g.dp.toPx()))
                    }
                }
            }
            .clip(ChamferShape(cut))
            .background(
                when {
                    !enabled -> p.k11
                    solid -> if (pressed) p.light(.3f) else p.accent
                    pressed -> p.acc(.25f)
                    else -> p.acc(.06f)
                }
            )
            .drawBehind { if (!solid) drawPath(chamferPath(size, cutPx), if (enabled) p.line2 else p.line, style = Stroke(1.dp.toPx())) }
            .clickable(src, null, enabled) {
                if (sound) fx.tap()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Txt(
            text,
            T.mono(13.sp, if (solid) Color.Black else if (enabled) p.accent else p.textMute, .16.sp)
        )
    }
}

/** Small bordered label/value box (like `.fng-hist div`). */
@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    val p = Cyber.c
    Column(
        modifier
            .background(Color.Black.copy(alpha = .25f))
            .drawBehind { drawRect(p.line, style = Stroke(1.dp.toPx())) }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Txt(label, T.mono(9.sp, p.textMute))
        Txt(value, T.mono(16.sp, valueColor, 0.sp, glow = p.acc(.5f)))
    }
}
