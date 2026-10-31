package dev.niklas.ironcore.domain

import kotlin.math.roundToInt

/**
 * Adaptive TDEE (RESEARCH §9): the formula is only the starting estimate. With enough logs,
 * observed TDEE = average intake − (trend weight change × 7700 kcal/kg) / days, blended
 * with the formula by data coverage. Short windows keep the 7700 approximation reasonable;
 * the slope is a least-squares line through all weigh-ins in the window (no lag, robust to
 * day-to-day water swings); the moving-average trend is only for display. [engineering]
 */
object Adaptive {
    const val WINDOW_DAYS = 28
    const val MIN_SPAN_DAYS = 14
    const val MIN_LOGGED_DAYS = 10
    const val MIN_WEIGHINS = 4
    private const val ALPHA = .1f   // EMA smoothing (≈ 10-day memory)

    data class Result(val tdee: Int, val confidence: Float, val observedTdee: Int?, val trendKg: Float?, val weeklyChangeKg: Float?)

    /** EMA trend over daily weights; missing days carry the last trend forward. */
    fun trend(weights: Map<Long, Float>, from: Long, to: Long): Map<Long, Float> {
        val out = linkedMapOf<Long, Float>()
        var t: Float? = null
        for (d in from..to) {
            val w = weights[d]
            t = when { t == null -> w; w == null -> t; else -> t + ALPHA * (w - t) }
            if (t != null) out[d] = t
        }
        return out
    }

    /** Least-squares slope (kg/day) of weight over day. */
    fun olsSlope(w: Map<Long, Float>): Float {
        val xm = w.keys.average(); val ym = w.values.average()
        var num = 0.0; var den = 0.0
        for ((x, y) in w) { num += (x - xm) * (y - ym); den += (x - xm) * (x - xm) }
        return if (den == 0.0) 0f else (num / den).toFloat()
    }

    /** [intake]: kcal per day for days with food logged. [weights]: kg per day. */
    fun estimate(formulaTdee: Int, today: Long, weights: Map<Long, Float>, intake: Map<Long, Int>): Result {
        val from = today - WINDOW_DAYS + 1
        val w = weights.filterKeys { it in from..today }
        val tr = trend(weights.filterKeys { it <= today }, minOf(from, weights.keys.minOrNull() ?: from), today)
        val trendNow = tr[today]
        val firstW = w.keys.minOrNull()
        val logged = intake.filterKeys { it in from..today && (firstW == null || it >= firstW) }.filterValues { it > 0 }
        if (firstW == null || w.size < MIN_WEIGHINS || today - firstW < MIN_SPAN_DAYS || logged.size < MIN_LOGGED_DAYS || trendNow == null)
            return Result(formulaTdee, 0f, null, trendNow, null)
        val slope = olsSlope(w)                                                  // kg/day
        val observed = (logged.values.average() - slope * Nutrition.KCAL_PER_KG_TISSUE).toFloat()
            .coerceIn(formulaTdee * .6f, formulaTdee * 1.6f)
        val conf = (logged.size / 21f).coerceAtMost(1f)
        return Result((formulaTdee * (1 - conf) + observed * conf).roundToInt(), conf, observed.roundToInt(), trendNow, slope * 7)
    }
}
