package dev.niklas.ironcore.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
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
import dev.niklas.ironcore.domain.C25K
import dev.niklas.ironcore.domain.Forecast
import dev.niklas.ironcore.domain.Format
import dev.niklas.ironcore.domain.ItemKind
import dev.niklas.ironcore.domain.Leveling
import dev.niklas.ironcore.domain.Nutrition
import dev.niklas.ironcore.domain.Plan
import dev.niklas.ironcore.domain.PlanGenerator
import dev.niklas.ironcore.domain.Setup
import dev.niklas.ironcore.domain.Stat
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.GlitchText
import dev.niklas.ironcore.ui.components.PanelHeader
import dev.niklas.ironcore.ui.components.StatBox
import dev.niklas.ironcore.ui.components.StatRadar
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T

@Composable
fun ReportScreen(setup: Setup, plan: Plan, onBack: () -> Unit, onEnter: () -> Unit) {
    val p = Cyber.c
    val stats = remember { PlanGenerator.initialStats(setup) }
    val nut = remember {
        Nutrition.targets(setup.sex, setup.age, setup.weightKg, setup.heightCm, setup.activity, setup.goal, setup.intensity, setup.targetWeightKg, setup.pregnant)
    }
    val ctx = setup.progContext
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Txt("// PROTOCOL COMPILED", T.mono(10.sp, p.textDim, .25.em))
            GlitchText("STATUS REPORT", T.head(28.sp, p.accent, glow = 20f, weight = 900, spacing = .06.em))
        }

        if (setup.conservative) CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader("!!", "MEDICAL_NOTICE", "CONSERVATIVE MODE")
            Txt(
                "Your health check flagged at least one item. Please get clearance from a doctor before training harder. " +
                    "The protocol starts with 1 set per exercise and easier targets. IRONCORE does not give medical advice.",
                T.body(14.sp, CyberPalette.AMBER)
            )
        }

        // 01 operative
        CyberPanel(Modifier.fillMaxWidth(), delayMs = 50) {
            PanelHeader("01", "OPERATIVE_PROFILE", "LV 1 // ${Leveling.rank(1).name}")
            Txt(setup.handle, T.head(22.sp, Color.White, glow = 12f, weight = 900))
            Txt("${setup.age} Y · ${setup.heightCm.toInt()} CM · ${setup.weightKg} KG · ${setup.experience.label} · ${setup.goal.label}", T.mono(10.sp, p.textDim))
            StatRadar(stats.map { it / 25f }, Stat.entries.map { it.short }, Modifier.fillMaxWidth().height(210.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Stat.entries.forEachIndexed { i, s -> StatBox(s.short, stats[i].toString(), Modifier.weight(1f)) }
            }
            Txt("+${Leveling.INIT_BONUS} XP INITIALIZATION BONUS ON ENTRY", T.mono(10.sp, CyberPalette.UP), Modifier.padding(top = 8.dp))
        }

        // 02 week protocol
        CyberPanel(Modifier.fillMaxWidth(), delayMs = 150) {
            PanelHeader("02", "WEEK_PROTOCOL", "${plan.sessions.size} SESSIONS")
            val states = plan.states.associateBy { it.ladderId }
            for (d in 1..7) {
                val s = plan.sessions[d]
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Txt(Format.DAY_SHORT[d - 1], T.head(12.sp, if (s != null) p.accent else p.textMute, glow = if (s != null) 10f else 0f), Modifier.width(34.dp))
                    Column(Modifier.weight(1f)) {
                        if (s == null) Txt("REST // RECOVERY PROTOCOL", T.mono(11.sp, p.textMute))
                        else {
                            Txt(s.type.label, T.mono(11.sp, Color.White))
                            s.items.forEach { item ->
                                val line = when (item.kind) {
                                    ItemKind.C25K -> "RUN/WALK // ${C25K.run(0).label} · ${C25K.run(0).summary}"
                                    ItemKind.WALK -> "BRISK WALK // ${item.minutes} MIN"
                                    ItemKind.LADDER -> states[item.ladderId]?.let { "${Format.variationName(it)} // ${Format.target(it, ctx)}" } ?: ""
                                }
                                Txt("· $line", T.body(13.5.sp, p.text, FontWeight.Medium))
                            }
                        }
                    }
                }
            }
            Txt("Warm up 5 min before each session (easy cardio + dynamic moves). Weekly side quest: ${plan.weeklyWalkGoalMin} active minutes (WHO).",
                T.mono(10.sp, p.textDim, 0.sp).copy(lineHeight = 1.5.em), Modifier.padding(top = 6.dp))
        }

        // 03 fuel
        CyberPanel(Modifier.fillMaxWidth(), delayMs = 250) {
            PanelHeader("03", "FUEL_TARGETS", "DAILY")
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(nut.kcal.toString(), T.head(36.sp, Color.White, glow = 18f, weight = 900, spacing = .02.em))
                Txt("  KCAL / DAY", T.mono(11.sp, p.textDim), Modifier.padding(bottom = 6.dp))
            }
            Txt("BMR ${nut.bmr} · TDEE ${nut.tdee} · " + when {
                nut.weeklyChangeKg < 0 -> "DEFICIT → %.2f KG/WK".format(nut.weeklyChangeKg)
                nut.weeklyChangeKg > 0 -> "SURPLUS → +%.2f KG/WK".format(nut.weeklyChangeKg)
                else -> "MAINTENANCE"
            }, T.mono(10.sp, p.textDim))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatBox("PROTEIN", "${nut.proteinG}g", Modifier.weight(1f))
                StatBox("CARBS", "${nut.carbsG}g", Modifier.weight(1f))
                StatBox("FAT", "${nut.fatG}g", Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatBox("FIBRE", "${nut.fibreG}g", Modifier.weight(1f))
                StatBox("WATER", "${nut.waterL}L", Modifier.weight(1f))
                StatBox("SALT MAX", "${nut.saltRefG}g", Modifier.weight(1f))
            }
            if (nut.note.isNotEmpty()) Txt("> ${nut.note}", T.mono(10.sp, CyberPalette.AMBER), Modifier.padding(top = 6.dp))
            Txt("Starting estimate (Mifflin-St Jeor × EFSA activity level). Re-calibrates from your weight and food logs.",
                T.mono(10.sp, p.textMute, 0.sp).copy(lineHeight = 1.5.em), Modifier.padding(top = 6.dp))
        }

        // 04 forecast
        CyberPanel(Modifier.fillMaxWidth(), delayMs = 350) {
            PanelHeader("04", "FORECAST", "PROJECTION")
            if (setup.targetWeightKg != null && nut.weeklyChangeKg != 0f) {
                val lose = nut.weeklyChangeKg < 0
                val pct = if (lose) Nutrition.lossRatePct(setup.intensity) else Nutrition.gainRatePct(setup.intensity)
                val wf = remember { Forecast.weight(setup.weightKg, setup.targetWeightKg, pct, lose) }
                Txt("BODY WEIGHT ${setup.weightKg} → ${setup.targetWeightKg} KG", T.mono(11.sp, Color.White))
                Txt(wf.etaWeeks?.let { "ETA ≈ $it WEEKS AT ${setup.intensity.label} PACE" } ?: "BEYOND 3 YEARS", T.head(14.sp, p.accent))
                WeightChart(wf.points.map { it.kg }, setup.targetWeightKg)
                Spacer(Modifier.height(10.dp))
            }
            val runDays = plan.sessions.values.count { s -> s.items.any { it.kind == ItemKind.C25K } }
            if (runDays > 0) {
                Txt("RUN 30 MIN NON-STOP (≈5 KM PACE): ~WEEK ${Forecast.c25kWeeks(runDays)}", T.mono(12.sp, Color.White), Modifier.padding(bottom = 6.dp))
            }
            val unlocks = remember { Forecast.unlocks(plan, ctx).distinctBy { it.variation.id }.take(6) }
            if (unlocks.isNotEmpty()) {
                Txt("VARIATION UNLOCKS", T.mono(10.sp, p.textMute, .16.em), Modifier.padding(bottom = 4.dp))
                unlocks.forEach { u ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Txt("WK ${u.week.toString().padStart(2)}", T.mono(12.sp, p.accent), Modifier.width(56.dp))
                        Txt(u.variation.name, T.body(14.sp, Color.White, FontWeight.SemiBold))
                    }
                }
            }
            if (plan.states.any { dev.niklas.ironcore.domain.Exercises.ladder(it.ladderId).isLoad }) {
                Txt("Loaded lifts: your first session calibrates the weight, then +2.5–5 % steps (ACSM).", T.mono(10.sp, p.textDim, 0.sp), Modifier.padding(top = 6.dp))
            }
            Txt("Projection assumes ~2 of 3 sessions feel SOLID and a lighter week every 6th week. Your real logs drive the actual progression.",
                T.mono(10.sp, p.textMute, 0.sp).copy(lineHeight = 1.5.em), Modifier.padding(top = 8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CyberButton("< EDIT", Modifier.weight(1f), BtnStyle.OUTLINE) { onBack() }
            CyberButton("ENTER SYSTEM >", Modifier.weight(2f)) { onEnter() }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun WeightChart(points: List<Float>, target: Float) {
    val p = Cyber.c
    Canvas(Modifier.fillMaxWidth().height(110.dp).padding(top = 8.dp).background(Color.Black.copy(alpha = .25f))) {
        if (points.size < 2) return@Canvas
        val lo = minOf(points.min(), target) - 1f
        val hi = maxOf(points.max(), target) + 1f
        fun x(i: Int) = size.width * i / (points.size - 1)
        fun y(v: Float) = size.height * (1 - (v - lo) / (hi - lo))
        for (k in 1..3) drawLine(p.line, Offset(0f, size.height * k / 4), Offset(size.width, size.height * k / 4), 1f)
        drawLine(CyberPalette.UP.copy(alpha = .6f), Offset(0f, y(target)), Offset(size.width, y(target)), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        val path = Path().apply { points.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) } }
        drawPath(path, p.acc(.25f), style = Stroke(6.dp.toPx()))
        drawPath(path, p.accent, style = Stroke(2.dp.toPx()))
        drawCircle(Color.White, 3.dp.toPx(), Offset(x(points.lastIndex), y(points.last())))
    }
}
