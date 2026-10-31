package dev.niklas.ironcore.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {
    @Test fun catalogIsConsistent() {
        val ids = Achievements.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        Achievements.ALL.forEach { assertTrue("reward ${it.reward} exists", Cosmetics.get(it.reward) != null) }
        // every non-default cosmetic is obtainable exactly once
        val rewards = Achievements.ALL.map { it.reward }
        assertEquals(rewards.size, rewards.toSet().size)
        assertEquals(Cosmetics.ALL.map { it.id }.toSet() - Cosmetics.DEFAULTS, rewards.toSet())
    }

    @Test fun unlocking() {
        val p = Progress(quests = 1, bestStreak = 7, level = 5, steps = mapOf("push_bw" to 3, "pull_bar" to 1))
        val got = Achievements.newlyUnlocked(p, emptySet()).map { it.id }.toSet()
        assertEquals(setOf("a_first", "a_s7", "a_l5", "a_pushup"), got)
        assertTrue(Achievements.newlyUnlocked(p, got).isEmpty())
        val cos = Achievements.unlockedCosmetics(got)
        assertTrue("t_firstblood" in cos && "n_glitch" in cos && "t_newblood" in cos)
    }
}
