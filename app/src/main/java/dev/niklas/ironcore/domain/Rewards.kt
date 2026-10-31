package dev.niklas.ironcore.domain

import kotlin.math.roundToInt

/** XP, attribute points and streak rules (SPEC §4, §6). Pure functions, unit-tested. */
object Rewards {
    /** Base XP before multipliers: bigger quests are worth a bit more. */
    fun baseXp(kind: ItemKind, sets: Int = 0, minutes: Int = 0): Int = when (kind) {
        ItemKind.LADDER -> 20 + 5 * sets.coerceIn(1, 6)
        ItemKind.C25K -> 45
        ItemKind.WALK -> minutes.coerceIn(10, 45)
    }

    fun ratingMultiplier(r: Rating) = when (r) {
        Rating.EASY, Rating.SOLID -> 1f
        Rating.HARD -> 1.1f       // effort counts
        Rating.FAILED -> .5f      // showing up counts too
    }

    /** XP for the k-th (0-based) quest completed today. */
    fun questXp(base: Int, rating: Rating, kToday: Int, streak: Int): Int =
        (base * ratingMultiplier(rating) * Leveling.dailyMultiplier(kToday) * Leveling.streakMultiplier(streak)).roundToInt().coerceAtLeast(1)

    fun statFor(kind: ItemKind, ladderId: String?): Stat = when (kind) {
        ItemKind.LADDER -> Exercises.ladder(ladderId!!).stat
        ItemKind.C25K, ItemKind.WALK -> Stat.END
    }

    /** Points needed to raise an attribute from [value] to value+1: grows slowly with the value. */
    fun pointsToRaise(value: Int) = maxOf(2, value / 4)

    /** Adds [gained] points; returns (newValue, newPoints). */
    fun addPoints(value: Int, points: Int, gained: Int): Pair<Int, Int> {
        var v = value
        var p = points + gained
        while (p >= pointsToRaise(v)) { p -= pointsToRaise(v); v++ }
        return v to p
    }

    /** Streak after activity on [today]. Consecutive days count; a gap resets to 1. */
    fun streakAfterActivity(lastActive: Long?, streak: Int, today: Long): Int = when (lastActive) {
        today -> streak
        today - 1 -> streak + 1
        else -> 1
    }

    /** Streak as shown: a missed day (no activity yesterday or today) means 0. */
    fun effectiveStreak(lastActive: Long?, streak: Int, today: Long): Int =
        if (lastActive != null && lastActive >= today - 1) streak else 0

    /** Default rest between sets (ACSM: 1–2 min novice/intermediate, 2–3 min heavy core lifts). */
    fun restSeconds(m: Measure, largeMuscle: Boolean) = when (m) {
        Measure.LOAD -> if (largeMuscle) 120 else 90
        Measure.REPS -> 60
        Measure.HOLD -> 45
    }
}
