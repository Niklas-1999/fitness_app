package dev.niklas.ironcore.domain

import kotlin.math.pow
import kotlin.math.roundToInt

/** Level curve, XP rules and ranks (SPEC §6, simulated in tools/level_curve.py). */
object Leveling {
    /** XP needed to go from [level] to level+1. No max level. */
    fun xpToNext(level: Int): Int = maxOf(60, (84 * 1.075.pow(level) - 10).roundToInt())

    /** Multiplier for the k-th (0-based) quest completed today: discourages overtraining. */
    private val DAILY = floatArrayOf(1f, 1f, 1f, .9f, .8f, .6f, .3f, .15f)
    fun dailyMultiplier(k: Int): Float = if (k < DAILY.size) DAILY[k] else .05f

    fun streakMultiplier(streak: Int): Float = 1f + minOf(streak * .02f, .30f)

    const val DIRECTIVE_CLEAR_BONUS = 40
    const val REST_XP = 20
    const val INIT_BONUS = 50

    data class Rank(val name: String, val minLevel: Int)

    val RANKS = listOf(
        Rank("GHOST", 1), Rank("RUNNER", 10), Rank("OPERATIVE", 22), Rank("ENFORCER", 33),
        Rank("VANGUARD", 40), Rank("APEX", 46), Rank("LEGEND", 53)
    )

    fun rankIndex(level: Int) = RANKS.indexOfLast { level >= it.minLevel }
    fun rank(level: Int) = RANKS[rankIndex(level)]
    fun nextRank(level: Int) = RANKS.getOrNull(rankIndex(level) + 1)

    /** 0..1 progress from the current rank's first level to the next rank's. */
    fun rankProgress(level: Int): Float {
        val r = rank(level)
        val n = nextRank(level) ?: return 1f
        return (level - r.minLevel) / (n.minLevel - r.minLevel).toFloat()
    }

    /** Adds XP, returns (newLevel, newXp, levelsGained). */
    fun addXp(level: Int, xp: Int, gained: Int): Triple<Int, Int, Int> {
        var l = level
        var x = xp + gained
        while (x >= xpToNext(l)) { x -= xpToNext(l); l++ }
        return Triple(l, x, l - level)
    }
}
