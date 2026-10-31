package dev.niklas.ironcore.ui.quest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.AppState
import dev.niklas.ironcore.TodayItem
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.domain.Exercises
import dev.niklas.ironcore.domain.Format
import dev.niklas.ironcore.domain.ItemKind
import dev.niklas.ironcore.domain.Measure
import dev.niklas.ironcore.domain.Progression
import dev.niklas.ironcore.domain.Rating
import dev.niklas.ironcore.domain.Rewards
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.FieldLabel
import dev.niklas.ironcore.ui.components.GlitchText
import dev.niklas.ironcore.ui.components.NeonBar
import dev.niklas.ironcore.ui.components.NumberField
import dev.niklas.ironcore.ui.components.OptionCard
import dev.niklas.ironcore.ui.components.PanelHeader
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import kotlinx.coroutines.delay

/** Full-screen quest execution. Calls [onComplete] once with the rating (and load / minutes). */
@Composable
fun QuestRunner(item: TodayItem, s: AppState.Ready, onClose: () -> Unit, onComplete: (Rating, Float?, Int) -> Unit) {
    val p = Cyber.c
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    androidx.activity.compose.BackHandler { onClose() }
    Box(
        Modifier
            .fillMaxSize()
            .background(p.k01.copy(alpha = .985f))
            .clickable(remember { MutableInteractionSource() }, null) {}
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        when (item.kind) {
            ItemKind.LADDER -> LadderRunner(item, s, onClose, onComplete)
            ItemKind.WALK -> WalkLogger(item, onClose, onComplete)
            ItemKind.C25K -> RunTimer(s.c25kIndex, onClose) { r -> onComplete(r, null, dev.niklas.ironcore.domain.C25K.run(s.c25kIndex).let { (it.segments.sumOf { x -> x.first + x.second } + 600) / 60 }) }
        }
    }
}

@Composable
fun RunnerFrame(tag: String, title: String, meta: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = Cyber.c
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("// QUEST EXECUTION", T.mono(10.sp, p.textDim, .25.em), Modifier.weight(1f))
            CyberButton("✕ ABORT", style = BtnStyle.OUTLINE) { onClose() }
        }
        Spacer(Modifier.height(10.dp))
        CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader(tag, title, meta, live = true)
            content()
        }
    }
}

private enum class Phase { SETUP, WORK, REST, RATE }

@Composable
private fun LadderRunner(item: TodayItem, s: AppState.Ready, onClose: () -> Unit, onComplete: (Rating, Float?, Int) -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    val st = s.states.getValue(item.ladderId!!)
    val ladder = Exercises.ladder(st.ladderId)
    val v = ladder.steps[st.step]
    val sets = if (s.deloadWeek) Progression.deloadSets(st.sets) else st.sets
    val isLoad = v.measure == Measure.LOAD
    var load by remember { mutableStateOf(st.loadKg) }
    var phase by remember { mutableStateOf(if (isLoad) Phase.SETUP else Phase.WORK) }
    var set by remember { mutableIntStateOf(1) }
    val restSec = Rewards.restSeconds(v.measure, ladder.largeMuscle)
    val unit = when (v.measure) { Measure.HOLD -> "SEC"; else -> "REPS" } + if (v.perSide) " / SIDE" else ""

    RunnerFrame("Q", v.name.replace(' ', '_'), if (s.deloadWeek) "DELOAD WEEK" else "SET $set/$sets", onClose) {
        Txt(v.cue, T.body(15.sp, p.text, FontWeight.Medium))
        Spacer(Modifier.height(4.dp))
        Txt("TARGET // ${Format.target(st.copy(loadKg = load), s.setup.progContext, s.deloadWeek)}", T.mono(11.sp, p.textDim))
        Spacer(Modifier.height(10.dp))
        NeonBar((set - 1 + if (phase == Phase.RATE) 1 else 0) / sets.toFloat(), height = 6.dp, segments = sets, durationMs = 400)
        Spacer(Modifier.height(14.dp))
        when (phase) {
            Phase.SETUP -> {
                Txt(if (st.loadKg == null) "LOAD CALIBRATION" else "LOAD CHECK", T.head(14.sp))
                Txt(
                    if (st.loadKg == null) "First time: pick a weight you can lift for ${st.target} reps with about 2–3 reps left in the tank. Warm up with a lighter set first."
                    else "Planned load. Change it only if your equipment differs.",
                    T.body(14.sp, p.textDim), Modifier.padding(vertical = 6.dp)
                )
                FieldLabel(if (v.perSide || ladder.id.contains("db")) "WEIGHT (PER DUMBBELL)" else "WEIGHT")
                NumberField(load, { load = it }, "KG", 1f, 0f, 500f, decimals = 1)
                Spacer(Modifier.height(12.dp))
                CyberButton("LOCK IN LOAD >", Modifier.fillMaxWidth(), enabled = (load ?: 0f) > 0f) { phase = Phase.WORK }
            }
            Phase.WORK -> {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt("SET $set OF $sets", T.mono(12.sp, p.textDim, .3.em))
                    if (v.measure == Measure.HOLD) {
                        HoldTimer(st.target, key = set) { fx.complete(); if (set < sets) phase = Phase.REST else phase = Phase.RATE }
                    } else {
                        GlitchText(st.target.toString(), T.head(84.sp, Color.White, glow = 30f, weight = 900))
                        Txt(unit + (load?.let { " @ " + Format.kg(it) } ?: ""), T.mono(14.sp, p.accent, glow = p.acc(.5f)))
                        Spacer(Modifier.height(18.dp))
                        CyberButton("SET DONE", Modifier.fillMaxWidth()) {
                            fx.complete()
                            if (set < sets) phase = Phase.REST else phase = Phase.RATE
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    CyberButton("LOG ALL SETS DONE >>", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { set = sets; phase = Phase.RATE }
                }
            }
            Phase.REST -> RestTimer(restSec) { fx.streak(); set++; phase = Phase.WORK }
            Phase.RATE -> RatePanel { r -> onComplete(r, if (isLoad) load else null, 0) }
        }
    }
}

/** Effort rating = simplified reps in reserve (RESEARCH §4). */
@Composable
fun RatePanel(labels: List<Pair<String, String>>? = null, onRate: (Rating) -> Unit) {
    val p = Cyber.c
    var pick by remember { mutableStateOf<Rating?>(null) }
    val opts = labels ?: listOf(
        "EASY" to "4+ more reps were possible",
        "SOLID" to "2–3 reps left in the tank",
        "HARD" to "0–1 rep left, last reps were a grind",
        "FAILED" to "Could not reach the target"
    )
    Txt("RATE THE EFFORT", T.head(14.sp))
    Txt("This drives your next target. Be honest. The system adapts either way.", T.body(14.sp, p.textDim), Modifier.padding(bottom = 10.dp))
    Rating.entries.forEachIndexed { i, r ->
        OptionCard(opts[i].first, opts[i].second, pick == r, Modifier.padding(bottom = 8.dp)) { pick = r }
    }
    Spacer(Modifier.height(6.dp))
    CyberButton("UPLOAD RESULT >", Modifier.fillMaxWidth(), enabled = pick != null) { onRate(pick!!) }
}

/** Countdown ring between sets. */
@Composable
private fun RestTimer(seconds: Int, onDone: () -> Unit) {
    val p = Cyber.c
    var total by remember { mutableIntStateOf(seconds) }
    val start = remember { System.currentTimeMillis() }
    var left by remember { mutableLongStateOf(seconds * 1000L) }
    LaunchedEffect(total) {
        while (true) {
            left = total * 1000L - (System.currentTimeMillis() - start)
            if (left <= 0) { onDone(); break }
            delay(100)
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Txt("RECOVERY CYCLE", T.mono(12.sp, CyberPalette.UP, .3.em))
        Box(Modifier.size(200.dp).padding(8.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(p.k16, 0f, 360f, false, style = Stroke(10.dp.toPx()))
                drawArc(CyberPalette.UP, -90f, 360f * (left / (total * 1000f)).coerceIn(0f, 1f), false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Butt))
            }
            Txt("%d:%02d".format((left / 1000 + 1) / 60, (left / 1000 + 1) % 60), T.head(40.sp, Color.White, glow = 18f, weight = 900))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CyberButton("+15 S", Modifier.weight(1f), BtnStyle.OUTLINE) { total += 15 }
            CyberButton("SKIP >", Modifier.weight(1f)) { onDone() }
        }
    }
}

/** Hold countdown: START, counts down the target, auto-completes at 0. */
@Composable
private fun HoldTimer(target: Int, key: Int, onDone: () -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    var running by remember(key) { mutableStateOf(false) }
    var left by remember(key) { mutableLongStateOf(target * 1000L) }
    LaunchedEffect(running, key) {
        if (!running) return@LaunchedEffect
        val start = System.currentTimeMillis()
        var lastSec = target
        while (true) {
            left = target * 1000L - (System.currentTimeMillis() - start)
            val sec = (left / 1000).toInt()
            if (sec != lastSec && sec in 1..3) fx.tick()
            lastSec = sec
            if (left <= 0) { running = false; onDone(); break }
            delay(50)
        }
    }
    Box(Modifier.size(210.dp).padding(8.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(p.k16, 0f, 360f, false, style = Stroke(10.dp.toPx()))
            drawArc(p.accent, -90f, 360f * (left / (target * 1000f)).coerceIn(0f, 1f), false, style = Stroke(10.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("%.1f".format(left.coerceAtLeast(0) / 1000f), T.head(48.sp, Color.White, glow = 22f, weight = 900))
            Txt("SEC HOLD", T.mono(11.sp, p.textDim))
        }
    }
    Spacer(Modifier.height(10.dp))
    if (!running) CyberButton("START HOLD", Modifier.fillMaxWidth()) { fx.tap(); running = true }
    else CyberButton("DONE EARLY", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { running = false; onDone() }
}

@Composable
private fun WalkLogger(item: TodayItem, onClose: () -> Unit, onComplete: (Rating, Float?, Int) -> Unit) {
    val p = Cyber.c
    var min by remember { mutableStateOf<Float?>(item.minutes.toFloat()) }
    RunnerFrame("W", "BRISK_WALK", "${item.minutes} MIN", onClose) {
        Txt("Walk at a pace where you can talk but not sing. Counts toward the WHO target of 150 active minutes per week.", T.body(15.sp, p.text, FontWeight.Medium))
        FieldLabel("MINUTES WALKED", Modifier.padding(top = 12.dp))
        NumberField(min, { min = it }, "MIN", 5f, 0f, 300f)
        Spacer(Modifier.height(14.dp))
        CyberButton("UPLOAD RESULT >", Modifier.fillMaxWidth(), enabled = (min ?: 0f) >= 5f) {
            val m = min!!.toInt()
            onComplete(Rating.SOLID, null, m)
        }
    }
}
