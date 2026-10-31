package dev.niklas.ironcore.domain

/**
 * NHS Couch to 5K (RESEARCH §7): 9 weeks × 3 runs. Every run has a 5 min warm-up walk
 * and a 5 min cool-down walk. Segments are (run seconds, walk seconds); walk 0 = last segment.
 */
object C25K {
    data class Run(val week: Int, val run: Int, val segments: List<Pair<Int, Int>>) {
        val runSeconds get() = segments.sumOf { it.first }
        val label get() = "C25K W$week R$run"
        val summary: String get() {
            if (segments.size == 1) return "RUN ${segments[0].first / 60} MIN NON-STOP"
            val first = segments.first()
            return if (segments.all { it == first || it == segments.last() } && segments.size > 2)
                "${segments.size - 1} × (RUN ${fmt(first.first)} / WALK ${fmt(first.second)}) + RUN ${fmt(segments.last().first)}"
            else segments.joinToString(" · ") { (r, w) -> if (w > 0) "R ${fmt(r)} / W ${fmt(w)}" else "R ${fmt(r)}" }
        }
    }

    fun fmt(sec: Int) = if (sec % 60 == 0) "${sec / 60}:00" else "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"

    private fun rep(run: Int, walk: Int, times: Int, last: Int) = List(times) { run to walk } + (last to 0)
    private fun seq(vararg mins: Double): List<Pair<Int, Int>> {
        // alternating run, walk, run, … in minutes
        val s = mins.map { (it * 60).toInt() }
        return s.chunked(2).map { it[0] to (it.getOrNull(1) ?: 0) }
    }

    val RUNS: List<Run> = buildList {
        repeat(3) { add(Run(1, it + 1, rep(60, 90, 7, 60))) }
        repeat(3) { add(Run(2, it + 1, rep(90, 120, 5, 90))) }
        repeat(3) { add(Run(3, it + 1, seq(1.5, 1.5, 3.0, 3.0, 1.5, 1.5, 3.0))) }
        repeat(3) { add(Run(4, it + 1, seq(3.0, 1.5, 5.0, 2.5, 3.0, 1.5, 5.0))) }
        add(Run(5, 1, seq(5.0, 3.0, 5.0, 3.0, 5.0)))
        add(Run(5, 2, seq(8.0, 5.0, 8.0)))
        add(Run(5, 3, seq(20.0)))
        add(Run(6, 1, seq(5.0, 3.0, 8.0, 3.0, 5.0)))
        add(Run(6, 2, seq(10.0, 3.0, 10.0)))
        add(Run(6, 3, seq(25.0)))
        repeat(3) { add(Run(7, it + 1, seq(25.0))) }
        repeat(3) { add(Run(8, it + 1, seq(28.0))) }
        repeat(3) { add(Run(9, it + 1, seq(30.0))) }
    }

    /** Index into RUNS; after the last run, keep the 30 min run. */
    fun run(index: Int) = RUNS[index.coerceIn(0, RUNS.lastIndex)]
}
