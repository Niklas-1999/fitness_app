package dev.niklas.ironcore.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local database. Version bumps MUST ship a Migration (never destructive),
 * so updates never wipe progress. Schemas are exported to app/schemas.
 */
@Database(
    entities = [ProfileEntity::class, PlayerEntity::class, ProgressionEntity::class, PlanSlotEntity::class,
        CardioEntity::class, WeightLogEntity::class, QuestLogEntity::class, DayLogEntity::class,
        FoodEntity::class, FoodLogEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),   // v2: quest_log, day_log, attribute points
        AutoMigration(from = 2, to = 3)    // v3: food, food_log, water / nutrition goals / reroll on day_log
    ]
)
abstract class IronDb : RoomDatabase() {
    abstract fun dao(): IronDao

    companion object {
        @Volatile private var instance: IronDb? = null
        fun get(context: Context): IronDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, IronDb::class.java, "ironcore.db")
                .build().also { instance = it }
        }
    }
}
