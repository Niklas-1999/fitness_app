package dev.niklas.ironcore.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.R

/** Orbitron is a variable font; one file, three weights. */
@OptIn(ExperimentalTextApi::class)
private fun orbitron(w: Int) = Font(
    R.font.orbitron, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w))
)

val Orbitron = FontFamily(orbitron(500), orbitron(700), orbitron(900))
val Rajdhani = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold)
)
val ShareTechMono = FontFamily(Font(R.font.share_tech_mono, FontWeight.Normal))

/** Text styles mirroring the dashboard's three families. */
object T {
    /** Orbitron heading with neon glow. */
    @Composable
    fun head(size: TextUnit = 13.sp, color: Color = Cyber.c.accent, glow: Float = 14f, weight: Int = 700, spacing: TextUnit = .18.em) =
        TextStyle(
            fontFamily = Orbitron, fontWeight = FontWeight(weight), fontSize = size, letterSpacing = spacing,
            color = color, shadow = if (glow > 0) Shadow(Cyber.c.acc(.7f), blurRadius = glow) else null
        )

    /** Share Tech Mono label / number. */
    @Composable
    fun mono(size: TextUnit = 11.sp, color: Color = Cyber.c.textDim, spacing: TextUnit = .14.em, glow: Color? = null) =
        TextStyle(
            fontFamily = ShareTechMono, fontSize = size, letterSpacing = spacing, color = color,
            shadow = glow?.let { Shadow(it, blurRadius = 12f) }
        )

    /** Rajdhani body. */
    @Composable
    fun body(size: TextUnit = 15.sp, color: Color = Cyber.c.text, weight: FontWeight = FontWeight.SemiBold) =
        TextStyle(fontFamily = Rajdhani, fontWeight = weight, fontSize = size, color = color)
}
