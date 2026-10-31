package dev.niklas.ironcore.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTest {
    @Test fun notEnoughDataKeepsFormula() {
        val r = Adaptive.estimate(2500, 100, mapOf(95L to 80f, 99L to 79.8f), mapOf(99L to 2000))
        assertEquals(2500, r.tdee); assertNull(r.observedTdee)
    }

    @Test fun stableWeightMeansTdeeEqualsIntake() {
        val w = (73L..100L).associateWith { 80f }
        val i = (73L..100L).associateWith { 2200 }
        val r = Adaptive.estimate(2600, 100, w, i)
        assertEquals(2200, r.observedTdee)
        assertEquals(2200, r.tdee)            // full confidence after 21+ logged days
    }

    @Test fun losingWeightRaisesObservedTdee() {
        // losing 0.5 kg/week on 2000 kcal -> TDEE ≈ 2000 + 0.5/7*7700 ≈ 2550
        val w = (73L..100L).associateWith { 80f - (it - 73) * (0.5f / 7f) }
        val i = (73L..100L).associateWith { 2000 }
        val r = Adaptive.estimate(2400, 100, w, i)
        assertTrue("observed ${r.observedTdee}", r.observedTdee!! in 2540..2560)
        assertEquals(-.5f, r.weeklyChangeKg!!, .01f)
    }
}
