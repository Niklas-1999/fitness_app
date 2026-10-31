package dev.niklas.ironcore.domain

import kotlin.math.ceil

/**
 * Projections (SPEC §5). Body weight follows the chosen % rate (it bends because the rate
 * is a % of current weight) and is re-fitted from logs later. Performance projections run
 * the real progression rules with an assumed rating pattern. No kg-of-muscle predictions.
 */
object Forecast {
    const val DELOAD_EVERY_WEEKS = 6

    /** Assumed pattern: two SOLID sessions, then one HARD. */
    private val PATTERN = listOf(Rating.SOLID, Rating.SOLID, Rating.HARD)

    data class WeightPoint(val week: Int, val kg: Float)
    data class WeightForecast(val points: List<WeightPoint>, val etaWeeks: Int?)

    fun weight(startKg: Float, targetKg: Float?, weeklyPct: Float, lose: Boolean, maxWeeks: Int = 156): WeightForecast {
        if (targetKg == null || weeklyPct <= 0f) return WeightForecast(listOf(WeightPoint(0, startKg)), null)
        val pts = mutableListOf(WeightPoint(0, startKg))
        var w = startKg
        var eta: Int? = null
        for (week in 1..maxWeeks) {
            w = if (lose) w * (1 - weeklyPct / 100f) else w * (1 + weeklyPct / 100f)
            val reached = if (lose) w <= targetKg else w >= targetKg
            if (reached) { pts += WeightPoint(week, targetKg); eta = week; break }
            pts += WeightPoint(week, w)
        }
        return WeightForecast(pts, eta)
    }

    data class Unlock(val week: Int, val ladderId: String, val variation: Variation)

    /** Weeks until each next variation in each bodyweight ladder unlocks (within [horizonWeeks]). */
    fun unlocks(plan: Plan, ctx: ProgContext, horizonWeeks: Int = 52): List<Unlock> {
        val perWeek = plan.sessions.values.flatMap { it.items }.mapNotNull { it.ladderId }.groupingBy { it }.eachCount()
        val out = mutableListOf<Unlock>()
        for (state in plan.states) {
            val ladder = Exercises.ladder(state.ladderId)
            if (ladder.isLoad) continue
            val freq = perWeek[state.ladderId] ?: continue
            var s = state
            var k = 0
            for (week in 1..horizonWeeks) {
                if (week % DELOAD_EVERY_WEEKS == 0) continue
                repeat(freq) {
                    val (ns, ev) = Progression.apply(s, PATTERN[k++ % PATTERN.size], ctx)
                    if (ev == ProgEvent.UNLOCK_VARIATION) out += Unlock(week, ladder.id, ladder.steps[ns.step])
                    s = ns
                }
            }
        }
        return out.sortedBy { it.week }
    }

    /** Weeks to finish Couch to 5K with [runsPerWeek] runs (NHS plan: 27 runs). */
    fun c25kWeeks(runsPerWeek: Int): Int = if (runsPerWeek <= 0) 0 else ceil(C25K.RUNS.size / runsPerWeek.toDouble()).toInt()
}
