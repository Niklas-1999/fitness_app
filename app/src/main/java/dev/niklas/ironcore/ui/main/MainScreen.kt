package dev.niklas.ironcore.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.AppState
import dev.niklas.ironcore.TodayItem
import dev.niklas.ironcore.data.QuestResult
import dev.niklas.ironcore.domain.Rating
import dev.niklas.ironcore.ui.components.LevelUpInfo
import dev.niklas.ironcore.ui.components.LevelUpTerminal
import dev.niklas.ironcore.ui.quest.QuestRunner
import dev.niklas.ironcore.ui.quest.RewardOverlay
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.data.PlanSlotEntity
import dev.niklas.ironcore.domain.C25K
import dev.niklas.ironcore.domain.Exercises
import dev.niklas.ironcore.domain.Format
import dev.niklas.ironcore.domain.ItemKind
import dev.niklas.ironcore.domain.Leveling
import dev.niklas.ironcore.domain.Stat
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.CountUpText
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.NeonBar
import dev.niklas.ironcore.ui.components.OptionCard
import dev.niklas.ironcore.ui.components.PanelHeader
import dev.niklas.ironcore.ui.components.StatBox
import dev.niklas.ironcore.ui.components.StatRadar
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import dev.niklas.ironcore.ui.theme.ThemePickerOverlay

private val TABS = listOf("HUD", "QUESTS", "FUEL", "PROFILE")

/** Callbacks into the view model. */
class MainActions(
    val complete: (TodayItem, Rating, Float?, Int) -> Unit,
    val restCheckIn: () -> Unit,
    val trainAnyway: (Boolean) -> Unit,
    val reinitialize: () -> Unit
)

@Composable
fun MainScreen(s: AppState.Ready, result: QuestResult?, onResultShown: () -> Unit, actions: MainActions) {
    val fx = LocalFx.current
    var tab by remember { mutableIntStateOf(0) }
    var showTheme by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf<TodayItem?>(null) }
    var levelUp by remember { mutableStateOf<LevelUpInfo?>(null) }
    val pl = s.player
    val rank = Leveling.rank(pl.level)
    androidx.activity.compose.BackHandler(enabled = (tab != 0 || showTheme) && running == null) {
        if (showTheme) showTheme = false else { fx.glitch(); tab = 0 }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopBar(onTheme = { fx.glitch(); showTheme = true })
            Tape(
                listOf("LVL ${pl.level}", "RANK ${rank.name}", "STREAK ${pl.streak}", "WEEK ${s.week}" + if (s.deloadWeek) " // DELOAD" else " // DELOAD IN ${s.weeksToDeload} WK") +
                    Stat.entries.mapIndexed { i, st -> "${st.short} ${pl.stats[i]}" } +
                    listOf("FUEL ${s.nutrition.kcal} KCAL", "PROTEIN ${s.nutrition.proteinG} G")
            )
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HudTab(s, onExecute = { fx.glitch(); running = it }, actions = actions)
                    1 -> WeekTab(s)
                    2 -> ModuleOffline("FUEL", Modifier)
                    else -> ProfileTab(s, actions.reinitialize) { fx.glitch(); running = TodayItem("debug_run", ItemKind.C25K, null, 0) }
                }
            }
            BottomNav(tab) { tab = it }
        }
        AnimatedVisibility(showTheme, enter = fadeIn(), exit = fadeOut()) { ThemePickerOverlay { showTheme = false } }
        running?.let { item ->
            QuestRunner(item, s, onClose = { fx.glitch(); running = null }) { rating, load, minutes ->
                running = null
                if (item.key != "debug_run") actions.complete(item, rating, load, minutes)
            }
        }
        if (running == null && levelUp == null) result?.let { r ->
            RewardOverlay(r, if (r.stat == Stat.VIT && r.event == dev.niklas.ironcore.domain.ProgEvent.NONE && r.nextTarget == null && r.xp == Leveling.REST_XP) "RECOVERY LOGGED" else "QUEST CLEARED") {
                onResultShown()
                if (r.leveledUp) {
                    val to = r.levelTo
                    levelUp = LevelUpInfo(r.levelFrom, to, Stat.entries.map { it.short }, r.statsBefore, r.statsAfter,
                        Leveling.rank(to).name, Leveling.nextRank(to)?.name, Leveling.rankProgress(to))
                }
            }
        }
        levelUp?.let { LevelUpTerminal(it) { levelUp = null } }
    }
}

@Composable
private fun Scroll(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) { content(); Spacer(Modifier.height(8.dp)) }
}

/** Label + target for one plan slot. */
private fun describe(slot: PlanSlotEntity, s: AppState.Ready): Pair<String, String> = when (slot.kind) {
    ItemKind.C25K -> C25K.run(s.c25kIndex).let { "RUN / WALK // ${it.label}" to it.summary }
    ItemKind.WALK -> "BRISK WALK" to "${slot.minutes} MIN"
    ItemKind.LADDER -> s.states[slot.ladderId]?.let {
        Format.variationName(it) to Format.target(it, s.setup.progContext, s.deloadWeek)
    } ?: ("?" to "")
}

private fun describe(item: TodayItem, s: AppState.Ready): Pair<String, String> = when (item.kind) {
    ItemKind.C25K -> C25K.run(s.c25kIndex).let { "RUN / WALK // ${it.label}" to it.summary }
    ItemKind.WALK -> "BRISK WALK" to "${item.minutes} MIN"
    ItemKind.LADDER -> s.states[item.ladderId]?.let {
        Format.variationName(it) to Format.target(it, s.setup.progContext, s.deloadWeek)
    } ?: ("?" to "")
}

@Composable
private fun OperativePanel(s: AppState.Ready) {
    val p = Cyber.c
    val pl = s.player
    CyberPanel(Modifier.fillMaxWidth(), delayMs = 50) {
        PanelHeader("01", "OPERATIVE_STATUS", "LOCAL", live = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            HexBadge(s.setup.handle.take(1), Modifier.size(62.dp, 70.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Txt(s.setup.handle, T.head(18.sp, Color.White, glow = 10f, weight = 900))
                Txt("RANK // ${Leveling.rank(pl.level).name}", T.mono(11.sp, p.textDim))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Txt("LV ", T.mono(12.sp, p.textDim))
                    CountUpText(pl.level, T.head(26.sp, Color.White, glow = 18f, weight = 900, spacing = .02.em))
                    Spacer(Modifier.weight(1f))
                    Txt("STREAK ", T.mono(11.sp, p.textDim))
                    CountUpText(s.streak, T.head(18.sp, CyberPalette.AMBER, glow = 0f, weight = 700))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Txt("EXP", T.mono(10.sp, p.textMute))
            Spacer(Modifier.weight(1f))
            CountUpText(pl.xp, T.mono(11.sp, Color.White)) { "$it / ${Leveling.xpToNext(pl.level)}" }
        }
        Spacer(Modifier.height(5.dp))
        val fx = LocalFx.current
        NeonBar(pl.xp / Leveling.xpToNext(pl.level).toFloat(), height = 8.dp, segments = 10, onTick = { fx.tick() })
    }
}

@Composable
private fun HudTab(s: AppState.Ready, onExecute: (TodayItem) -> Unit, actions: MainActions) {
    val p = Cyber.c
    val fx = LocalFx.current
    Scroll {
        OperativePanel(s)
        val done = s.doneToday
        if (s.directive.isNotEmpty()) CyberPanel(Modifier.fillMaxWidth(), delayMs = 150) {
            val cleared = s.directive.count { it.key in done }
            PanelHeader("02", if (s.isTrainingDay) "DAILY_DIRECTIVE" else "BONUS_OPS",
                "$cleared/${s.directive.size} CLEARED" + if (s.deloadWeek) " // DELOAD" else "")
            Txt((s.directiveType?.label ?: "") + if (s.deloadWeek) " // LIGHTER WEEK: HALF THE SETS" else "",
                T.mono(10.sp, p.textDim), Modifier.padding(bottom = 4.dp))
            s.directive.forEach { item ->
                val (name, target) = describe(item, s)
                val log = done[item.key]
                QuestRow(
                    name, target,
                    if (log != null) "+${log.xp} XP · ${log.rating}" else "",
                    done = log != null
                ) { onExecute(item) }
            }
            Spacer(Modifier.height(6.dp))
            NeonBar(cleared / s.directive.size.toFloat(), height = 4.dp, color = CyberPalette.UP)
            Txt(
                if (cleared == s.directive.size) "> DIRECTIVE CLEARED. RECOVER WELL." else "> ONE QUEST KEEPS THE STREAK ALIVE. FIRST QUESTS OF THE DAY GIVE THE MOST XP.",
                T.mono(10.sp, if (cleared == s.directive.size) CyberPalette.UP else p.textMute, 0.sp), Modifier.padding(top = 8.dp)
            )
            if (!s.isTrainingDay && cleared == 0) {
                Spacer(Modifier.height(8.dp))
                CyberButton("CANCEL BONUS OPS", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { actions.trainAnyway(false) }
            }
        } else CyberPanel(Modifier.fillMaxWidth(), delayMs = 150) {
            PanelHeader("02", "REST_DAY", Format.DAY_LONG[s.todayDow - 1])
            Txt("No training scheduled today. Recovery is part of the protocol: muscles adapt between sessions.", T.body(15.sp, p.text))
            Spacer(Modifier.height(10.dp))
            val checked = s.day?.restCheckIn == true
            OptionCard("REST PROTOCOL", if (checked) "Recovery logged today ✓" else "Check in: +${Leveling.REST_XP} XP, VIT data, streak stays alive",
                checked, Modifier.padding(bottom = 8.dp)) { if (!checked) actions.restCheckIn() else fx.error() }
            OptionCard("TRAIN ANYWAY", "Extra session: only muscles not trained in the last 48 h", false) { actions.trainAnyway(true) }
        }
        CyberPanel(Modifier.fillMaxWidth(), delayMs = 200) {
            PanelHeader("AM", "WEEKLY_ACTIVITY", "WHO TARGET")
            Row {
                Txt("ACTIVE MINUTES // LAST 7 DAYS", T.mono(10.sp, p.textMute))
                Spacer(Modifier.weight(1f))
                Txt("${s.weekActiveMin} / 150", T.mono(11.sp, Color.White))
            }
            Spacer(Modifier.height(4.dp))
            NeonBar(s.weekActiveMin / 150f, height = 6.dp, segments = 5)
            Txt("Walks and runs count. WHO: 150–300 min of moderate activity per week.", T.mono(10.sp, p.textMute, 0.sp), Modifier.padding(top = 6.dp))
        }
        CyberPanel(Modifier.fillMaxWidth(), delayMs = 250) {
            PanelHeader("03", "STAT_MATRIX", "5 ATTRIBUTES")
            StatRadar(s.player.stats.map { it / 25f }, Stat.entries.map { it.short }, Modifier.fillMaxWidth().height(220.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Stat.entries.forEachIndexed { i, st -> StatBox(st.short, s.player.stats[i].toString(), Modifier.weight(1f)) }
            }
        }
        CyberPanel(Modifier.fillMaxWidth(), delayMs = 350) {
            val n = s.nutrition
            PanelHeader("04", "FUEL_GAUGE", "TODAY")
            FuelGauge(0, n.kcal)
            Spacer(Modifier.height(12.dp))
            MacroRow("PROTEIN", 0, n.proteinG, "g")
            MacroRow("CARBS", 0, n.carbsG, "g")
            MacroRow("FAT", 0, n.fatG, "g")
            MacroRow("WATER", 0, n.waterL, "L")
        }
    }
}

@Composable
private fun WeekTab(s: AppState.Ready) {
    val p = Cyber.c
    Scroll {
        RecentOps(s)
        CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader("WK", "WEEK_PROTOCOL", "WEEK ${s.week}" + if (s.deloadWeek) " // DELOAD" else "")
            for (d in 1..7) {
                val slots = s.slots[d]
                val isToday = d == s.todayDow
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Txt(Format.DAY_SHORT[d - 1], T.head(12.sp, if (isToday) Color.White else if (slots != null) p.accent else p.textMute,
                        glow = if (slots != null) 10f else 0f), Modifier.width(34.dp))
                    Column(Modifier.weight(1f)) {
                        if (slots == null) Txt("REST // RECOVERY PROTOCOL", T.mono(11.sp, p.textMute))
                        else {
                            Txt(slots.first().sessionType.label + if (isToday) "  ◄ TODAY" else "", T.mono(11.sp, Color.White))
                            slots.forEach { slot ->
                                val (name, target) = describe(slot, s)
                                Txt("· $name // $target", T.body(13.5.sp, p.text, FontWeight.Medium))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentOps(s: AppState.Ready) {
    val p = Cyber.c
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("LG", "OPS_LOG", "14 DAYS")
        if (s.log.isEmpty()) Txt("> NO OPERATIONS LOGGED YET.", T.mono(11.sp, p.textMute))
        s.log.take(25).forEach { q ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                val d = java.time.LocalDate.ofEpochDay(q.epochDay)
                Txt(Format.DAY_SHORT[d.dayOfWeek.value - 1] + " " + d.dayOfMonth.toString().padStart(2, '0'), T.mono(10.sp, p.textMute), Modifier.width(52.dp))
                Column(Modifier.weight(1f)) {
                    Txt(q.variationName, T.body(13.5.sp, Color.White, FontWeight.SemiBold))
                    val detail = when {
                        q.minutes > 0 && q.sets == 0 -> "${q.minutes} MIN"
                        else -> "${q.sets} × ${q.target}" + (q.loadKg?.let { " @ " + Format.kg(it) } ?: "")
                    }
                    Txt(detail + " · " + q.rating + if (q.deload) " · DELOAD" else "", T.mono(10.sp, p.textDim))
                }
                Txt("+${q.xp}", T.mono(12.sp, CyberPalette.UP))
            }
        }
    }
}

@Composable
private fun ProfileTab(s: AppState.Ready, onReinitialize: () -> Unit, onDebugRun: () -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    var sound by remember { mutableStateOf(fx.soundOn) }
    var haptics by remember { mutableStateOf(fx.hapticsOn) }
    var confirm by remember { mutableStateOf(false) }
    Scroll {
        OperativePanel(s)
        CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader("CFG", "SYSTEM_SETTINGS")
            OptionCard("SOUND EFFECTS", if (sound) "ON" else "OFF", sound, Modifier.padding(bottom = 8.dp)) { sound = !sound; fx.soundOn = sound }
            OptionCard("HAPTICS", if (haptics) "ON" else "OFF", haptics) { haptics = !haptics; fx.hapticsOn = haptics }
        }
        if (androidx.compose.ui.platform.LocalContext.current.packageName.endsWith(".debug")) CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader("DBG", "DEBUG_TOOLS", "DEV BUILD ONLY")
            CyberButton("TEST RUN TIMER (NOT SAVED)", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { onDebugRun() }
        }
        CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader("!!", "RE-INITIALIZE", "DESTRUCTIVE")
            Txt("Deletes your profile, plan and progress on this device and restarts the setup.", T.body(14.sp, p.textDim))
            Spacer(Modifier.height(10.dp))
            if (!confirm) CyberButton("RE-INITIALIZE SYSTEM", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { fx.error(); confirm = true }
            else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberButton("CANCEL", Modifier.weight(1f), BtnStyle.OUTLINE) { confirm = false }
                CyberButton("CONFIRM WIPE", Modifier.weight(1f)) { fx.glitch(); onReinitialize() }
            }
        }
    }
}
