package dev.niklas.ironcore.data

import dev.niklas.ironcore.domain.Leveling
import dev.niklas.ironcore.domain.Plan
import dev.niklas.ironcore.domain.PlanGenerator
import dev.niklas.ironcore.domain.Setup
import java.time.LocalDate

class Repo(private val dao: IronDao) {
    val profile = dao.profile()
    val player = dao.player()
    val progression = dao.progression()
    val plan = dao.plan()
    val cardio = dao.cardio()
    val weights = dao.weights()

    /** Writes everything produced by onboarding in one transaction. */
    suspend fun initialize(setup: Setup, plan: Plan) {
        val today = LocalDate.now().toEpochDay()
        val st = PlanGenerator.initialStats(setup)
        val (lvl, xp, _) = Leveling.addXp(1, 0, Leveling.INIT_BONUS)
        val player = PlayerEntity(
            level = lvl, xp = xp, totalXp = Leveling.INIT_BONUS,
            str = st[0], end = st[1], agi = st[2], vit = st[3], wil = st[4],
            lastStr = st[0], lastEnd = st[1], lastAgi = st[2], lastVit = st[3], lastWil = st[4]
        )
        val slots = plan.sessions.flatMap { (day, s) ->
            s.items.mapIndexed { i, it -> PlanSlotEntity(day, i, s.type, it.kind, it.ladderId, it.minutes) }
        }
        dao.initialize(
            ProfileEntity.from(setup, today), player, slots,
            plan.states.map { ProgressionEntity.from(it) }, WeightLogEntity(today, setup.weightKg)
        )
    }

    suspend fun wipe() = dao.wipe()
}
