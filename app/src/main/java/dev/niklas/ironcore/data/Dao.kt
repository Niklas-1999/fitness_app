package dev.niklas.ironcore.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IronDao {
    @Query("SELECT * FROM profile WHERE id = 0") fun profile(): Flow<ProfileEntity?>
    @Query("SELECT * FROM player WHERE id = 0") fun player(): Flow<PlayerEntity?>
    @Query("SELECT * FROM progression") fun progression(): Flow<List<ProgressionEntity>>
    @Query("SELECT * FROM plan_slot ORDER BY dayOfWeek, ord") fun plan(): Flow<List<PlanSlotEntity>>
    @Query("SELECT * FROM cardio WHERE id = 0") fun cardio(): Flow<CardioEntity?>
    @Query("SELECT * FROM weight_log ORDER BY epochDay") fun weights(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM quest_log WHERE epochDay >= :from ORDER BY timestamp DESC") fun questsSince(from: Long): Flow<List<QuestLogEntity>>
    @Query("SELECT * FROM day_log WHERE epochDay >= :from ORDER BY epochDay") fun daysSince(from: Long): Flow<List<DayLogEntity>>

    // one-shot reads for transactions
    @Query("SELECT * FROM player WHERE id = 0") suspend fun playerNow(): PlayerEntity?
    @Query("SELECT * FROM profile WHERE id = 0") suspend fun profileNow(): ProfileEntity?
    @Query("SELECT * FROM progression WHERE ladderId = :id") suspend fun progressionNow(id: String): ProgressionEntity?
    @Query("SELECT * FROM day_log WHERE epochDay = :day") suspend fun dayNow(day: Long): DayLogEntity?
    @Query("SELECT itemKey FROM quest_log WHERE epochDay = :day") suspend fun keysOnDay(day: Long): List<String>
    @Query("SELECT * FROM cardio WHERE id = 0") suspend fun cardioNow(): CardioEntity?

    @Query("SELECT * FROM food ORDER BY lastUsed DESC, uses DESC") fun foods(): Flow<List<FoodEntity>>
    @Query("SELECT * FROM food_log WHERE epochDay >= :from ORDER BY timestamp DESC") fun foodLogSince(from: Long): Flow<List<FoodLogEntity>>
    @Query("SELECT * FROM weight_log WHERE epochDay >= :from ORDER BY epochDay") fun weightsSince(from: Long): Flow<List<WeightLogEntity>>
    @Query("SELECT COALESCE(SUM(protein), 0) FROM food_log WHERE epochDay = :day") suspend fun proteinOn(day: Long): Float
    @Query("SELECT * FROM food WHERE id = :id") suspend fun foodNow(id: Long): FoodEntity?

    @Insert suspend fun insertFood(f: FoodEntity): Long
    @androidx.room.Update suspend fun updateFood(f: FoodEntity)
    @Query("DELETE FROM food WHERE id = :id") suspend fun deleteFood(id: Long)
    @Insert suspend fun insertFoodLog(f: FoodLogEntity)
    @Query("DELETE FROM food_log WHERE id = :id") suspend fun deleteFoodLog(id: Long)
    @Query("DELETE FROM food") suspend fun clearFoods()
    @Query("DELETE FROM food_log") suspend fun clearFoodLog()

    @Query("""SELECT
        (SELECT COUNT(*) FROM quest_log) AS quests,
        (SELECT COUNT(*) FROM day_log WHERE cleared = 1) AS clears,
        (SELECT COUNT(*) FROM day_log WHERE restCheckIn = 1) AS restDays,
        (SELECT COUNT(*) FROM food_log) AS foodLogs,
        (SELECT COUNT(*) FROM weight_log) AS weighIns,
        (SELECT COUNT(*) FROM day_log WHERE proteinGoal = 1) AS proteinDays,
        (SELECT COUNT(*) FROM day_log WHERE waterGoal = 1) AS waterDays,
        (SELECT COALESCE(SUM(activeMin), 0) FROM day_log) AS activeMinutes,
        (SELECT COUNT(*) FROM quest_log WHERE itemKey LIKE 'squat%' OR itemKey LIKE 'lunge%' OR itemKey LIKE 'hinge%') AS legQuests""")
    fun counters(): Flow<Counters>
    @Query("SELECT * FROM achievement") fun achievements(): Flow<List<AchievementEntity>>
    @Query("SELECT * FROM cosmetics WHERE id = 0") fun cosmetics(): Flow<CosmeticsEntity?>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAchievements(a: List<AchievementEntity>)
    @Query("UPDATE achievement SET seen = 1") suspend fun markAchievementsSeen()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCosmetics(c: CosmeticsEntity)
    @Query("UPDATE profile SET handle = :handle") suspend fun setHandle(handle: String)
    @Query("DELETE FROM achievement") suspend fun clearAchievements()
    @Query("DELETE FROM cosmetics") suspend fun clearCosmetics()

    @Insert suspend fun insertQuest(q: QuestLogEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDay(d: DayLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProfile(p: ProfileEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPlayer(p: PlayerEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProgression(p: List<ProgressionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPlan(p: List<PlanSlotEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCardio(c: CardioEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWeight(w: WeightLogEntity)

    @Query("DELETE FROM plan_slot") suspend fun clearPlan()
    @Query("DELETE FROM progression") suspend fun clearProgression()
    @Query("DELETE FROM profile") suspend fun clearProfile()
    @Query("DELETE FROM player") suspend fun clearPlayer()
    @Query("DELETE FROM cardio") suspend fun clearCardio()
    @Query("DELETE FROM weight_log") suspend fun clearWeights()
    @Query("DELETE FROM quest_log") suspend fun clearQuests()
    @Query("DELETE FROM day_log") suspend fun clearDays()

    @Transaction
    suspend fun initialize(
        profile: ProfileEntity, player: PlayerEntity, slots: List<PlanSlotEntity>,
        progression: List<ProgressionEntity>, weight: WeightLogEntity
    ) {
        clearPlan(); clearProgression(); clearCardio(); clearQuests(); clearDays()
        upsertProfile(profile); upsertPlayer(player); insertPlan(slots); upsertProgression(progression)
        upsertCardio(CardioEntity()); upsertWeight(weight)
    }

    @Transaction
    suspend fun wipe() { clearPlan(); clearProgression(); clearProfile(); clearPlayer(); clearCardio(); clearWeights(); clearQuests(); clearDays(); clearFoodLog(); clearAchievements(); clearCosmetics() }
}
