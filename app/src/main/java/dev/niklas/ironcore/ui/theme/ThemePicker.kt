package dev.niklas.ironcore.ui.theme

import dev.niklas.ironcore.ui.components.Txt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.PanelHeader

private fun hsv(h: Float, s: Float, v: Float) = Color.hsv(h.coerceIn(0f, 359.9f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))

private fun Color.toHsv(): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.rgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()), out)
    return out
}

/** Full-screen dimmed overlay with the theme configuration panel. */
@Composable
fun ThemePickerOverlay(onClose: () -> Unit) {
    val state = LocalThemeState.current ?: return
    val fx = LocalFx.current
    val p = Cyber.c
    val start = remember { p.accent.toHsv() }
    var hue by remember { mutableStateOf(start[0]) }
    var sat by remember { mutableStateOf(start[1]) }
    var value by remember { mutableStateOf(start[2]) }
    var hex by remember { mutableStateOf(p.accent.toHex()) }

    fun applyColor(c: Color, fromHsv: Boolean = false) {
        state.apply(c)
        hex = c.toHex()
        if (!fromHsv) { val h = c.toHsv(); hue = h[0]; sat = h[1]; value = h[2] }
    }
    fun applyHsv() = applyColor(hsv(hue, sat, value), fromHsv = true)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .6f))
            .clickable(remember { MutableInteractionSource() }, null) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        CyberPanel(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, null) {}
        ) {
            PanelHeader("CFG", "THEME_CONFIG", "ACCENT")
            Txt("The whole interface, background and effects recolor live from one accent color.", T.body(14.sp, p.textDim))
            Spacer(Modifier.height(12.dp))
            Txt("PRESETS", T.mono(10.sp, p.textMute))
            Spacer(Modifier.height(6.dp))
            THEME_PRESETS.chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (_, c) ->
                        val on = c.toHex() == p.accent.toHex()
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .drawBehind {
                                    drawRect(c.copy(alpha = .35f), Offset(-4f, -4f), size.copy(size.width + 8f, size.height + 8f))
                                    if (on) drawRect(Color.White, Offset(-5.dp.toPx(), -5.dp.toPx()),
                                        size.copy(size.width + 10.dp.toPx(), size.height + 10.dp.toPx()), style = Stroke(2.dp.toPx()))
                                }
                                .background(c)
                                .clickable { fx.glitch(); applyColor(c) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Txt("CUSTOM", T.mono(10.sp, p.textMute))
            Spacer(Modifier.height(6.dp))
            // saturation / brightness field
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .pointerInput(Unit) {
                        fun set(o: Offset) { sat = (o.x / size.width).coerceIn(0f, 1f); value = 1f - (o.y / size.height).coerceIn(0f, 1f); applyHsv() }
                        detectTapGestures { set(it) }
                    }
                    .pointerInput(Unit) {
                        fun set(o: Offset) { sat = (o.x / size.width).coerceIn(0f, 1f); value = 1f - (o.y / size.height).coerceIn(0f, 1f); applyHsv() }
                        detectDragGestures(onDragStart = { set(it) }) { ch, _ -> set(ch.position) }
                    }
            ) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, hsv(hue, 1f, 1f))))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val c = Offset(sat * size.width, (1 - value) * size.height)
                drawCircle(Color.White, 8.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                drawCircle(Color.Black, 10.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            }
            Spacer(Modifier.height(8.dp))
            // hue strip
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { hue = (it.x / size.width * 360f).coerceIn(0f, 359.9f); applyHsv() }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(onDragStart = { hue = (it.x / size.width * 360f).coerceIn(0f, 359.9f); applyHsv() }) { ch, _ ->
                            hue = (ch.position.x / size.width * 360f).coerceIn(0f, 359.9f); applyHsv()
                        }
                    }
            ) {
                drawRect(Brush.horizontalGradient((0..6).map { hsv(it * 60f, 1f, 1f) }))
                val x = hue / 360f * size.width
                drawRect(Color.White, Offset(x - 2.dp.toPx(), -3.dp.toPx()), size.copy(4.dp.toPx(), size.height + 6.dp.toPx()))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(
                    hex,
                    onValueChange = { s ->
                        hex = s.uppercase().take(7)
                        parseHex(hex)?.let { applyColor(it) }
                    },
                    textStyle = T.mono(14.sp, Color.White),
                    singleLine = true,
                    cursorBrush = SolidColor(p.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .width(110.dp)
                        .background(Color.Black.copy(alpha = .4f))
                        .drawBehind { drawRect(p.line2, style = Stroke(1.dp.toPx())) }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                )
                CyberButton("RESET", Modifier.weight(1f), BtnStyle.OUTLINE) { applyColor(DEFAULT_ACCENT) }
                CyberButton("DONE", Modifier.weight(1f)) { onClose() }
            }
        }
    }
}
