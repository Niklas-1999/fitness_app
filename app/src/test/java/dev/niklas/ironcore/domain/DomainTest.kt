package dev.niklas.ironcore.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    private fun setup(
        days: Set<Int> = setOf(1, 3, 5), goal: Goal = Goal.GENERAL, exp: Experience = Experience.NONE,
        equip: Set<Equip> = emptySet(), age: Int = 30, pushStep: Int = 2, pushReps: Int = 12, squat: Int = 20,
        plank: Int = 40, pull: Int? = null, intensity: Intensity = Intensity.STANDARD, flags: Int = 0, min: Int = 40
    ) = Setup("T", age, Sex.MALE, 180f, 80f, Activity.MODERATE, exp, flags, false, goal, null, null, intensity, min,
        days, equip, null, pushStep, pushReps, squat, plank, pull)

    // ---------- nutrition (RESEARCH §9–11) ----------
    @Test fun mifflinMale() = assertEquals(1780f, Nutrition.bmr(Sex.MALE, 80f, 180f, 30), .01f)
    @Test fun mifflinFemale() = assertEquals(1320.0f, Nutrition.bmr(Sex.FEMALE, 60f, 165f, 30), .5f)

    @Test fun lossTargetsAndFloor() {
        val t = Nutrition.targets(Sex.MALE, 30, 80f, 180f, Activity.MODERATE, Goal.LOSE_FAT, Intensity.STANDARD, 72f, false)
        assertEquals(2848, t.tdee)                        // 1780 * 1.6
        // 0.75 % of 80 kg = 0.6 kg/week -> 660 kcal/day deficit
        assertEquals(2188, t.kcal)
        assertEquals(160, t.proteinG)                     // 2.0 g/kg in deficit
        assertEquals(-.6f, t.weeklyChangeKg, .001f)
        val hard = Nutrition.targets(Sex.FEMALE, 60, 55f, 160f, Activity.LOW, Goal.LOSE_FAT, Intensity.HARD, 50f, false)
        assertTrue("never below BMR", hard.kcal >= hard.bmr)
    }

    @Test fun gainSurplusAndPregnancy() {
        val g = Nutrition.targets(Sex.MALE, 30, 80f, 180f, Activity.MODERATE, Goal.BUILD_MUSCLE, Intensity.STANDARD, null, false)
        assertTrue(kotlin.math.abs((2848 * 1.15).toInt() - g.kcal) <= 1)
        assertEquals(128, g.proteinG)                     // 1.6 g/kg
        val p = Nutrition.targets(Sex.FEMALE, 30, 70f, 170f, Activity.MODERATE, Goal.LOSE_FAT, Intensity.HARD, 60f, true)
        assertEquals(p.tdee, p.kcal)
    }

    // ---------- leveling ----------
    @Test fun levelCurve() {
        assertEquals(80, Leveling.xpToNext(1))
        assertEquals(725, Leveling.xpToNext(30))
        assertEquals("LEGEND", Leveling.rank(53).name)
        assertEquals("APEX", Leveling.rank(52).name)
        val (l, x, gained) = Leveling.addXp(1, 0, 181)
        assertEquals(3, l); assertEquals(2, gained); assertEquals(181 - 80 - 87, x)
    }

    // ---------- progression (RESEARCH §1, §3) ----------
    @Test fun doubleProgressionUnlocksNextVariation() {
        val ctx = ProgContext(Goal.GENERAL, Experience.INTERMEDIATE)
        var s = ProgState("push_bw", 2, 3, 15)            // knee push-up at top of 10..15
        s = Progression.apply(s, Rating.SOLID, ctx).first
        assertEquals(2, s.step)                            // first top hit: repeat
        val (s2, ev) = Progression.apply(s, Rating.SOLID, ctx)
        assertEquals(ProgEvent.UNLOCK_VARIATION, ev)
        assertEquals(3, s2.step); assertEquals(10, s2.target)
    }

    @Test fun failTwiceBacksOff() {
        val ctx = ProgContext(Goal.GENERAL, Experience.INTERMEDIATE)
        var s = ProgState("push_bw", 3, 3, 10)
        s = Progression.apply(s, Rating.FAILED, ctx).first
        val (s2, ev) = Progression.apply(s, Rating.FAILED, ctx)
        assertEquals(ProgEvent.STEP_DOWN, ev); assertEquals(2, s2.step)
    }

    @Test fun loadProgressionAcsm() {
        val ctx = ProgContext(Goal.STRENGTH, Experience.INTERMEDIATE)
        var s = ProgState("squat_gym", 0, 3, 10, loadKg = 60f)
        s = Progression.apply(s, Rating.SOLID, ctx).first
        val (s2, ev) = Progression.apply(s, Rating.SOLID, ctx)
        assertEquals(ProgEvent.LOAD_UP, ev); assertEquals(63f, s2.loadKg!!, .01f); assertEquals(6, s2.target)
    }

    @Test fun dumbbellCapExtendsReps() {
        val ctx = ProgContext(Goal.BUILD_MUSCLE, Experience.INTERMEDIATE, dumbbellMaxKg = 20f)
        var s = ProgState("squat_db", 0, 3, 12, loadKg = 20f)
        s = Progression.apply(s, Rating.SOLID, ctx).first
        val s2 = Progression.apply(s, Rating.SOLID, ctx).first
        assertEquals(20f, s2.loadKg!!, .01f); assertEquals(13, s2.target)
    }

    // ---------- plan generator (RESEARCH §1, §6, §7) ----------
    @Test fun noviceThreeDaysFullBody() {
        val p = PlanGenerator.generate(setup())
        assertEquals(3, p.sessions.size)
        assertTrue(p.sessions.values.all { it.type == SessionType.FULL_A || it.type == SessionType.FULL_B })
        assertTrue(p.sessions.values.all { it.items.size == 5 })
        assertTrue(p.states.none { Exercises.ladder(it.ladderId).isLoad })
    }

    @Test fun noviceExtraDaysBecomeConditioning() {
        val p = PlanGenerator.generate(setup(days = setOf(1, 2, 3, 4, 5)))
        assertEquals(3, p.sessions.values.count { it.type == SessionType.FULL_A || it.type == SessionType.FULL_B })
        assertEquals(2, p.sessions.values.count { it.type == SessionType.CONDITIONING })
    }

    @Test fun intermediateFourDaysSplit() {
        val p = PlanGenerator.generate(setup(days = setOf(1, 2, 4, 5), exp = Experience.INTERMEDIATE, equip = Equip.fromMask(Equip.GYM.bit)))
        assertEquals(listOf(SessionType.UPPER, SessionType.LOWER, SessionType.UPPER, SessionType.LOWER), p.sessions.values.map { it.type })
        assertTrue("gym picks barbell", p.states.any { it.ladderId == "push_gym" })
        assertNull(p.states.first { it.ladderId == "push_gym" }.loadKg)
    }

    @Test fun enduranceGetsC25kAndStrength() {
        val p = PlanGenerator.generate(setup(goal = Goal.ENDURANCE, days = setOf(2, 4, 6)))
        assertEquals(3, p.sessions.values.count { it.type == SessionType.RUN })
        assertEquals("WHO >=2 strength days", 2, p.sessions.values.count { s -> s.items.any { it.kind == ItemKind.LADDER } })
    }

    @Test fun olderAdultsGetBalance() {
        val p = PlanGenerator.generate(setup(age = 70))
        assertEquals(3, p.sessions.values.count { s -> s.items.any { it.ladderId == "balance" } })
    }

    @Test fun calibrationStartsSensibly() {
        // 12 knee push-ups at 0.6 -> 7.2 < range 10: step down to incline push-ups
        val s = PlanGenerator.initialState("push_bw", setup(pushStep = 2, pushReps = 12))
        assertEquals(1, s.step)
        val weak = PlanGenerator.initialState("squat_bw", setup(squat = 3))
        assertEquals(0, weak.step); assertTrue(weak.target in 1..3)
        val strong = PlanGenerator.initialState("pull_bar", setup(pull = 8, equip = setOf(Equip.PULLUP_BAR)))
        assertEquals(2, strong.step)
        assertEquals(1, PlanGenerator.initialState("push_bw", setup(flags = 1)).sets)
    }

    // ---------- cardio + forecast ----------
    @Test fun c25k() {
        assertEquals(27, C25K.RUNS.size)
        assertEquals(60 * 8, C25K.RUNS[0].runSeconds)
        assertEquals(30 * 60, C25K.RUNS.last().runSeconds)
        assertEquals(9, Forecast.c25kWeeks(3)); assertEquals(14, Forecast.c25kWeeks(2))
    }

    @Test fun forecasts() {
        val w = Forecast.weight(80f, 72f, .75f, lose = true)
        assertTrue(w.etaWeeks!! in 13..15)                 // ln(72/80)/ln(1-0.0075) ≈ 14
        val plan = PlanGenerator.generate(setup())
        val u = Forecast.unlocks(plan, setup().progContext)
        assertTrue(u.isNotEmpty())
        assertTrue(u.first().week >= 1)
        assertFalse(u.any { it.week % Forecast.DELOAD_EVERY_WEEKS == 0 })
    }
}
