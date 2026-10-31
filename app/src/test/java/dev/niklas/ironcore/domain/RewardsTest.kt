package dev.niklas.ironcore.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardsTest {
    @Test fun dailyDiminishingXp() {
        val base = Rewards.baseXp(ItemKind.LADDER, sets = 2)          // 30
        assertEquals(30, base)
        val xs = (0 until 10).map { Rewards.questXp(base, Rating.SOLID, it, 0) }
        assertEquals(listOf(30, 30, 30, 27, 24, 18, 9, 5, 2, 2), xs)
        assertEquals(15, Rewards.questXp(base, Rating.FAILED, 0, 0))
        assertEquals(39, Rewards.questXp(base, Rating.SOLID, 0, 15))   // +30 % streak cap
    }

    @Test fun attributePointsSlowDown() {
        assertEquals(6 to 1, Rewards.addPoints(5, 0, 3))               // needs 2 at 5
        assertEquals(15 to 2, Rewards.addPoints(15, 0, 2))              // needs 3 at 15
        assertEquals(16 to 0, Rewards.addPoints(15, 2, 1))
        assertEquals(40 to 9, Rewards.addPoints(40, 0, 9))              // needs 10 at 40
    }

    @Test fun streakRules() {
        assertEquals(1, Rewards.streakAfterActivity(null, 0, 100))
        assertEquals(5, Rewards.streakAfterActivity(99, 4, 100))
        assertEquals(4, Rewards.streakAfterActivity(100, 4, 100))
        assertEquals(1, Rewards.streakAfterActivity(97, 9, 100))        // missed days reset
        assertEquals(4, Rewards.effectiveStreak(99, 4, 100))
        assertEquals(0, Rewards.effectiveStreak(98, 4, 100))
    }

    @Test fun extraSessionRespectsRecovery() {
        val plan = listOf("squat_bw", "push_bw", "row_bw", "hinge_bw", "core_front", "pushv_bw", "core_side")
        val recent = setOf("squat_bw", "push_bw", "core_front")
        val s = PlanGenerator.extraSession(plan, recent, 40)
        assertEquals(SessionType.EXTRA, s.type)
        assertTrue(s.items.none { it.ladderId in recent })
        assertEquals(listOf("hinge_bw", "row_bw", "pushv_bw", "core_side"), s.items.map { it.ladderId })
        val all = PlanGenerator.extraSession(plan, plan.toSet() - "core_side", 40)
        assertEquals(SessionType.CONDITIONING, all.type)
        assertEquals(ItemKind.WALK, all.items.first().kind)
    }
}
