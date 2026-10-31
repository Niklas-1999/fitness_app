package dev.niklas.ironcore.ui.demo

import dev.niklas.ironcore.ui.components.Txt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.CountUpText
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.GlitchText
import dev.niklas.ironcore.ui.components.LevelUpInfo
import dev.niklas.ironcore.ui.components.LevelUpTerminal
import dev.niklas.ironcore.ui.components.NeonBar
import dev.niklas.ironcore.ui.components.PanelHeader
import dev.niklas.ironcore.ui.components.StatBox
import dev.niklas.ironcore.ui.components.StatRadar
import dev.niklas.ironcore.ui.components.chamferPath
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import dev.niklas.ironcore.ui.theme.ThemePickerOverlay
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ---------- mock data for the P0 visual demo ---------- */

private data class DemoQuest(val name: String, val target: String, val xp: Int, val stat: Int, val statName: String)

private val QUESTS = listOf(
    DemoQuest("KNEE PUSH-UP", "3 × 10", 40, 0, "STR"),
    DemoQuest("BODYWEIGHT SQUAT", "3 × 15", 40, 0, "STR"),
    DemoQuest("PLANK HOLD", "3 × 30 s", 30, 2, "AGI"),
    DemoQuest("RUN / WALK // C25K W1", "8 × 1:00", 60, 1, "END"),
)
private val STAT_NAMES = listOf("STR", "END", "AGI", "VIT", "WIL")

private val RANKS = listOf("GHOST" to 1, "RUNNER" to 10, "OPERATIVE" to 22, "ENFORCER" to 33, "VANGUARD" to 40, "APEX" to 46, "LEGEND" to 53)
private fun rankIndex(level: Int) = RANKS.indexOfLast { level >= it.second }

private fun xpToNext(level: Int) = maxOf(60, Math.round(84 * Math.pow(1.075, level.toDouble()) - 10).toInt())

@Composable
fun HudDemo() {
    val p = Cyber.c
    val fx = LocalFx.current
    var level by remember { mutableIntStateOf(7) }
    var xp by remember { mutableIntStateOf(120) }
    val stats = remember { mutableStateListOf(14, 9, 7, 11, 12) }
    val done = remember { mutableStateListOf(false, false, false, false) }
    var streak by remember { mutableIntStateOf(12) }
    var showTheme by remember { mutableStateOf(false) }
    var levelUp by remember { mutableStateOf<LevelUpInfo?>(null) }
    val statsAtLastLevel = remember { mutableStateListOf(12, 9, 6, 11, 11) }
    var tab by remember { mutableIntStateOf(0) }
    var popup by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var popupKey by remember { mutableIntStateOf(0) }

    fun complete(i: Int) {
        if (done[i]) return
        val q = QUESTS[i]
        done[i] = true
        fx.complete()
        stats[q.stat] = stats[q.stat] + 1
        var nx = xp + q.xp
        val allDone = done.all { it }
        if (allDone) { nx += 80; stats[4] = stats[4] + 1 }
        popup = (q.xp + if (allDone) 80 else 0) to "+1 ${q.statName}" + if (allDone) "  //  DIRECTIVE CLEAR" else ""
        popupKey++
        if (nx >= xpToNext(level)) {
            nx -= xpToNext(level)
            level++
            val r = rankIndex(level)
            val next = RANKS.getOrNull(r + 1)
            levelUp = LevelUpInfo(
                level - 1, level, STAT_NAMES, statsAtLastLevel.toList(), stats.toList(),
                RANKS[r].first, next?.first,
                if (next == null) 1f else (level - RANKS[r].second) / (next.second - RANKS[r].second).toFloat()
            )
            statsAtLastLevel.clear(); statsAtLastLevel.addAll(stats)
        }
        xp = nx
        if (i == 0 && done.count { it } == 1) { streak++; fx.streak() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            TopBar(onTheme = { fx.glitch(); showTheme = true })
            Tape(level, streak, stats)
            if (tab != 0) {
                ModuleOffline(listOf("HUD", "QUESTS", "FUEL", "PROFILE")[tab], Modifier.weight(1f))
            } else Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 01 operative status
                CyberPanel(Modifier.fillMaxWidth(), delayMs = 50) {
                    PanelHeader("01", "OPERATIVE_STATUS", "SYNCED", live = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HexBadge("N", Modifier.size(62.dp, 70.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Txt("NIKLAS", T.head(18.sp, Color.White, glow = 10f, weight = 900))
                            Txt("RANK // ${RANKS[rankIndex(level)].first}", T.mono(11.sp, p.textDim))
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Txt("LV ", T.mono(12.sp, p.textDim))
                                CountUpText(level, T.head(26.sp, Color.White, glow = 18f, weight = 900, spacing = .02.em))
                                Spacer(Modifier.weight(1f))
                                Txt("STREAK ", T.mono(11.sp, p.textDim))
                                CountUpText(streak, T.head(18.sp, CyberPalette.AMBER, glow = 0f, weight = 700))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box {
                        Column {
                            Row {
                                Txt("EXP", T.mono(10.sp, p.textMute))
                                Spacer(Modifier.weight(1f))
                                CountUpText(xp, T.mono(11.sp, Color.White)) { "$it / ${xpToNext(level)}" }
                            }
                            Spacer(Modifier.height(5.dp))
                            NeonBar(xp / xpToNext(level).toFloat(), height = 8.dp, segments = 10, onTick = { fx.tick() })
                        }
                        XpPopup(popup, popupKey)
                    }
                }

                // 02 daily directive
                CyberPanel(Modifier.fillMaxWidth(), delayMs = 150) {
                    PanelHeader("02", "DAILY_DIRECTIVE", "${done.count { it }}/${QUESTS.size} CLEARED")
                    QUESTS.forEachIndexed { i, q -> QuestRow(q, done[i]) { complete(i) } }
                    Spacer(Modifier.height(6.dp))
                    NeonBar(done.count { it } / QUESTS.size.toFloat(), height = 4.dp, color = CyberPalette.UP)
                    Spacer(Modifier.height(10.dp))
                    CyberButton("RESET DEMO QUESTS", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) {
                        for (i in done.indices) done[i] = false
                    }
                }

                // 03 stat matrix
                CyberPanel(Modifier.fillMaxWidth(), delayMs = 250) {
                    PanelHeader("03", "STAT_MATRIX", "5 ATTRIBUTES")
                    StatRadar(stats.map { it / 25f }, STAT_NAMES, Modifier.fillMaxWidth().height(230.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        STAT_NAMES.forEachIndexed { i, n ->
                            StatBox(n, stats[i].toString(), Modifier.weight(1f))
                        }
                    }
                }

                // 04 fuel gauge
                CyberPanel(Modifier.fillMaxWidth(), delayMs = 350) {
                    PanelHeader("04", "FUEL_GAUGE", "TODAY")
                    FuelGauge(1640, 2450)
                    Spacer(Modifier.height(12.dp))
                    MacroRow("PROTEIN", 96, 128, "g")
                    MacroRow("CARBS", 180, 290, "g")
                    MacroRow("FAT", 52, 82, "g")
                    MacroRow("WATER", 1.4f, 2.5f, "L")
                }
                Spacer(Modifier.height(8.dp))
            }
            BottomNav(tab) { tab = it }
        }

        AnimatedVisibility(showTheme, enter = fadeIn(), exit = fadeOut()) {
            ThemePickerOverlay { showTheme = false }
        }
        levelUp?.let { info -> LevelUpTerminal(info) { levelUp = null } }
    }
}

/* ---------- top bar ---------- */

@Composable
private fun TopBar(onTheme: () -> Unit) {
    val p = Cyber.c
    val now by produceState(Date()) { while (true) { value = Date(); delay(1000) } }
    val scan by rememberInfiniteTransition(label = "tb").animateFloat(.3f, 1f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "s")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .drawBehind {
                val cut = 18.dp.toPx()
                drawPath(chamferPath(size, cut), Brush.horizontalGradient(listOf(p.k16.copy(alpha = .85f), p.k05.copy(alpha = .7f), p.k16.copy(alpha = .85f))))
                drawPath(chamferPath(size, cut), p.line, style = Stroke(1.dp.toPx()))
                drawLine(Brush.horizontalGradient(listOf(Color.Transparent, p.acc(scan), Color.Transparent)),
                    Offset(0f, size.height), Offset(size.width, size.height), 1.5.dp.toPx())
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HexBadge("⚡", Modifier.size(38.dp, 42.dp), small = true)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            GlitchText("IRONCORE", T.head(16.sp, p.accent, glow = 18f, weight = 900, spacing = .08.em))
            Txt("NEURAL TRAINING TERMINAL", T.mono(8.sp, p.textDim, .18.em), maxLines = 1, softWrap = false)
        }
        Column(horizontalAlignment = Alignment.End) {
            Txt(SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(now), T.head(16.sp, Color.White, glow = 14f, spacing = .06.em))
            Txt(SimpleDateFormat("EEE dd MMM", Locale.ENGLISH).format(now).uppercase(), T.mono(9.sp, p.textDim))
        }
        Spacer(Modifier.width(10.dp))
        ThemeButton(onTheme)
    }
}

@Composable
private fun ThemeButton(onClick: () -> Unit) {
    val p = Cyber.c
    val rot by rememberInfiniteTransition(label = "tbn").animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "r")
    Canvas(
        Modifier
            .size(34.dp)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
    ) {
        drawRect(p.line2, style = Stroke(1.dp.toPx()))
        rotate(rot) {
            drawCircle(
                Brush.sweepGradient(listOf(p.accent, Color.Transparent, p.accent, Color.Transparent, p.accent)),
                size.minDimension / 2 - 3.dp.toPx(), style = Stroke(3.dp.toPx())
            )
        }
        drawCircle(p.acc(.4f), 9.dp.toPx())
        drawCircle(p.accent, 6.dp.toPx())
    }
}

/** Hexagon emblem like `.hex`, with a pulsing glow. */
@Composable
private fun HexBadge(letter: String, modifier: Modifier, small: Boolean = false) {
    val p = Cyber.c
    val pulse by rememberInfiniteTransition(label = "hex").animateFloat(0f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "p")
    val hex = remember {
        GenericShape { s, _ ->
            moveTo(s.width / 2, 0f); lineTo(s.width, s.height * .25f); lineTo(s.width, s.height * .75f)
            lineTo(s.width / 2, s.height); lineTo(0f, s.height * .75f); lineTo(0f, s.height * .25f); close()
        }
    }
    Box(modifier.drawBehind {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width / 2, 0f); lineTo(size.width, size.height * .25f); lineTo(size.width, size.height * .75f)
            lineTo(size.width / 2, size.height); lineTo(0f, size.height * .75f); lineTo(0f, size.height * .25f); close()
        }
        for ((w, a) in listOf(14f to .10f, 8f to .2f, 4f to .35f)) drawPath(path, p.acc(a * (.4f + .6f * pulse)), style = Stroke(w.dp.toPx()))
    }, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(hex).background(p.accent).padding(2.dp).clip(hex).background(p.k07), contentAlignment = Alignment.Center) {
            Txt(letter, T.head(if (small) 18.sp else 28.sp, p.accent, glow = 16f, weight = 900))
        }
    }
}

/** Scrolling ticker tape. */
@Composable
private fun Tape(level: Int, streak: Int, stats: List<Int>) {
    val p = Cyber.c
    val items = listOf("LVL $level", "STREAK $streak ▲", "NEXT DIRECTIVE 18:00") +
        STAT_NAMES.mapIndexed { i, n -> "$n ${stats[i]}" } + listOf("WEEK 3 // PHASE: BUILD", "DELOAD IN 3 WK")
    val text = items.joinToString("   ◆   ")
    val shift by rememberInfiniteTransition(label = "tape").animateFloat(0f, 1f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "x")
    var copyW by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(p.k11.copy(alpha = .55f))
            .drawBehind {
                drawLine(p.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                drawLine(p.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .clip(androidx.compose.ui.graphics.RectangleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .graphicsLayer { translationX = -shift * copyW }
        ) {
            repeat(3) { i ->
                Txt("$text   ◆   ", T.mono(12.sp, p.text), softWrap = false, maxLines = 1,
                    modifier = if (i == 0) Modifier.onSizeChanged { copyW = it.width } else Modifier)
            }
        }
    }
}

/* ---------- quests ---------- */

@Composable
private fun QuestRow(q: DemoQuest, done: Boolean, onExecute: () -> Unit) {
    val p = Cyber.c
    val stamp = remember { Animatable(if (done) 1f else 0f) }
    LaunchedEffect(done) {
        if (done) { stamp.snapTo(2.2f); stamp.animateTo(1f, spring(.45f, 380f)) } else stamp.snapTo(0f)
    }
    val col = if (done) CyberPalette.UP else p.accent
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Brush.horizontalGradient(listOf((if (done) CyberPalette.UP else p.accent).copy(alpha = .10f), Color.Transparent)))
            .drawBehind { drawRect(col, size = Size(2.dp.toPx(), size.height)) }
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt(q.name, T.body(16.sp, if (done) p.textMute else Color.White, androidx.compose.ui.text.font.FontWeight.Bold))
            Row {
                Txt(q.target, T.mono(13.sp, if (done) p.textMute else p.accent, glow = if (done) null else p.acc(.5f)))
                Spacer(Modifier.width(10.dp))
                Txt("+${q.xp} XP · ${q.statName}+1", T.mono(11.sp, p.textMute))
            }
        }
        Box(contentAlignment = Alignment.Center) {
            if (!done) {
                CyberButton("EXECUTE", sound = false) { onExecute() }
            } else {
                Txt(
                    "CLEARED",
                    T.head(12.sp, CyberPalette.UP, glow = 0f).copy(shadow = androidx.compose.ui.graphics.Shadow(CyberPalette.UP, blurRadius = 14f)),
                    Modifier
                        .graphicsLayer { scaleX = stamp.value; scaleY = stamp.value; rotationZ = -8f; alpha = (stamp.value).coerceAtMost(1f) }
                        .drawBehind { drawRect(CyberPalette.UP, style = Stroke(1.5.dp.toPx())) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/** "+40 XP" floating up from the XP bar. */
@Composable
private fun XpPopup(popup: Pair<Int, String>?, key: Int) {
    if (popup == null) return
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { a.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
    val p = Cyber.c
    Column(
        Modifier
            .fillMaxWidth()
            .offset(y = (-30 * a.value).dp)
            .graphicsLayer { alpha = if (a.value < .15f) a.value / .15f else 1f - ((a.value - .6f) / .4f).coerceIn(0f, 1f) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Txt("+${popup.first} XP", T.head(22.sp, Color.White, glow = 22f, weight = 900))
        Txt(popup.second, T.mono(11.sp, CyberPalette.UP, glow = CyberPalette.UP))
    }
}

/* ---------- placeholder modules ---------- */

@Composable
private fun ModuleOffline(name: String, modifier: Modifier) {
    val p = Cyber.c
    val phase = mapOf("QUESTS" to "P2", "FUEL" to "P3 / P4", "PROFILE" to "P5")[name]
    Box(modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.TopCenter) {
        CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader("--", "${name}_MODULE", "OFFLINE")
            Txt("> LOCATING MODULE ${name} ........ FAIL\n> STATUS: NOT YET COMPILED\n> SCHEDULED FOR BUILD PHASE $phase", T.mono(12.sp, p.accent, 0.sp, glow = p.acc(.5f)))
            Spacer(Modifier.height(12.dp))
            GlitchText("// AWAITING DEPLOYMENT", T.head(13.sp, p.textDim, glow = 0f))
        }
    }
}

/* ---------- fuel ---------- */

@Composable
private fun FuelGauge(kcal: Int, target: Int) {
    val p = Cyber.c
    val anim = remember { Animatable(0f) }
    LaunchedEffect(kcal) { anim.animateTo(kcal / target.toFloat(), tween(2000, easing = FastOutSlowInEasing)) }
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val r = size.height * .9f
            val c = Offset(size.width / 2, size.height - 4.dp.toPx())
            val tl = Offset(c.x - r, c.y - r)
            val s = Size(r * 2, r * 2)
            drawArc(p.k16, 180f, 180f, false, tl, s, style = Stroke(14.dp.toPx(), cap = StrokeCap.Butt))
            drawArc(
                Brush.sweepGradient(0f to p.accDim, .5f to p.accDim, .75f to p.accent, 1f to Color.White, center = c),
                180f, 180f * anim.value.coerceAtMost(1f), false, tl, s, style = Stroke(14.dp.toPx(), cap = StrokeCap.Butt)
            )
            for (i in 0..10) {
                rotate(-90f + i * 18f, c) {
                    drawLine(p.textMute, Offset(c.x, c.y - r - 12.dp.toPx()), Offset(c.x, c.y - r - (if (i % 5 == 0) 20 else 16).dp.toPx()), 1.5.dp.toPx())
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CountUpText(kcal, T.head(38.sp, Color.White, glow = 20f, weight = 900, spacing = .02.em))
            Txt("/ $target KCAL", T.mono(11.sp, p.textDim))
            Txt("${target - kcal} REMAINING", T.head(11.sp, p.accent, glow = 8f, spacing = .3.em))
        }
    }
}

@Composable
private fun MacroRow(name: String, v: Number, t: Number, unit: String) {
    val p = Cyber.c
    Column(Modifier.padding(vertical = 4.dp)) {
        Row {
            Txt(name, T.mono(10.sp, p.textMute))
            Spacer(Modifier.weight(1f))
            Txt("$v / $t $unit", T.mono(11.sp, Color.White))
        }
        Spacer(Modifier.height(3.dp))
        NeonBar(v.toFloat() / t.toFloat(), height = 5.dp)
    }
}

/* ---------- bottom nav ---------- */

@Composable
private fun BottomNav(sel: Int, onSelect: (Int) -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    val tabs = listOf("HUD", "QUESTS", "FUEL", "PROFILE")
    Row(
        Modifier
            .fillMaxWidth()
            .background(p.k04.copy(alpha = .92f))
            .drawBehind { drawLine(p.line2, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { i, t ->
            val on = i == sel
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .drawBehind {
                        val path = chamferPath(size, 8.dp.toPx())
                        if (on) {
                            drawPath(path, p.acc(.18f), style = Stroke(8.dp.toPx()))
                            drawPath(path, p.accent)
                        } else drawPath(path, p.line, style = Stroke(1.dp.toPx()))
                    }
                    .clickable(remember { MutableInteractionSource() }, null) { if (i != sel) { fx.glitch(); onSelect(i) } },
                contentAlignment = Alignment.Center
            ) {
                Txt(t, T.mono(12.sp, if (on) Color.Black else p.textDim, .14.em))
            }
        }
    }
}
