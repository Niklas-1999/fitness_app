package dev.niklas.ironcore.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.niklas.ironcore.domain.Activity
import dev.niklas.ironcore.domain.Equip
import dev.niklas.ironcore.domain.Experience
import dev.niklas.ironcore.domain.Goal
import dev.niklas.ironcore.domain.Intensity
import dev.niklas.ironcore.domain.ItemKind
import dev.niklas.ironcore.domain.ProgState
import dev.niklas.ironcore.domain.SessionType
import dev.niklas.ironcore.domain.Setup
import dev.niklas.ironcore.domain.Sex

/** Single row (id = 0): the onboarding answers. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val handle: String,
    val age: Int,
    val sex: Sex,
    val heightCm: Float,
    val weightKg: Float,
    val activity: Activity,
    val experience: Experience,
    val healthFlags: Int,
    val pregnant: Boolean,
    val goal: Goal,
    val goal2: Goal?,
    val targetWeightKg: Float?,
    val intensity: Intensity,
    val sessionMin: Int,
    val trainingDaysMask: Int,     // bit (day-1), 1 = Monday
    val equipmentMask: Int,
    val dumbbellMaxKg: Float?,
    val calPushStep: Int,
    val calPushReps: Int,
    val calSquatReps: Int,
    val calPlankSec: Int,
    val calPullReps: Int?,
    val planStartEpochDay: Long,
    val createdAt: Long
) {
    fun toSetup() = Setup(
        handle, age, sex, heightCm, weightKg, activity, experience, healthFlags, pregnant, goal, goal2, targetWeightKg,
        intensity, sessionMin, (1..7).filter { trainingDaysMask and (1 shl (it - 1)) != 0 }.toSet(),
        Equip.fromMask(equipmentMask), dumbbellMaxKg, calPushStep, calPushReps, calSquatReps, calPlankSec, calPullReps
    )

    companion object {
        fun from(s: Setup, planStartEpochDay: Long) = ProfileEntity(
            0, s.handle, s.age, s.sex, s.heightCm, s.weightKg, s.activity, s.experience, s.healthFlags, s.pregnant,
            s.goal, s.goal2, s.targetWeightKg, s.intensity, s.sessionMin,
            s.trainingDays.fold(0) { m, d -> m or (1 shl (d - 1)) }, Equip.toMask(s.equipment), s.dumbbellMaxKg,
            s.pushStep, s.pushReps, s.squatReps, s.plankSec, s.pullReps, planStartEpochDay, System.currentTimeMillis()
        )
    }
}

/** Single row (id = 0): RPG state. */
@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: Int = 0,
    val level: Int = 1,
    val xp: Int = 0,
    val totalXp: Int = 0,
    val str: Int, val end: Int, val agi: Int, val vit: Int, val wil: Int,
    /** stats snapshot at the previous level-up, for the "since last level" diff */
    val lastStr: Int, val lastEnd: Int, val lastAgi: Int, val lastVit: Int, val lastWil: Int,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveEpochDay: Long? = null,
    /** progress points toward the next attribute value (DB v2) */
    @ColumnInfo(defaultValue = "0") val strP: Int = 0,
    @ColumnInfo(defaultValue = "0") val endP: Int = 0,
    @ColumnInfo(defaultValue = "0") val agiP: Int = 0,
    @ColumnInfo(defaultValue = "0") val vitP: Int = 0,
    @ColumnInfo(defaultValue = "0") val wilP: Int = 0
) {
    val stats get() = listOf(str, end, agi, vit, wil)
    val lastStats get() = listOf(lastStr, lastEnd, lastAgi, lastVit, lastWil)
    val points get() = listOf(strP, endP, agiP, vitP, wilP)

    fun withStat(i: Int, value: Int, points: Int) = when (i) {
        0 -> copy(str = value, strP = points)
        1 -> copy(end = value, endP = points)
        2 -> copy(agi = value, agiP = points)
        3 -> copy(vit = value, vitP = points)
        else -> copy(wil = value, wilP = points)
    }

    fun snapshotStats() = copy(lastStr = str, lastEnd = end, lastAgi = agi, lastVit = vit, lastWil = wil)
}

@Entity(tableName = "progression")
data class ProgressionEntity(
    @PrimaryKey val ladderId: String,
    val step: Int,
    val sets: Int,
    val target: Int,
    val loadKg: Float?,
    val topHits: Int,
    val fails: Int,
    val sessions: Int
) {
    fun toState() = ProgState(ladderId, step, sets, target, loadKg, topHits, fails, sessions)
    companion object {
        fun from(s: ProgState) = ProgressionEntity(s.ladderId, s.step, s.sets, s.target, s.loadKg, s.topHits, s.fails, s.sessions)
    }
}

@Entity(tableName = "plan_slot", primaryKeys = ["dayOfWeek", "ord"])
data class PlanSlotEntity(
    val dayOfWeek: Int,
    val ord: Int,
    val sessionType: SessionType,
    val kind: ItemKind,
    val ladderId: String?,
    val minutes: Int
)

/** C25K position etc. Single row. */
@Entity(tableName = "cardio")
data class CardioEntity(@PrimaryKey val id: Int = 0, val c25kIndex: Int = 0)

@Entity(tableName = "weight_log")
data class WeightLogEntity(@PrimaryKey val epochDay: Long, val kg: Float)

/** One completed quest (DB v2). itemKey = ladderId, "c25k" or "walk". */
@Entity(tableName = "quest_log", indices = [Index("epochDay")])
data class QuestLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val itemKey: String,
    val kind: ItemKind,
    val variationName: String,
    val sets: Int,
    val target: Int,
    val loadKg: Float?,
    val minutes: Int,
    val rating: dev.niklas.ironcore.domain.Rating,
    val xp: Int,
    val deload: Boolean,
    val timestamp: Long
)

/** Per-day summary (DB v2). */
@Entity(tableName = "day_log")
data class DayLogEntity(
    @PrimaryKey val epochDay: Long,
    val xp: Int = 0,
    val quests: Int = 0,
    val restCheckIn: Boolean = false,
    val trainAnyway: Boolean = false,
    val cleared: Boolean = false,
    val activeMin: Int = 0,
    // DB v3
    @ColumnInfo(defaultValue = "0") val waterMl: Int = 0,
    @ColumnInfo(defaultValue = "0") val proteinGoal: Boolean = false,
    @ColumnInfo(defaultValue = "0") val waterGoal: Boolean = false,
    /** daily reroll: the directive key that was replaced and the ladder that replaces it */
    val rerollFrom: String? = null,
    val rerollTo: String? = null
)

/** Saved food with EU nutrition declaration values per 100 g / 100 ml (RESEARCH §12). DB v3. */
@Entity(tableName = "food")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcal: Float,
    val fat: Float,
    val satFat: Float,
    val carbs: Float,
    val sugar: Float,
    val protein: Float,
    val salt: Float,
    val fibre: Float?,
    /** last portion in grams, offered as default */
    val portionG: Float = 100f,
    val uses: Int = 0,
    val lastUsed: Long = 0
)

/** One eaten portion; values already scaled to the grams eaten (kept even if the food is edited). DB v3. */
@Entity(tableName = "food_log", indices = [Index("epochDay")])
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val foodId: Long?,
    val name: String,
    val grams: Float,
    val kcal: Float,
    val fat: Float,
    val satFat: Float,
    val carbs: Float,
    val sugar: Float,
    val protein: Float,
    val salt: Float,
    val fibre: Float,
    val timestamp: Long
)

/** Unlocked achievements (DB v4). */
@Entity(tableName = "achievement")
data class AchievementEntity(@PrimaryKey val id: String, val unlockedAt: Long, val seen: Boolean = false)

/** Profile cosmetics and custom images (DB v4). Single row. */
@Entity(tableName = "cosmetics")
data class CosmeticsEntity(
    @PrimaryKey val id: Int = 0,
    val avatarPath: String? = null,
    val bannerPath: String? = null,
    val title: String = "t_newblood",
    val frame: String = "f_hex",
    val banner: String = "b_grid",
    val nameFx: String = "n_none"
)

/** Lifetime counters for achievements (query result, not a table). */
data class Counters(
    val quests: Int,
    val clears: Int,
    val restDays: Int,
    val foodLogs: Int,
    val weighIns: Int,
    val proteinDays: Int,
    val waterDays: Int,
    val activeMinutes: Int,
    val legQuests: Int
)
