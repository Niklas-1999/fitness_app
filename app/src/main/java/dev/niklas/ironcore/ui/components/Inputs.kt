package dev.niklas.ironcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import kotlinx.coroutines.delay

/** Selectable chamfered card: title + description, glowing when selected. */
@Composable
fun OptionCard(
    title: String,
    desc: String? = null,
    selected: Boolean,
    modifier: Modifier = Modifier,
    tag: String? = null,
    onClick: () -> Unit
) {
    val p = Cyber.c
    val fx = LocalFx.current
    val cut = 10.dp
    val cutPx = with(LocalDensity.current) { cut.toPx() }
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                if (selected) for ((w, a) in listOf(8f to .12f, 4f to .25f)) drawPath(chamferPath(size, cutPx), p.acc(a), style = Stroke(w.dp.toPx()))
            }
            .clip(ChamferShape(cut))
            .background(if (selected) Brush.horizontalGradient(listOf(p.acc(.28f), p.acc(.06f))) else Brush.horizontalGradient(listOf(p.k07.copy(alpha = .7f), p.k05.copy(alpha = .5f))))
            .drawBehind {
                drawPath(chamferPath(size, cutPx), if (selected) p.accent else p.line, style = Stroke((if (selected) 1.5f else 1f).dp.toPx()))
                if (selected) drawRect(p.accent, size = Size(3.dp.toPx(), size.height))
            }
            .clickable(remember { MutableInteractionSource() }, null) { fx.tap(); onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt(title, T.body(17.sp, if (selected) Color.White else p.text, FontWeight.Bold))
                if (desc != null) Txt(desc, T.body(13.5.sp, if (selected) p.text else p.textDim, FontWeight.Medium))
            }
            if (tag != null) Txt(tag, T.mono(10.sp, if (selected) p.accent else p.textMute))
            Spacer(Modifier.width(8.dp))
            // checkbox-ish indicator
            Box(
                Modifier
                    .size(14.dp)
                    .drawBehind {
                        drawRect(if (selected) p.accent else p.line2, style = Stroke(1.5.dp.toPx()))
                        if (selected) drawRect(p.accent, Offset(3.dp.toPx(), 3.dp.toPx()), Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()))
                    }
            )
        }
    }
}

/** Label above a field, mono style. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Txt(text, T.mono(10.sp, Cyber.c.textMute, .16.sp), modifier.padding(bottom = 6.dp, top = 4.dp))
}

/** Terminal text input. */
@Composable
fun TermInput(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", maxLen: Int = 20) {
    val p = Cyber.c
    Box(
        modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = .45f))
            .drawBehind {
                drawRect(p.line2, style = Stroke(1.dp.toPx()))
                drawRect(p.accent, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
            }
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty()) Txt(placeholder, T.mono(18.sp, p.textMute))
        BasicTextField(
            value, { onChange(it.take(maxLen)) },
            textStyle = T.mono(18.sp, Color.White, glow = p.acc(.5f)),
            singleLine = true,
            cursorBrush = SolidColor(p.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Numeric field with − / + steppers. Keeps its own text so partial input is possible. */
@Composable
fun NumberField(
    value: Float?,
    onChange: (Float?) -> Unit,
    unit: String,
    step: Float = 1f,
    min: Float = 0f,
    max: Float = 999f,
    decimals: Int = 0,
    modifier: Modifier = Modifier
) {
    val p = Cyber.c
    fun fmt(v: Float?) = when {
        v == null -> ""
        decimals == 0 -> v.toInt().toString()
        else -> "%.${decimals}f".format(java.util.Locale.US, v)
    }
    var text by remember { mutableStateOf(fmt(value)) }
    LaunchedEffect(value) { if (text.replace(',', '.').toFloatOrNull() != value) text = fmt(value) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CyberButton("−", Modifier.width(48.dp), BtnStyle.OUTLINE) {
            onChange(((value ?: min) - step).coerceIn(min, max))
        }
        Box(
            Modifier
                .weight(1f)
                .background(Color.Black.copy(alpha = .45f))
                .drawBehind { drawRect(p.line2, style = Stroke(1.dp.toPx())) }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                BasicTextField(
                    text,
                    { s ->
                        val clean = s.filter { it.isDigit() || ((it == '.' || it == ',') && decimals > 0) }.take(6)
                        text = clean
                        onChange(clean.replace(',', '.').toFloatOrNull()?.coerceAtMost(max))
                    },
                    textStyle = T.head(22.sp, Color.White, glow = 12f, weight = 700).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End),
                    singleLine = true,
                    cursorBrush = SolidColor(p.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = if (decimals > 0) KeyboardType.Decimal else KeyboardType.Number),
                    modifier = Modifier.width(110.dp)
                )
                Spacer(Modifier.width(8.dp))
                Txt(unit, T.mono(12.sp, p.textDim), Modifier.padding(bottom = 4.dp))
            }
        }
        CyberButton("+", Modifier.width(48.dp), BtnStyle.OUTLINE) {
            onChange(((value ?: min) + step).coerceIn(min, max))
        }
    }
}

/** Row of selectable chips (single or multi select handled by caller). */
@Composable
fun ChipRow(labels: List<String>, isOn: (Int) -> Boolean, onToggle: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = Cyber.c
    val fx = LocalFx.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        labels.forEachIndexed { i, l ->
            val on = isOn(i)
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .drawBehind {
                        val path = chamferPath(size, 7.dp.toPx())
                        if (on) { drawPath(path, p.acc(.2f), style = Stroke(6.dp.toPx())); drawPath(path, p.accent) }
                        else drawPath(path, p.line2, style = Stroke(1.dp.toPx()))
                    }
                    .clickable(remember { MutableInteractionSource() }, null) { fx.tap(); onToggle(i) },
                contentAlignment = Alignment.Center
            ) { Txt(l, T.mono(13.sp, if (on) Color.Black else p.textDim)) }
        }
    }
}

/** A yes/no question line. answer: null = unanswered. */
@Composable
fun YesNo(question: String, answer: Boolean?, onAnswer: (Boolean) -> Unit) {
    val p = Cyber.c
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Txt(question, T.body(15.sp, if (answer == null) Color.White else p.text, FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        ChipRow(listOf("NO", "YES"), { (it == 1) == answer && answer != null }, { onAnswer(it == 1) })
    }
}

/** Count-up stopwatch for hold tests; STOP writes the seconds. */
@Composable
fun Stopwatch(onResult: (Int) -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    var running by remember { mutableStateOf(false) }
    var start by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        while (running) { elapsed = System.currentTimeMillis() - start; delay(50) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(
            "%02d.%d s".format(elapsed / 1000, (elapsed % 1000) / 100),
            T.head(24.sp, if (running) CyberPalette.UP else Color.White, glow = 14f),
            Modifier.weight(1f)
        )
        CyberButton(if (running) "STOP" else "START TIMER", style = if (running) BtnStyle.SOLID else BtnStyle.OUTLINE) {
            if (running) {
                running = false
                fx.complete()
                onResult((elapsed / 1000).toInt())
            } else {
                start = System.currentTimeMillis(); elapsed = 0; running = true
            }
        }
    }
}
