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

    @Transaction
    suspend fun initialize(
        profile: ProfileEntity, player: PlayerEntity, slots: List<PlanSlotEntity>,
        progression: List<ProgressionEntity>, weight: WeightLogEntity
    ) {
        clearPlan(); clearProgression(); clearCardio()
        upsertProfile(profile); upsertPlayer(player); insertPlan(slots); upsertProgression(progression)
        upsertCardio(CardioEntity()); upsertWeight(weight)
    }

    @Transaction
    suspend fun wipe() { clearPlan(); clearProgression(); clearProfile(); clearPlayer(); clearCardio(); clearWeights() }
}
