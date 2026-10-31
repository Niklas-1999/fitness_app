package dev.niklas.ironcore.data

import androidx.room.Entity
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
    val lastActiveEpochDay: Long? = null
) {
    val stats get() = listOf(str, end, agi, vit, wil)
    val lastStats get() = listOf(lastStr, lastEnd, lastAgi, lastVit, lastWil)
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
