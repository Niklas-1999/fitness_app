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

@Composable
fun MainScreen(s: AppState.Ready, onReinitialize: () -> Unit) {
    val fx = LocalFx.current
    var tab by remember { mutableIntStateOf(0) }
    var showTheme by remember { mutableStateOf(false) }
    val pl = s.player
    val rank = Leveling.rank(pl.level)
    androidx.activity.compose.BackHandler(enabled = tab != 0 || showTheme) {
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
                    0 -> HudTab(s)
                    1 -> WeekTab(s)
                    2 -> ModuleOffline("FUEL", Modifier)
                    else -> ProfileTab(s, onReinitialize)
                }
            }
            BottomNav(tab) { tab = it }
        }
        AnimatedVisibility(showTheme, enter = fadeIn(), exit = fadeOut()) { ThemePickerOverlay { showTheme = false } }
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
                    CountUpText(pl.streak, T.head(18.sp, CyberPalette.AMBER, glow = 0f, weight = 700))
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
private fun HudTab(s: AppState.Ready) {
    val p = Cyber.c
    Scroll {
        OperativePanel(s)
        val today = s.slots[s.todayDow]
        CyberPanel(Modifier.fillMaxWidth(), delayMs = 150) {
            if (today != null) {
                PanelHeader("02", "DAILY_DIRECTIVE", today.first().sessionType.label + if (s.deloadWeek) " // DELOAD" else "")
                today.forEach { slot ->
                    val (name, target) = describe(slot, s)
                    val cue = slot.ladderId?.let { id -> s.states[id]?.let { Exercises.ladder(id).steps[it.step].cue } } ?: ""
                    QuestRow(name, target, cue, done = false, action = null)
                }
                Txt("> QUEST ENGINE (execute, rate, XP) ARRIVES IN THE NEXT BUILD.", T.mono(10.sp, p.textMute), Modifier.padding(top = 8.dp))
            } else {
                PanelHeader("02", "REST_DAY", Format.DAY_LONG[s.todayDow - 1])
                Txt("No training scheduled today. Recovery is part of the protocol.", T.body(15.sp, p.text))
                Spacer(Modifier.height(10.dp))
                OptionCard("REST PROTOCOL", "Check in, +${Leveling.REST_XP} XP, streak stays alive", false, Modifier.padding(bottom = 8.dp)) {}
                OptionCard("TRAIN ANYWAY", "Extra session that respects muscle recovery", false) {}
                Txt("> REST-DAY ACTIONS ARRIVE WITH THE QUEST ENGINE (NEXT BUILD).", T.mono(10.sp, p.textMute), Modifier.padding(top = 8.dp))
            }
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
private fun ProfileTab(s: AppState.Ready, onReinitialize: () -> Unit) {
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
