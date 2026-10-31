package dev.niklas.ironcore.domain

/** Human-readable quest targets. */
object Format {
    fun target(s: ProgState, ctx: ProgContext, deload: Boolean = false): String {
        val ladder = Exercises.ladder(s.ladderId)
        val v = ladder.steps[s.step]
        val sets = if (deload) Progression.deloadSets(s.sets) else s.sets
        val side = if (v.perSide) " / SIDE" else ""
        return when (v.measure) {
            Measure.REPS -> "$sets × ${s.target}$side"
            Measure.HOLD -> "$sets × ${s.target} s$side"
            Measure.LOAD -> "$sets × ${s.target}$side @ " + (s.loadKg?.let { kg(it) } ?: "CALIBRATE")
        }
    }

    fun kg(v: Float) = if (v % 1f == 0f) "${v.toInt()} KG" else "%.1f KG".format(java.util.Locale.US, v)

    fun variationName(s: ProgState) = Exercises.ladder(s.ladderId).steps[s.step].name

    val DAY_SHORT = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
    val DAY_LONG = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
}
