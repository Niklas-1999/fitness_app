package dev.niklas.ironcore.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.niklas.ironcore.domain.Activity
import dev.niklas.ironcore.domain.Equip
import dev.niklas.ironcore.domain.Experience
import dev.niklas.ironcore.domain.Goal
import dev.niklas.ironcore.domain.Intensity
import dev.niklas.ironcore.domain.Nutrition
import dev.niklas.ironcore.domain.Setup
import dev.niklas.ironcore.domain.Sex

/** Health screening, paraphrased from the PAR-Q+ general questions (RESEARCH §8). */
val HEALTH_QUESTIONS = listOf(
    "Has a doctor ever told you that you have a heart condition or high blood pressure?",
    "Do you get chest pain at rest, in daily life, or when you are physically active?",
    "In the past 12 months, have you lost your balance from dizziness or lost consciousness?",
    "Have you been diagnosed with any other chronic medical condition?",
    "Do you currently take prescribed medication for a chronic condition?",
    "Do you have a bone, joint or soft-tissue problem (now or in the past 12 months) that more activity could make worse?",
    "Has a doctor said you should only exercise under medical supervision?"
)

enum class Step(val title: String, val tag: String) {
    INTRO("SYSTEM_INITIALIZATION", "BOOT"),
    HANDLE("IDENTITY", "ID"),
    BIOMETRICS("BIOMETRICS", "BIO"),
    ACTIVITY("DAILY_ACTIVITY", "PAL"),
    EXPERIENCE("TRAINING_HISTORY", "EXP"),
    HEALTH("HEALTH_CHECK", "MED"),
    GOAL("PRIMARY_OBJECTIVE", "OBJ"),
    TARGET("TARGET_PARAMETERS", "TGT"),
    INTENSITY("INTENSITY", "INT"),
    SCHEDULE("SCHEDULE", "CAL"),
    EQUIPMENT("EQUIPMENT", "EQP"),
    SCAN("SYSTEM_SCAN", "SCN"),
    COMPILE("COMPILING", "CMP"),
    REPORT("STATUS_REPORT", "RPT")
}

class OnboardingVM : ViewModel() {
    var step by mutableStateOf(Step.INTRO)

    var handle by mutableStateOf("")
    var age by mutableStateOf<Float?>(null)
    var sex by mutableStateOf<Sex?>(null)
    var heightCm by mutableStateOf<Float?>(null)
    var weightKg by mutableStateOf<Float?>(null)
    var activity by mutableStateOf<Activity?>(null)
    var experience by mutableStateOf<Experience?>(null)
    val health = mutableStateListOf<Boolean?>().apply { repeat(HEALTH_QUESTIONS.size) { add(null) } }
    var pregnant by mutableStateOf<Boolean?>(null)
    var goal by mutableStateOf<Goal?>(null)
    var goal2 by mutableStateOf<Goal?>(null)
    var targetKg by mutableStateOf<Float?>(null)
    var intensity by mutableStateOf<Intensity?>(null)
    val days = mutableStateListOf<Int>()
    var sessionMin by mutableStateOf(40)
    val equipment = mutableStateListOf<Equip>()
    var dumbbellMax by mutableStateOf<Float?>(null)
    var pushStep by mutableStateOf<Int?>(null)
    var pushReps by mutableStateOf<Float?>(null)
    var squatReps by mutableStateOf<Float?>(null)
    var plankSec by mutableStateOf<Float?>(null)
    var pullReps by mutableStateOf<Float?>(null)

    val askPregnancy get() = sex == Sex.FEMALE || sex == Sex.UNSPECIFIED
    val needsTarget get() = goal == Goal.LOSE_FAT || goal == Goal.BUILD_MUSCLE
    val hasBar get() = Equip.PULLUP_BAR in equipment || Equip.GYM in equipment
    val hasDumbbells get() = Equip.DUMBBELLS in equipment && Equip.GYM !in equipment

    /** Lowest target allowed: BMI 18.5 (WHO lower bound of normal weight). */
    val minHealthyKg get() = heightCm?.let { 18.5f * (it / 100f) * (it / 100f) }

    fun ageError(): String? = age?.let { if (it < 18) "IRONCORE is built for adults (18+). Its nutrition maths uses adult reference values." else null }

    fun targetError(): String? {
        val t = targetKg ?: return null
        val w = weightKg ?: return null
        val min = minHealthyKg ?: return null
        return when {
            t < min -> "Target is below a healthy weight for your height (BMI 18.5 = ${"%.1f".format(min)} kg)."
            goal == Goal.LOSE_FAT && t >= w -> "For fat loss the target must be below your current weight."
            goal == Goal.BUILD_MUSCLE && t <= w -> "For muscle gain the target must be above your current weight."
            else -> null
        }
    }

    fun valid(s: Step): Boolean = when (s) {
        Step.INTRO, Step.COMPILE, Step.REPORT -> true
        Step.HANDLE -> handle.isNotBlank()
        Step.BIOMETRICS -> age != null && ageError() == null && age!! <= 100 && sex != null &&
            (heightCm ?: 0f) in 120f..230f && (weightKg ?: 0f) in 35f..300f
        Step.ACTIVITY -> activity != null
        Step.EXPERIENCE -> experience != null
        Step.HEALTH -> health.all { it != null } && (!askPregnancy || pregnant != null)
        Step.GOAL -> goal != null
        Step.TARGET -> !needsTarget || (targetKg != null && targetError() == null)
        Step.INTENSITY -> intensity != null
        Step.SCHEDULE -> days.isNotEmpty()
        Step.EQUIPMENT -> !hasDumbbells || (dumbbellMax ?: 0f) > 0f
        Step.SCAN -> pushStep != null && pushReps != null && squatReps != null && plankSec != null && (!hasBar || pullReps != null)
    }

    private fun visible(s: Step) = when (s) {
        Step.TARGET -> needsTarget
        else -> true
    }

    val flow get() = Step.entries.filter { visible(it) }
    val index get() = flow.indexOf(step)

    fun next() { flow.getOrNull(index + 1)?.let { step = it } }
    fun back() { flow.getOrNull(index - 1)?.let { if (it != Step.COMPILE) step = it else step = Step.SCAN } }

    fun toSetup() = Setup(
        handle = handle.trim().uppercase(),
        age = age!!.toInt(),
        sex = sex!!,
        heightCm = heightCm!!,
        weightKg = weightKg!!,
        activity = activity!!,
        experience = experience!!,
        healthFlags = health.count { it == true },
        pregnant = askPregnancy && pregnant == true,
        goal = goal!!,
        goal2 = goal2,
        targetWeightKg = if (needsTarget) targetKg else null,
        intensity = intensity!!,
        sessionMin = sessionMin,
        trainingDays = days.toSet(),
        equipment = Equip.fromMask(Equip.toMask(equipment.toSet())),
        dumbbellMaxKg = if (hasDumbbells) dumbbellMax else null,
        pushStep = pushStep!!,
        pushReps = pushReps!!.toInt(),
        squatReps = squatReps!!.toInt(),
        plankSec = plankSec!!.toInt(),
        pullReps = if (hasBar) pullReps?.toInt() else null
    )

    /** Weekly rate preview for the TARGET step. */
    fun ratePreview(i: Intensity): String? {
        val w = weightKg ?: return null
        return when (goal) {
            Goal.LOSE_FAT -> "−%.2f kg/week".format(w * Nutrition.lossRatePct(i) / 100f)
            Goal.BUILD_MUSCLE -> "+%.2f kg/week".format(w * Nutrition.gainRatePct(i) / 100f)
            else -> null
        }
    }
}
