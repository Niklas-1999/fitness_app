package dev.niklas.ironcore.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.AppState
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.data.CosmeticsEntity
import dev.niklas.ironcore.domain.AchCategory
import dev.niklas.ironcore.domain.Achievements
import dev.niklas.ironcore.domain.CosmeticType
import dev.niklas.ironcore.domain.Cosmetics
import dev.niklas.ironcore.domain.Leveling
import dev.niklas.ironcore.domain.Stat
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.ChipRow
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.GlitchText
import dev.niklas.ironcore.ui.components.NeonBar
import dev.niklas.ironcore.ui.components.OptionCard
import dev.niklas.ironcore.ui.components.PanelHeader
import dev.niklas.ironcore.ui.components.StatBox
import dev.niklas.ironcore.ui.components.StatRadar
import dev.niklas.ironcore.ui.components.TermInput
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActions(
    val saveCosmetics: (CosmeticsEntity) -> Unit,
    val setHandle: (String) -> Unit,
    val setImage: (android.net.Uri, Boolean) -> Unit,
    val clearImage: (Boolean) -> Unit,
    val reinitialize: () -> Unit,
    val debugRun: () -> Unit,
    val exportData: (android.net.Uri) -> Unit,
    val importData: (android.net.Uri) -> Unit,
    val dataMessage: String?
)

fun titleOf(id: String) = Cosmetics.get(id)?.name ?: "NEW BLOOD"

/** Banner + framed avatar + styled handle + title: the "ID card", also used on the HUD. */
@Composable
fun OperativeCard(s: AppState.Ready, bannerHeight: Int = 120) {
    val p = Cyber.c
    val c = s.cosmetics
    Column {
        Box {
            Banner(c.bannerPath, c.banner, Modifier.fillMaxWidth().height(bannerHeight.dp))
            Txt("ID // ${s.setup.handle.hashCode().toUInt().toString(16).uppercase().take(6)}", T.mono(9.sp, p.textDim), Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Row(Modifier.offset(y = (-36).dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.Bottom) {
            Avatar(c.avatarPath, c.frame, s.setup.handle.take(1), Modifier.size(84.dp, 94.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.padding(bottom = 2.dp)) {
                StyledName(s.setup.handle, c.nameFx, T.head(22.sp, Color.White, glow = 12f, weight = 900))
                Txt("« ${titleOf(c.title)} »", T.mono(11.sp, CyberPalette.AMBER, glow = CyberPalette.AMBER))
                Txt("LV ${s.player.level} // ${Leveling.rank(s.player.level).name}", T.mono(11.sp, p.textDim))
            }
        }
    }
}

@Composable
fun ProfileTab(s: AppState.Ready, a: ProfileActions) {
    val p = Cyber.c
    val fx = LocalFx.current
    var editing by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CyberPanel(Modifier.fillMaxWidth(), padding = 0.dp) {
            OperativeCard(s, 130)
            Column(Modifier.padding(horizontal = 14.dp).offset(y = (-24).dp)) {
                val pl = s.player
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatBox("TOTAL XP", pl.totalXp.toString(), Modifier.weight(1f))
                    StatBox("BEST STREAK", pl.bestStreak.toString(), Modifier.weight(1f), CyberPalette.AMBER)
                    StatBox("QUESTS", s.progress.quests.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatBox("CLEARS", s.progress.clears.toString(), Modifier.weight(1f))
                    StatBox("ACHIEVED", "${s.achievements.size}/${Achievements.ALL.size}", Modifier.weight(1f), CyberPalette.UP)
                    StatBox("FUEL LOGS", s.progress.foodLogs.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                CyberButton(if (editing) "CLOSE EDITOR" else "EDIT PROFILE // LOADOUT", Modifier.fillMaxWidth(), if (editing) BtnStyle.SOLID else BtnStyle.OUTLINE) {
                    fx.glitch(); editing = !editing
                }
            }
        }
        if (editing) ProfileEditor(s, a)
        CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader("03", "STAT_MATRIX", "5 ATTRIBUTES")
            StatRadar(s.player.stats.map { it / maxOf(25f, (s.player.stats.maxOrNull() ?: 0) + 5f) }, Stat.entries.map { it.short }, Modifier.fillMaxWidth().height(210.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Stat.entries.forEachIndexed { i, st -> StatBox(st.short, s.player.stats[i].toString(), Modifier.weight(1f)) }
            }
        }
        AchievementGrid(s)
        Settings(s, a)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileEditor(s: AppState.Ready, a: ProfileActions) {
    val p = Cyber.c
    val fx = LocalFx.current
    val c = s.cosmetics
    var handle by remember { mutableStateOf(s.setup.handle) }
    var target by remember { mutableStateOf(false) }   // false = avatar, true = banner
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) { fx.complete(); a.setImage(uri, target) } }
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("ED", "PROFILE_EDITOR")
        Txt("HANDLE", T.mono(10.sp, p.textMute), Modifier.padding(bottom = 6.dp))
        TermInput(handle, { handle = it.uppercase() }, maxLen = 16)
        Spacer(Modifier.height(6.dp))
        CyberButton("SAVE HANDLE", Modifier.fillMaxWidth(), BtnStyle.OUTLINE, enabled = handle.isNotBlank() && handle != s.setup.handle) { fx.complete(); a.setHandle(handle) }
        Spacer(Modifier.height(12.dp))
        Txt("IMAGES // STORED ONLY ON THIS DEVICE", T.mono(10.sp, p.textMute), Modifier.padding(bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberButton("AVATAR PHOTO", Modifier.weight(1f)) { target = false; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            CyberButton("BANNER PHOTO", Modifier.weight(1f)) { target = true; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        }
        if (c.avatarPath != null || c.bannerPath != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            CyberButton("REMOVE AVATAR", Modifier.weight(1f), BtnStyle.OUTLINE, enabled = c.avatarPath != null) { a.clearImage(false) }
            CyberButton("REMOVE BANNER", Modifier.weight(1f), BtnStyle.OUTLINE, enabled = c.bannerPath != null) { a.clearImage(true) }
        }
        Spacer(Modifier.height(14.dp))
        val unlocked = s.unlockedCosmetics
        CosmeticType.entries.forEach { type ->
            Txt(type.label + if (type == CosmeticType.BANNER && c.bannerPath != null) " // HIDDEN BY YOUR PHOTO" else "", T.head(12.sp), Modifier.padding(top = 8.dp, bottom = 6.dp))
            Cosmetics.ALL.filter { it.type == type }.forEach { cos ->
                val have = cos.id in unlocked
                val equipped = when (type) {
                    CosmeticType.TITLE -> c.title; CosmeticType.FRAME -> c.frame; CosmeticType.BANNER -> c.banner; CosmeticType.NAME_FX -> c.nameFx
                } == cos.id
                val source = Achievements.ALL.firstOrNull { it.reward == cos.id }
                Box(Modifier.graphicsLayer { alpha = if (have) 1f else .45f }) {
                    OptionCard(
                        (if (have) "" else "[LOCKED] ") + cos.name,
                        if (have) (if (equipped) "EQUIPPED" else "Tap to equip") else "Unlock: ${source?.name} // ${source?.desc}",
                        equipped, Modifier.padding(bottom = 6.dp)
                    ) {
                        if (!have) { fx.error(); return@OptionCard }
                        fx.glitch()
                        a.saveCosmetics(when (type) {
                            CosmeticType.TITLE -> c.copy(title = cos.id); CosmeticType.FRAME -> c.copy(frame = cos.id)
                            CosmeticType.BANNER -> c.copy(banner = cos.id); CosmeticType.NAME_FX -> c.copy(nameFx = cos.id)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementGrid(s: AppState.Ready) {
    val p = Cyber.c
    val fmt = remember { SimpleDateFormat("dd.MM.yy", Locale.GERMANY) }
    var cat by remember { mutableStateOf(0) }
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("AC", "ACHIEVEMENTS", "${s.achievements.size}/${Achievements.ALL.size} DECRYPTED")
        val cats = AchCategory.entries
        ChipRow(listOf("TRAIN", "STREAK", "PROG", "FUEL", "LEVEL"), { it == cat }, { cat = it })
        Spacer(Modifier.height(10.dp))
        Achievements.ALL.filter { it.category == cats[cat] }.forEach { ach ->
            val got = s.achievements[ach.id]
            val v = ach.value(s.progress).coerceAtMost(ach.goal)
            Column(
                Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    .background(if (got != null) CyberPalette.UP.copy(alpha = .07f) else Color.Transparent)
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(if (got != null) "◆" else "◇", T.head(14.sp, if (got != null) CyberPalette.UP else p.textMute, glow = 0f), Modifier.width(22.dp))
                    Column(Modifier.weight(1f)) {
                        Txt(ach.name, T.body(15.sp, if (got != null) Color.White else p.textDim, FontWeight.Bold))
                        Txt(ach.desc, T.body(13.sp, p.textDim, FontWeight.Medium))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Txt(if (got != null) fmt.format(Date(got.unlockedAt)) else "$v/${ach.goal}", T.mono(10.sp, if (got != null) CyberPalette.UP else p.textMute))
                        Txt(Cosmetics.get(ach.reward)?.let { it.type.label.substringBefore(' ') + ": " + it.name } ?: "", T.mono(9.sp, CyberPalette.AMBER))
                    }
                }
                if (got == null && ach.goal > 1) {
                    Spacer(Modifier.height(4.dp))
                    NeonBar(v / ach.goal.toFloat(), height = 3.dp)
                }
            }
        }
    }
}

@Composable
private fun Settings(s: AppState.Ready, a: ProfileActions) {
    val p = Cyber.c
    val fx = LocalFx.current
    var sound by remember { mutableStateOf(fx.soundOn) }
    var haptics by remember { mutableStateOf(fx.hapticsOn) }
    var confirm by remember { mutableStateOf(false) }
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("CFG", "SYSTEM_SETTINGS")
        OptionCard("SOUND EFFECTS", if (sound) "ON" else "OFF", sound, Modifier.padding(bottom = 8.dp)) { sound = !sound; fx.soundOn = sound }
        OptionCard("HAPTICS", if (haptics) "ON" else "OFF", haptics) { haptics = !haptics; fx.hapticsOn = haptics }
    }
    RemindersPanel()
    DataPanel(a)
    if (androidx.compose.ui.platform.LocalContext.current.packageName.endsWith(".debug")) CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("DBG", "DEBUG_TOOLS", "DEV BUILD ONLY")
        CyberButton("TEST RUN TIMER (NOT SAVED)", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { a.debugRun() }
        Spacer(Modifier.height(8.dp))
        val ctx = androidx.compose.ui.platform.LocalContext.current
        CyberButton("TEST NOTIFICATION", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) {
            dev.niklas.ironcore.notify.Reminders.notify(ctx, 99, "STREAK AT RISK // 12 DAYS", "Nothing logged today. One quest or a rest check-in before midnight keeps it alive.")
        }
    }
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("!!", "RE-INITIALIZE", "DESTRUCTIVE")
        Txt("Deletes your profile, plan, progress and achievements on this device and restarts the setup. Saved foods are kept.", T.body(14.sp, p.textDim))
        Spacer(Modifier.height(10.dp))
        if (!confirm) CyberButton("RE-INITIALIZE SYSTEM", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { fx.error(); confirm = true }
        else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberButton("CANCEL", Modifier.weight(1f), BtnStyle.OUTLINE) { confirm = false }
            CyberButton("CONFIRM WIPE", Modifier.weight(1f)) { fx.glitch(); a.reinitialize() }
        }
    }
}

@Composable
private fun RemindersPanel() {
    val p = Cyber.c
    val fx = LocalFx.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var cfg by remember { mutableStateOf(dev.niklas.ironcore.notify.Reminders.config(ctx)) }
    var pendingCfg by remember { mutableStateOf<dev.niklas.ironcore.notify.Reminders.Config?>(null) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        pendingCfg?.let { if (ok) { cfg = it; dev.niklas.ironcore.notify.Reminders.save(ctx, it) } else fx.error() }
        pendingCfg = null
    }
    fun update(c: dev.niklas.ironcore.notify.Reminders.Config) {
        val turningOn = (c.directive && !cfg.directive) || (c.streak && !cfg.streak)
        if (turningOn && !dev.niklas.ironcore.notify.Reminders.canNotify(ctx) && android.os.Build.VERSION.SDK_INT >= 33) {
            pendingCfg = c; perm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else { cfg = c; dev.niklas.ironcore.notify.Reminders.save(ctx, c) }
    }
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("RM", "REMINDERS", "LOCAL // NO PUSH SERVER")
        OptionCard("DIRECTIVE REMINDER", if (cfg.directive) "Training days at %02d:00, only if nothing is done yet".format(cfg.directiveHour) else "OFF", cfg.directive,
            Modifier.padding(bottom = 6.dp)) { update(cfg.copy(directive = !cfg.directive)) }
        if (cfg.directive) HourRow(cfg.directiveHour) { update(cfg.copy(directiveHour = it)) }
        OptionCard("STREAK GUARD", if (cfg.streak) "Every day at %02d:00 if your streak would break".format(cfg.streakHour) else "OFF", cfg.streak,
            Modifier.padding(top = 6.dp, bottom = 6.dp)) { update(cfg.copy(streak = !cfg.streak)) }
        if (cfg.streak) HourRow(cfg.streakHour) { update(cfg.copy(streakHour = it)) }
    }
}

@Composable
private fun HourRow(hour: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        CyberButton("−", Modifier.width(52.dp), BtnStyle.OUTLINE) { onChange((hour + 23) % 24) }
        Txt("%02d:00".format(hour), T.head(20.sp, Color.White, glow = 12f), Modifier.weight(1f))
        CyberButton("+", Modifier.width(52.dp), BtnStyle.OUTLINE) { onChange((hour + 1) % 24) }
    }
}

@Composable
private fun DataPanel(a: ProfileActions) {
    val p = Cyber.c
    val fx = LocalFx.current
    var confirmImport by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) { fx.complete(); a.exportData(uri) } }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { fx.glitch(); a.importData(uri) } }
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("DB", "DATA_VAULT", "OFFLINE BACKUP")
        Txt("Everything lives only on this phone. Export a backup file regularly (e.g. to your PC or a USB stick). Import it on a new phone to continue.",
            T.body(14.sp, p.textDim))
        Spacer(Modifier.height(10.dp))
        CyberButton("EXPORT BACKUP", Modifier.fillMaxWidth()) {
            export.launch("ironcore_backup_${java.time.LocalDate.now()}.json")
        }
        Spacer(Modifier.height(8.dp))
        if (!confirmImport) CyberButton("IMPORT BACKUP", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { fx.error(); confirmImport = true }
        else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberButton("CANCEL", Modifier.weight(1f), BtnStyle.OUTLINE) { confirmImport = false }
            CyberButton("OVERWRITE & IMPORT", Modifier.weight(1.4f)) { confirmImport = false; import.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
        }
        if (confirmImport) Txt("> IMPORT REPLACES ALL CURRENT DATA ON THIS PHONE.", T.mono(10.sp, CyberPalette.AMBER), Modifier.padding(top = 6.dp))
        a.dataMessage?.let { Txt("> $it", T.mono(11.sp, if (it.contains("FAILED")) CyberPalette.DOWN else CyberPalette.UP), Modifier.padding(top = 8.dp)) }
    }
}

/** "ACHIEVEMENT DECRYPTED" terminal overlay for all unseen unlocks. */
@Composable
fun AchievementOverlay(s: AppState.Ready, onDone: () -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    val list = s.unseenAchievements.mapNotNull { e -> Achievements.ALL.firstOrNull { it.id == e.id } }
    if (list.isEmpty()) return
    LaunchedEffect(list.size) { fx.levelUp() }
    Box(
        Modifier.fillMaxSize().background(p.k01.copy(alpha = .9f))
            .clickable(remember { MutableInteractionSource() }, null) { onDone() },
        contentAlignment = Alignment.Center
    ) {
        CyberPanel(Modifier.padding(18.dp).fillMaxWidth()) {
            PanelHeader("◆", "DECRYPTED", if (list.size > 1) "${list.size} NEW" else "NEW", live = true)
            list.take(6).forEach { ach ->
                val cos = Cosmetics.get(ach.reward)
                Column(Modifier.padding(vertical = 6.dp)) {
                    GlitchText(ach.name, T.head(20.sp, CyberPalette.UP, glow = 18f, weight = 900), periodMs = 1800)
                    Txt(ach.desc, T.body(14.sp, p.text, FontWeight.Medium))
                    if (cos != null) Txt("> UNLOCKED ${cos.type.label}: ${cos.name}", T.mono(11.sp, CyberPalette.AMBER, 0.sp, glow = CyberPalette.AMBER).copy(lineHeight = 1.4.em))
                }
            }
            if (list.size > 6) Txt("+ ${list.size - 6} MORE IN PROFILE", T.mono(11.sp, p.textDim))
            Spacer(Modifier.height(10.dp))
            Txt("Equip rewards in PROFILE → EDIT PROFILE. [ TAP TO ACKNOWLEDGE ]", T.mono(10.sp, p.textMute, 0.sp))
        }
    }
}
