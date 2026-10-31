package dev.niklas.ironcore.domain

import kotlin.math.roundToInt

/** Daily targets (RESEARCH §9–§12). All numbers are the starting estimate; P3 re-fits TDEE from logs. */
data class NutritionTargets(
    val bmr: Int,
    val tdee: Int,
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val fibreG: Int,
    val waterL: Float,
    /** EU reference intakes (Reg. 1169/2011 Annex XIII), shown for information */
    val sugarRefG: Int = 90,
    val saltRefG: Int = 6,
    /** signed kg per week the target aims for (negative = loss) */
    val weeklyChangeKg: Float,
    val note: String
)

object Nutrition {
    const val KCAL_PER_KG_TISSUE = 7700f  // initial estimate only; dynamics handled by adaptive re-fit (RESEARCH §9)

    /** Mifflin–St Jeor 1990. UNSPECIFIED uses the midpoint of the sex constants. */
    fun bmr(sex: Sex, kg: Float, cm: Float, age: Int): Float {
        val base = 10f * kg + 6.25f * cm - 5f * age
        return base + when (sex) {
            Sex.MALE -> 5f
            Sex.FEMALE -> -161f
            Sex.UNSPECIFIED -> -78f
        }
    }

    fun bmi(kg: Float, cm: Float) = kg / ((cm / 100f) * (cm / 100f))

    /** Fat loss: % body weight per week (Helms 2014). */
    fun lossRatePct(i: Intensity) = when (i) { Intensity.GENTLE -> .5f; Intensity.STANDARD -> .75f; Intensity.HARD -> 1f }

    /** Muscle gain: surplus % of maintenance (Iraki 2019: 10–20 %, aiming at 0.25–0.5 % BW/week). */
    fun surplusPct(i: Intensity) = when (i) { Intensity.GENTLE -> .10f; Intensity.STANDARD -> .15f; Intensity.HARD -> .20f }
    fun gainRatePct(i: Intensity) = when (i) { Intensity.GENTLE -> .25f; Intensity.STANDARD -> .35f; Intensity.HARD -> .5f }

    enum class Direction { LOSE, GAIN, MAINTAIN }

    fun direction(goal: Goal, kg: Float, targetKg: Float?, pregnant: Boolean): Direction = when {
        pregnant -> Direction.MAINTAIN
        targetKg == null -> if (goal == Goal.LOSE_FAT) Direction.LOSE else if (goal == Goal.BUILD_MUSCLE) Direction.GAIN else Direction.MAINTAIN
        targetKg < kg - .5f -> Direction.LOSE
        targetKg > kg + .5f -> Direction.GAIN
        else -> Direction.MAINTAIN
    }

    fun targets(
        sex: Sex, age: Int, kg: Float, cm: Float, activity: Activity,
        goal: Goal, intensity: Intensity, targetKg: Float?, pregnant: Boolean
    ): NutritionTargets {
        val bmr = bmr(sex, kg, cm, age)
        val tdee = bmr * activity.pal
        val dir = direction(goal, kg, targetKg, pregnant)
        var note = ""
        var weekly = 0f
        val kcal = when (dir) {
            Direction.LOSE -> {
                weekly = -kg * lossRatePct(intensity) / 100f
                val raw = tdee + weekly * KCAL_PER_KG_TISSUE / 7f
                if (raw < bmr) {
                    note = "Deficit capped at BMR (safety floor)."
                    weekly = (bmr - tdee) * 7f / KCAL_PER_KG_TISSUE
                    bmr
                } else raw
            }
            Direction.GAIN -> {
                weekly = kg * gainRatePct(intensity) / 100f
                tdee * (1 + surplusPct(intensity))
            }
            Direction.MAINTAIN -> {
                if (pregnant) note = "No deficit during pregnancy. Follow your doctor's advice."
                tdee
            }
        }
        // protein: 1.6 g/kg (Morton 2018), 2.0 g/kg in a deficit (ISSN upper bound)
        val refKg = if (dir == Direction.LOSE && bmi(kg, cm) > 30f && targetKg != null) targetKg else kg
        val protein = refKg * if (dir == Direction.LOSE) 2.0f else 1.6f
        val fat = kcal * .30f / 9f                      // 30 % E, EFSA range 20–35 %
        val carbs = ((kcal - protein * 4f - fat * 9f) / 4f).coerceAtLeast(0f)
        val water = when (sex) { Sex.MALE -> 2.5f; Sex.FEMALE -> 2.0f; Sex.UNSPECIFIED -> 2.25f }
        return NutritionTargets(
            bmr.roundToInt(), tdee.roundToInt(), kcal.roundToInt(), protein.roundToInt(), fat.roundToInt(),
            carbs.roundToInt(), 25, water, weeklyChangeKg = weekly, note = note
        )
    }
}
