package dev.niklas.ironcore.domain

import kotlin.math.roundToInt

/** Everything collected by onboarding (SPEC §2). Days: 1 = Monday … 7 = Sunday. */
data class Setup(
    val handle: String,
    val age: Int,
    val sex: Sex,
    val heightCm: Float,
    val weightKg: Float,
    val activity: Activity,
    val experience: Experience,
    val healthFlags: Int,          // number of YES answers in the health check
    val pregnant: Boolean,
    val goal: Goal,
    val goal2: Goal?,
    val targetWeightKg: Float?,
    val intensity: Intensity,
    val sessionMin: Int,           // 20 / 40 / 60
    val trainingDays: Set<Int>,
    val equipment: Set<Equip>,
    val dumbbellMaxKg: Float?,
    // calibration ("SYSTEM SCAN")
    val pushStep: Int,             // push_bw variation index the user tested
    val pushReps: Int,
    val squatReps: Int,
    val plankSec: Int,
    val pullReps: Int?             // null = no bar
) {
    val conservative get() = healthFlags > 0 || pregnant
    val progContext get() = ProgContext(goal, experience, dumbbellMaxKg)
    val wantsEndurance get() = goal == Goal.ENDURANCE || goal2 == Goal.ENDURANCE
}

enum class ItemKind { LADDER, C25K, WALK }

data class PlanItem(val kind: ItemKind, val ladderId: String? = null, val minutes: Int = 0)

enum class SessionType(val label: String) {
    FULL_A("FULL BODY // A"), FULL_B("FULL BODY // B"), UPPER("UPPER"), LOWER("LOWER"),
    RUN("RUN PROTOCOL"), CONDITIONING("CONDITIONING"), EXTRA("BONUS OPS // RECOVERY-AWARE")
}

data class Session(val type: SessionType, val items: List<PlanItem>)

data class Plan(
    val sessions: Map<Int, Session>,        // weekday -> session
    val states: List<ProgState>,
    val weeklyWalkGoalMin: Int = 150        // WHO 150 min/week (RESEARCH §6)
)

object PlanGenerator {
    private val FULL_A = listOf(Pattern.SQUAT, Pattern.PUSH, Pattern.ROW, Pattern.HINGE, Pattern.CORE, Pattern.PUSH_V)
    private val FULL_B = listOf(Pattern.HINGE, Pattern.PUSH_V, Pattern.PULL, Pattern.LUNGE, Pattern.CORE_SIDE, Pattern.PUSH)
    private val UPPER = listOf(Pattern.PUSH, Pattern.ROW, Pattern.PUSH_V, Pattern.PULL, Pattern.CORE, Pattern.CORE_SIDE)
    private val LOWER = listOf(Pattern.SQUAT, Pattern.HINGE, Pattern.LUNGE, Pattern.CORE_SIDE, Pattern.CORE)
    private val RUN_STRENGTH = listOf(Pattern.SQUAT, Pattern.PUSH, Pattern.ROW)
    private val CONDITIONING = listOf(Pattern.CORE, Pattern.CORE_SIDE)

    fun exerciseCount(sessionMin: Int) = when {
        sessionMin <= 20 -> 3
        sessionMin <= 40 -> 5
        else -> 6
    }

    /** Pick up to [k] days from [days] spread as evenly as possible (run days need rest between). */
    fun spread(days: List<Int>, k: Int): List<Int> {
        if (days.size <= k) return days
        return (0 until k).map { i -> days[(i * days.size) / k] }
    }

    fun generate(s: Setup): Plan {
        val days = s.trainingDays.sorted()
        val bodyweightFirst = s.goal == Goal.ENDURANCE
        val runDays = if (s.wantsEndurance) spread(days, 3).toSet() else emptySet()
        var strengthDays = days.filter { it !in runDays }
        val conditioningDays = mutableSetOf<Int>()
        // ACSM: novices train the whole body 2–3 d/week; extra days become conditioning
        val maxStrength = if (s.experience.novice) 3 else 6
        if (strengthDays.size > maxStrength) {
            val keep = spread(strengthDays, maxStrength).toSet()
            conditioningDays += strengthDays.filter { it !in keep }
            strengthDays = strengthDays.filter { it in keep }
        }
        val split = !s.experience.novice && strengthDays.size >= 4
        // endurance goal with no free strength day: attach a short strength block to 2 run days (WHO ≥2 d/week)
        val runWithStrength = if (strengthDays.isEmpty() && runDays.isNotEmpty()) spread(runDays.sorted(), 2).toSet() else emptySet()

        val count = exerciseCount(s.sessionMin)
        val sessions = linkedMapOf<Int, Session>()
        var rot = 0
        for (d in days) {
            val session = when {
                d in runDays -> {
                    val items = mutableListOf(PlanItem(ItemKind.C25K))
                    if (d in runWithStrength) items += ladders(RUN_STRENGTH, 3, s, bodyweightFirst)
                    Session(SessionType.RUN, items)
                }
                d in conditioningDays -> Session(
                    SessionType.CONDITIONING,
                    listOf(PlanItem(ItemKind.WALK, minutes = 30)) + ladders(CONDITIONING, 2, s, bodyweightFirst)
                )
                else -> {
                    val type = if (split) (if (rot % 2 == 0) SessionType.UPPER else SessionType.LOWER)
                    else (if (rot % 2 == 0) SessionType.FULL_A else SessionType.FULL_B)
                    rot++
                    val pats = when (type) {
                        SessionType.UPPER -> UPPER; SessionType.LOWER -> LOWER
                        SessionType.FULL_A -> FULL_A; else -> FULL_B
                    }
                    Session(type, ladders(pats, count, s, bodyweightFirst))
                }
            }
            sessions[d] = session
        }
        // WHO: adults 65+ add balance on ≥3 days
        if (s.age >= 65) {
            spread(days, 3).forEach { d ->
                sessions[d] = sessions.getValue(d).let { it.copy(items = it.items + PlanItem(ItemKind.LADDER, "balance")) }
            }
        }
        val used = sessions.values.flatMap { it.items }.mapNotNull { it.ladderId }.distinct()
        return Plan(sessions, used.map { initialState(it, s) })
    }

    private fun ladders(patterns: List<Pattern>, count: Int, s: Setup, bwFirst: Boolean): List<PlanItem> {
        val out = mutableListOf<String>()
        for (p in patterns) {
            if (out.size >= count) break
            val l = Exercises.pick(p, s.equipment, bwFirst)
                ?: if (p == Pattern.PULL) Exercises.pick(Pattern.ROW, s.equipment, bwFirst) else null
            if (l != null && l.id !in out) out += l.id
        }
        return out.map { PlanItem(ItemKind.LADDER, it) }
    }

    private fun startSets(s: Setup) = when {
        s.conservative -> 1
        s.experience.novice -> if (s.intensity == Intensity.GENTLE) 1 else 2
        else -> if (s.intensity == Intensity.GENTLE) 2 else 3
    }

    /** Fraction of the tested max used as the first target. */
    private fun factor(s: Setup) = if (s.conservative) .5f else when (s.intensity) {
        Intensity.GENTLE -> .5f; Intensity.STANDARD -> .6f; Intensity.HARD -> .7f
    }

    fun initialState(id: String, s: Setup): ProgState {
        val ladder = Exercises.ladder(id)
        val ctx = s.progContext
        val f = factor(s)
        val sets = startSets(s)
        fun r(step: Int) = Progression.range(ladder.steps[step], ctx)
        fun fromMax(step: Int, max: Int): ProgState {
            val rr = r(step)
            val t = (max * f).roundToInt()
            // within reach of the range: start inside it; otherwise a fraction of the max
            val target = if (max >= rr.first) t.coerceIn(rr.first, rr.last) else t.coerceIn(1, rr.last)
            return ProgState(id, step, sets, target)
        }
        if (ladder.isLoad) {
            val rr = r(0)
            return ProgState(id, 0, sets, (rr.first + rr.last) / 2, loadKg = null) // load calibrated in first session
        }
        return when (id) {
            "push_bw" -> {
                var step = s.pushStep.coerceIn(0, ladder.steps.lastIndex)
                val est = s.pushReps * f
                when {
                    est >= r(step).last && step < ladder.steps.lastIndex -> ProgState(id, step + 1, sets, r(step + 1).first)
                    est < r(step).first && step > 0 -> { step--; ProgState(id, step, sets, r(step).first) }
                    else -> fromMax(step, s.pushReps)
                }
            }
            "pushv_bw" -> ProgState(id, if (s.pushStep >= 3) 1 else 0, sets, r(if (s.pushStep >= 3) 1 else 0).first)
            "row_bw" -> { val st = if (s.pushStep >= 3) 2 else 0; ProgState(id, st, sets, r(st).first) }
            "pull_bar" -> {
                val p = s.pullReps ?: 0
                when {
                    p >= 3 -> fromMax(2, p)
                    p >= 1 -> ProgState(id, 1, sets, r(1).first)
                    else -> ProgState(id, 0, sets, r(0).first)
                }
            }
            "squat_bw" -> when {
                s.squatReps >= 40 -> ProgState(id, 2, sets, r(2).first)
                s.squatReps >= 5 -> fromMax(1, s.squatReps)
                else -> fromMax(0, maxOf(s.squatReps, 3))
            }
            "hinge_bw" -> { val st = if (s.squatReps >= 30) 1 else 0; ProgState(id, st, sets, r(st).first) }
            "core_front" -> if (s.plankSec < 15) ProgState(id, 0, sets, r(0).first)
                else ProgState(id, 1, sets, (s.plankSec * f).roundToInt().coerceIn(15, 60))
            "core_side" -> { val st = if (s.plankSec >= 30) 1 else 0; ProgState(id, st, sets, 15) }
            else -> ProgState(id, 0, sets, r(0).first)
        }
    }

    /**
     * Rest-day "TRAIN ANYWAY" session (SPEC §4): only ladders NOT trained in the last ~48 h
     * ([recent]); if too few strength options are fresh, a light conditioning session.
     */
    fun extraSession(planLadders: List<String>, recent: Set<String>, sessionMin: Int): Session {
        val core = setOf(Pattern.CORE, Pattern.CORE_SIDE, Pattern.BALANCE)
        val order = listOf(Pattern.SQUAT, Pattern.HINGE, Pattern.PUSH, Pattern.ROW, Pattern.PULL, Pattern.LUNGE, Pattern.PUSH_V)
        val fresh = planLadders.distinct().filter { it !in recent }
        val strength = fresh.filter { Exercises.ladder(it).pattern !in core }
            .sortedBy { order.indexOf(Exercises.ladder(it).pattern) }
        val freshCore = fresh.filter { Exercises.ladder(it).pattern in core && it != "balance" }
        val n = exerciseCount(sessionMin)
        return if (strength.size >= 2) {
            val picks = strength.take(maxOf(2, n - 1)) + freshCore.take(1)
            Session(SessionType.EXTRA, picks.map { PlanItem(ItemKind.LADDER, it) })
        } else {
            Session(SessionType.CONDITIONING, listOf(PlanItem(ItemKind.WALK, minutes = 30)) + freshCore.take(2).map { PlanItem(ItemKind.LADDER, it) })
        }
    }

    /** Every ladder the user's equipment allows (the "arsenal"), in a stable pattern order. */
    fun arsenal(equip: Set<Equip>): List<Ladder> =
        Exercises.ALL.filter { equip.containsAll(it.requires) }.sortedBy { it.pattern.ordinal }

    /**
     * Daily reroll replacement for [fromLadder]: another ladder of the same movement pattern if the
     * equipment allows, otherwise a fresh strength ladder from the plan (not in today's directive,
     * not trained in the last 48 h).
     */
    fun rerollTarget(fromLadder: String, equip: Set<Equip>, directive: Set<String>, planLadders: List<String>, recent: Set<String>): String? {
        val from = Exercises.ladder(fromLadder)
        val same = arsenal(equip).filter { it.pattern == from.pattern && it.id != fromLadder && it.id !in directive }
        same.firstOrNull { it.isLoad != from.isLoad }?.let { return it.id }
        same.firstOrNull()?.let { return it.id }
        val core = setOf(Pattern.CORE, Pattern.CORE_SIDE, Pattern.BALANCE)
        return planLadders.distinct().firstOrNull {
            it !in directive && it !in recent && (from.pattern in core) == (Exercises.ladder(it).pattern in core)
        } ?: arsenal(equip).firstOrNull { it.id !in directive && it.id != fromLadder }?.id
    }

    /** Starting attributes from the calibration scan (game design, not science). */
    fun initialStats(s: Setup): IntArray {
        val str = 5 + s.pushStep * 2 + minOf(s.pushReps, 40) / 5 + minOf(s.squatReps, 50) / 10 + minOf(s.pullReps ?: 0, 15) / 3
        val end = 5 + s.activity.ordinal * 2 + minOf(s.squatReps, 50) / 15
        val agi = 5 + minOf(s.plankSec, 180) / 20
        val wil = 5 + s.experience.ordinal
        return intArrayOf(str, end, agi, 5, wil)
    }
}
