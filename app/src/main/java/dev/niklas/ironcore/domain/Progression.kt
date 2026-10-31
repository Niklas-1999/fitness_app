package dev.niklas.ironcore.domain

import kotlin.math.roundToInt

/** Progress of one ladder (exercise family). Persisted per ladder. */
data class ProgState(
    val ladderId: String,
    val step: Int,
    val sets: Int,
    /** reps, seconds (HOLD) or reps at [loadKg] (LOAD) */
    val target: Int,
    val loadKg: Float? = null,
    val topHits: Int = 0,
    val fails: Int = 0,
    val sessions: Int = 0
)

enum class ProgEvent { NONE, TARGET_UP, UNLOCK_VARIATION, LOAD_UP, SET_ADDED, REPEAT, BACKOFF, STEP_DOWN }

/** Everything about the user that affects progression. */
data class ProgContext(
    val goal: Goal,
    val experience: Experience,
    val dumbbellMaxKg: Float? = null
)

/**
 * Double progression (RESEARCH §1, §3, §4):
 * - EASY/SOLID with all sets done: +1 rep (EASY +2) or +5 s hold, up to the top of the range.
 * - At the top of the range on two consecutive sessions: next variation (bodyweight)
 *   or +2.5 % / +5 % load (small / large muscle groups), back to the bottom of the range.
 * - HARD: repeat. FAILED twice in a row: back off ~10 %, or step down a variation.
 * - Sets build up every 6 successful sessions to 3 (novice) or 4 sets.
 */
object Progression {
    const val SESSIONS_PER_SET_STEP = 6

    fun range(v: Variation, ctx: ProgContext): IntRange {
        v.fixedRange?.let { return it }
        return when (v.measure) {
            Measure.HOLD -> 15..60
            Measure.REPS -> when (ctx.goal) {
                Goal.STRENGTH -> 6..10
                Goal.BUILD_MUSCLE -> 8..15
                Goal.ENDURANCE -> 12..25
                Goal.LOSE_FAT, Goal.GENERAL -> 10..15
            }
            Measure.LOAD -> when {
                ctx.goal == Goal.ENDURANCE -> 12..15
                ctx.experience.novice -> 8..12           // ACSM: novices 8–12 RM (A)
                ctx.goal == Goal.STRENGTH -> 6..10
                ctx.goal == Goal.BUILD_MUSCLE -> 8..12
                else -> 10..15
            }
        }
    }

    fun maxSets(exp: Experience) = if (exp.novice) 3 else 4

    private fun inc(v: Variation, r: Rating) = when (v.measure) {
        Measure.HOLD -> if (r == Rating.EASY) 10 else 5
        else -> if (r == Rating.EASY) 2 else 1
    }

    fun roundLoad(kg: Float) = (kg * 2).roundToInt() / 2f

    fun apply(s: ProgState, rating: Rating, ctx: ProgContext): Pair<ProgState, ProgEvent> {
        val ladder = Exercises.ladder(s.ladderId)
        val v = ladder.steps[s.step]
        val r = range(v, ctx)
        val sessions = s.sessions + 1
        return when (rating) {
            Rating.EASY, Rating.SOLID -> {
                // sets build-up first: volume before intensity
                if (sessions % SESSIONS_PER_SET_STEP == 0 && s.sets < maxSets(ctx.experience)) {
                    return s.copy(sets = s.sets + 1, sessions = sessions, fails = 0) to ProgEvent.SET_ADDED
                }
                if (s.target >= r.last) {
                    val hits = s.topHits + 1
                    if (hits < 2) return s.copy(topHits = hits, sessions = sessions, fails = 0) to ProgEvent.REPEAT
                    advance(s.copy(sessions = sessions, fails = 0, topHits = 0), ladder, ctx)
                } else {
                    s.copy(target = minOf(r.last, s.target + inc(v, rating)), topHits = 0, fails = 0, sessions = sessions) to ProgEvent.TARGET_UP
                }
            }
            Rating.HARD -> s.copy(topHits = 0, fails = 0, sessions = sessions) to ProgEvent.REPEAT
            Rating.FAILED -> {
                val fails = s.fails + 1
                if (fails < 2) return s.copy(fails = fails, topHits = 0, sessions = sessions) to ProgEvent.REPEAT
                backoff(s.copy(fails = 0, topHits = 0, sessions = sessions), ladder, ctx)
            }
        }
    }

    private fun advance(s: ProgState, ladder: Ladder, ctx: ProgContext): Pair<ProgState, ProgEvent> {
        if (ladder.isLoad) {
            val load = s.loadKg ?: return s to ProgEvent.REPEAT
            val pct = if (ladder.largeMuscle) .05f else .025f
            var next = roundLoad(load * (1 + pct)).coerceAtLeast(load + 1f)
            val cap = if (ladder.requires.any { it == Equip.DUMBBELLS } && Equip.GYM !in ladder.requires) ctx.dumbbellMaxKg else null
            if (cap != null && next > cap) {
                // dumbbells maxed out: keep load, extend the rep range instead
                val r = range(ladder.steps[s.step], ctx)
                return s.copy(target = minOf(s.target + 1, r.last + 8)) to ProgEvent.TARGET_UP
            }
            val r = range(ladder.steps[s.step], ctx)
            return s.copy(loadKg = next, target = r.first) to ProgEvent.LOAD_UP
        }
        if (s.step < ladder.steps.lastIndex) {
            val nv = ladder.steps[s.step + 1]
            return s.copy(step = s.step + 1, target = range(nv, ctx).first) to ProgEvent.UNLOCK_VARIATION
        }
        // top of the ladder: keep adding reps / time
        val r = range(ladder.steps[s.step], ctx)
        val extra = if (ladder.steps[s.step].measure == Measure.HOLD) 10 else 1
        return s.copy(target = minOf(s.target + extra, r.last * 2)) to ProgEvent.TARGET_UP
    }

    private fun backoff(s: ProgState, ladder: Ladder, ctx: ProgContext): Pair<ProgState, ProgEvent> {
        val v = ladder.steps[s.step]
        val r = range(v, ctx)
        if (ladder.isLoad) {
            val load = s.loadKg ?: return s to ProgEvent.REPEAT
            return s.copy(loadKg = roundLoad(load * .9f)) to ProgEvent.BACKOFF
        }
        if (s.target <= r.first && s.step > 0) {
            val pv = ladder.steps[s.step - 1]
            val pr = range(pv, ctx)
            return s.copy(step = s.step - 1, target = (pr.first + pr.last) / 2) to ProgEvent.STEP_DOWN
        }
        val dec = if (v.measure == Measure.HOLD) 5 else maxOf(1, (s.target * .1f).roundToInt())
        return s.copy(target = maxOf(if (s.step == 0) 1 else r.first, s.target - dec)) to ProgEvent.BACKOFF
    }

    /** Deload week: about half the sets, same exercises (RESEARCH §5, convention). */
    fun deloadSets(sets: Int) = maxOf(1, (sets + 1) / 2)
}
