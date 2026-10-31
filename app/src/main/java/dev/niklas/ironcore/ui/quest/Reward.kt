package dev.niklas.ironcore.ui.quest

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.data.QuestResult
import dev.niklas.ironcore.domain.ProgEvent
import dev.niklas.ironcore.ui.components.CountUpText
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.GlitchText
import dev.niklas.ironcore.ui.components.PanelHeader
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import kotlinx.coroutines.delay

/** "UPLOAD COMPLETE" card after a quest: XP, bonus, attribute point, next target, streak. Tap or auto-dismiss. */
@Composable
fun RewardOverlay(r: QuestResult, title: String = "QUEST CLEARED", onDone: () -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    val a = remember { Animatable(0f) }
    LaunchedEffect(r) {
        fx.complete()
        a.animateTo(1f, tween(350))
        if (r.streakUp) { delay(450); fx.streak() }
        if (r.bonusXp > 0) { delay(350); fx.levelUp() }
        delay(if (r.bonusXp > 0) 2600 else 2200)
        onDone()
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f * a.value))
            .clickable(remember { MutableInteractionSource() }, null) { onDone() },
        contentAlignment = Alignment.Center
    ) {
        CyberPanel(Modifier.padding(22.dp).fillMaxWidth().graphicsLayer { alpha = a.value; scaleX = .9f + .1f * a.value; scaleY = scaleX }) {
            PanelHeader("OK", "UPLOAD_COMPLETE")
            GlitchText(title, T.head(26.sp, CyberPalette.UP, glow = 20f, weight = 900))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Txt("+", T.head(28.sp, Color.White, glow = 20f, weight = 900))
                CountUpText(r.xp + r.bonusXp, T.head(40.sp, Color.White, glow = 22f, weight = 900, spacing = .02.em))
                Txt(" XP", T.head(20.sp, p.accent, glow = 14f), Modifier.padding(bottom = 6.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
                if (r.bonusXp > 0) Line("DIRECTIVE CLEAR", "+${r.bonusXp} XP", CyberPalette.AMBER)
                r.stat?.let { Line("${it.short} DATA", if (r.statRaised) "${it.short} +1" else "+1 POINT", if (r.statRaised) CyberPalette.UP else p.textDim) }
                if (r.streakUp) Line("STREAK", "${r.streak} DAY${if (r.streak == 1) "" else "S"} ▲", CyberPalette.AMBER)
                when (r.event) {
                    ProgEvent.UNLOCK_VARIATION -> Line("NEW VARIATION", "UNLOCKED", CyberPalette.UP)
                    ProgEvent.LOAD_UP -> Line("LOAD", "INCREASED", CyberPalette.UP)
                    ProgEvent.SET_ADDED -> Line("VOLUME", "+1 SET", CyberPalette.UP)
                    ProgEvent.BACKOFF, ProgEvent.STEP_DOWN -> Line("TARGET", "RECALIBRATED", CyberPalette.AMBER)
                    ProgEvent.REPEAT -> Line("TARGET", "HOLD & REPEAT", p.textDim)
                    else -> {}
                }
                r.nextTarget?.let { Txt("> NEXT: $it", T.mono(11.sp, p.textDim, 0.sp), Modifier.padding(top = 4.dp)) }
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String, color: Color) {
    val p = Cyber.c
    Row(Modifier.fillMaxWidth()) {
        Txt(label, T.mono(12.sp, p.textMute))
        Spacer(Modifier.weight(1f))
        Txt(value, T.mono(13.sp, color, glow = color))
    }
}
