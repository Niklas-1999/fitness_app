package dev.niklas.ironcore.ui.fuel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.AppState
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.domain.Adaptive
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.NeonBar
import dev.niklas.ironcore.ui.components.NumberField
import dev.niklas.ironcore.ui.components.PanelHeader
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.main.FuelGauge
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import kotlin.math.roundToInt

class FuelActions(
    val openFoodSheet: () -> Unit,
    val deleteLog: (Long) -> Unit,
    val addWater: (Int) -> Unit,
    val logWeight: (Float) -> Unit
)

@Composable
fun FuelTab(s: AppState.Ready, a: FuelActions) {
    val p = Cyber.c
    val n = s.nutrition
    val today = s.foodToday
    fun sum(f: (dev.niklas.ironcore.data.FoodLogEntity) -> Float) = today.sumOf { f(it).toDouble() }.toFloat()
    val kcal = sum { it.kcal }.roundToInt()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CyberPanel(Modifier.fillMaxWidth(), delayMs = 50) {
            PanelHeader("F1", "FUEL_GAUGE", "TODAY")
            FuelGauge(kcal, n.kcal)
            Spacer(Modifier.height(10.dp))
            MacroLine("PROTEIN", sum { it.protein }, n.proteinG.toFloat(), "g", CyberPalette.UP)
            MacroLine("CARBS", sum { it.carbs }, n.carbsG.toFloat(), "g")
            MacroLine("FAT", sum { it.fat }, n.fatG.toFloat(), "g")
            MacroLine("FIBRE", sum { it.fibre }, n.fibreG.toFloat(), "g")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Info("SUGARS", sum { it.sugar }, n.sugarRefG.toFloat(), Modifier.weight(1f))
                Info("SAT. FAT", sum { it.satFat }, 20f, Modifier.weight(1f))
                Info("SALT", sum { it.salt }, n.saltRefG.toFloat(), Modifier.weight(1f))
            }
            Txt("Sugars / saturates / salt: EU reference intakes (max), Reg. 1169/2011.", T.mono(9.sp, p.textMute, 0.sp), Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(12.dp))
            CyberButton("+ LOG FOOD", Modifier.fillMaxWidth()) { a.openFoodSheet() }
        }

        CyberPanel(Modifier.fillMaxWidth(), delayMs = 120) {
            val target = (n.waterL * 1000).toInt()
            PanelHeader("F2", "HYDRATION", "EFSA ${n.waterL} L")
            Row {
                Txt("WATER // TODAY", T.mono(10.sp, p.textMute))
                Spacer(Modifier.weight(1f))
                Txt("%.2f / %.1f L".format(s.waterTodayMl / 1000f, n.waterL), T.mono(11.sp, Color.White))
            }
            Spacer(Modifier.height(4.dp))
            NeonBar(s.waterTodayMl / target.toFloat(), height = 8.dp, segments = 8, color = CyberPalette.CYAN)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberButton("−250", Modifier.weight(1f), BtnStyle.OUTLINE) { a.addWater(-250) }
                CyberButton("+250 ML", Modifier.weight(1.4f)) { a.addWater(250) }
                CyberButton("+500 ML", Modifier.weight(1.4f)) { a.addWater(500) }
            }
            Txt("Total water incl. food water (EFSA adequate intake). Drinks cover ~70–80 %.", T.mono(9.sp, p.textMute, 0.sp), Modifier.padding(top = 6.dp))
        }

        CyberPanel(Modifier.fillMaxWidth(), delayMs = 180) {
            PanelHeader("F3", "INTAKE_LOG", "${today.size} ENTRIES")
            if (today.isEmpty()) Txt("> NO FUEL LOGGED TODAY.", T.mono(11.sp, p.textMute))
            today.forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Txt(e.name, T.body(14.5.sp, Color.White, FontWeight.SemiBold))
                        Txt("${e.grams.roundToInt()} g · P ${e.protein.roundToInt()} · C ${e.carbs.roundToInt()} · F ${e.fat.roundToInt()}", T.mono(10.sp, p.textDim))
                    }
                    Txt("${e.kcal.roundToInt()} KCAL", T.mono(12.sp, p.accent))
                    Spacer(Modifier.width(10.dp))
                    Txt("✕", T.head(16.sp, p.textMute, glow = 0f), Modifier.clickable { a.deleteLog(e.id) }.padding(6.dp))
                }
            }
        }

        WeightPanel(s, a)
        CalibrationPanel(s)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MacroLine(name: String, v: Float, t: Float, unit: String, color: Color = Cyber.c.accent) {
    val p = Cyber.c
    Column(Modifier.padding(vertical = 3.dp)) {
        Row {
            Txt(name, T.mono(10.sp, p.textMute))
            Spacer(Modifier.weight(1f))
            Txt("${v.roundToInt()} / ${t.roundToInt()} $unit", T.mono(11.sp, if (v >= t) color else Color.White))
        }
        Spacer(Modifier.height(3.dp))
        NeonBar(v / t, height = 5.dp, color = color)
    }
}

@Composable
private fun Info(name: String, v: Float, max: Float, modifier: Modifier) {
    val p = Cyber.c
    Column(modifier) {
        Txt(name, T.mono(9.sp, p.textMute))
        Txt("%.1f / %.0f g".format(v, max), T.mono(11.sp, if (v > max) CyberPalette.DOWN else p.text))
    }
}

@Composable
private fun WeightPanel(s: AppState.Ready, a: FuelActions) {
    val p = Cyber.c
    val fx = LocalFx.current
    val todayW = s.weights.lastOrNull { it.epochDay == s.todayEpoch }?.kg
    var input by remember { mutableStateOf(todayW ?: s.weights.lastOrNull()?.kg ?: s.setup.weightKg) }
    CyberPanel(Modifier.fillMaxWidth(), delayMs = 240) {
        PanelHeader("F4", "MASS_TRACKER", s.adaptive.trendKg?.let { "TREND %.1f KG".format(it) } ?: "")
        NumberField(input, { input = it ?: input }, "KG", .1f, 30f, 300f, decimals = 1)
        Spacer(Modifier.height(8.dp))
        CyberButton(if (todayW != null) "UPDATE TODAY'S WEIGH-IN" else "LOG WEIGH-IN", Modifier.fillMaxWidth()) { fx.complete(); a.logWeight(input) }
        Txt("Weigh in the morning, after the toilet, before eating. The trend line smooths out daily water swings.",
            T.mono(9.sp, p.textMute, 0.sp).copy(lineHeight = 1.4.em), Modifier.padding(top = 6.dp))
        if (s.weights.size >= 2) {
            val from = s.weights.first().epochDay
            val trend = Adaptive.trend(s.weights.associate { it.epochDay to it.kg }, from, s.todayEpoch)
            Canvas(Modifier.fillMaxWidth().height(120.dp).padding(top = 10.dp).background(Color.Black.copy(alpha = .25f))) {
                val all = s.weights.map { it.kg } + trend.values + listOfNotNull(s.setup.targetWeightKg)
                val lo = all.min() - .5f; val hi = all.max() + .5f
                val span = (s.todayEpoch - from).coerceAtLeast(1)
                fun x(d: Long) = size.width * (d - from) / span
                fun y(v: Float) = size.height * (1 - (v - lo) / (hi - lo))
                s.setup.targetWeightKg?.let { t ->
                    drawLine(CyberPalette.UP.copy(alpha = .5f), Offset(0f, y(t)), Offset(size.width, y(t)), 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                }
                s.weights.forEach { drawCircle(p.textDim, 2.5.dp.toPx(), Offset(x(it.epochDay), y(it.kg))) }
                val path = Path()
                trend.entries.forEachIndexed { i, (d, v) -> if (i == 0) path.moveTo(x(d), y(v)) else path.lineTo(x(d), y(v)) }
                drawPath(path, p.acc(.25f), style = Stroke(6.dp.toPx()))
                drawPath(path, p.accent, style = Stroke(2.dp.toPx()))
            }
        }
        s.adaptive.weeklyChangeKg?.let {
            Txt("MEASURED RATE: %+.2f KG / WEEK (TARGET %+.2f)".format(it, s.nutrition.weeklyChangeKg), T.mono(10.sp, p.textDim), Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun CalibrationPanel(s: AppState.Ready) {
    val p = Cyber.c
    val ad = s.adaptive
    val from = s.todayEpoch - Adaptive.WINDOW_DAYS + 1
    val logged = s.foodLog.map { it.epochDay }.distinct().count { it >= from }
    val weighins = s.weights.count { it.epochDay >= from }
    CyberPanel(Modifier.fillMaxWidth(), delayMs = 300) {
        PanelHeader("F5", "METABOLIC_CALIBRATION", if (ad.confidence > 0f) "${(ad.confidence * 100).roundToInt()} % DATA" else "COLLECTING")
        Txt("ENERGY TARGET: ${s.nutrition.kcal} KCAL · TDEE ${s.nutrition.tdee}", T.mono(11.sp, Color.White))
        if (ad.observedTdee != null) {
            Txt("MEASURED TDEE ${ad.observedTdee} (from your logs) blended with the formula estimate.", T.body(13.5.sp, p.textDim))
        } else {
            Txt("The formula is a starting estimate. Log food and weigh in regularly. After 14+ days with ≥${Adaptive.MIN_LOGGED_DAYS} logged days the system measures your real energy use.",
                T.body(13.5.sp, p.textDim))
            Spacer(Modifier.height(6.dp))
            Txt("LOGGED DAYS $logged/${Adaptive.MIN_LOGGED_DAYS} · WEIGH-INS $weighins/${Adaptive.MIN_WEIGHINS}", T.mono(10.sp, p.textMute))
            NeonBar(minOf(logged / Adaptive.MIN_LOGGED_DAYS.toFloat(), weighins / Adaptive.MIN_WEIGHINS.toFloat()), height = 4.dp)
        }
    }
}
