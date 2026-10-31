package dev.niklas.ironcore.data

import androidx.room.withTransaction
import dev.niklas.ironcore.domain.Exercises
import dev.niklas.ironcore.domain.ItemKind
import dev.niklas.ironcore.domain.Leveling
import dev.niklas.ironcore.domain.Plan
import dev.niklas.ironcore.domain.PlanGenerator
import dev.niklas.ironcore.domain.ProgContext
import dev.niklas.ironcore.domain.ProgEvent
import dev.niklas.ironcore.domain.Progression
import dev.niklas.ironcore.domain.Rating
import dev.niklas.ironcore.domain.Rewards
import dev.niklas.ironcore.domain.Setup
import dev.niklas.ironcore.domain.Stat
import java.time.LocalDate

/** What happened when a quest / check-in was committed; drives the reward UI. */
data class QuestResult(
    val xp: Int,
    val bonusXp: Int,
    val stat: Stat?,
    val statRaised: Boolean,
    val event: ProgEvent,
    val nextTarget: String?,
    val streak: Int,
    val streakUp: Boolean,
    val levelFrom: Int,
    val levelTo: Int,
    val statsBefore: List<Int>,
    val statsAfter: List<Int>
) {
    val leveledUp get() = levelTo > levelFrom
}

/** A quest as the runner reports it back. */
data class QuestDone(
    val key: String,
    val kind: ItemKind,
    val ladderId: String?,
    val rating: Rating,
    val loadKg: Float? = null,
    val minutes: Int = 0
)

class Repo(private val db: IronDb) {
    private val dao = db.dao()
    val profile = dao.profile()
    val player = dao.player()
    val progression = dao.progression()
    val plan = dao.plan()
    val cardio = dao.cardio()
    val weights = dao.weights()
    fun questsSince(day: Long) = dao.questsSince(day)
    fun daysSince(day: Long) = dao.daysSince(day)

    private fun today() = LocalDate.now().toEpochDay()

    /** Writes everything produced by onboarding in one transaction. */
    suspend fun initialize(setup: Setup, plan: Plan) {
        val today = today()
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

    /**
     * Commits one completed quest: progression, XP (daily diminishing + streak), attribute points,
     * streak, directive-clear bonus and level-ups, atomically.
     * [directiveKeys] = all quest keys of today's directive (to detect a full clear).
     */
    suspend fun completeQuest(done: QuestDone, ctx: ProgContext, deload: Boolean, directiveKeys: List<String>): QuestResult =
        db.withTransaction {
            val today = today()
            val pl = dao.playerNow()!!
            val day = dao.dayNow(today) ?: DayLogEntity(today)
            val doneKeys = dao.keysOnDay(today)

            // streak
            val streak = Rewards.streakAfterActivity(pl.lastActiveEpochDay, pl.streak, today)
            val streakUp = streak > Rewards.effectiveStreak(pl.lastActiveEpochDay, pl.streak, today)

            // progression
            var event = ProgEvent.NONE
            var nextTarget: String? = null
            var sets = 0
            var target = done.minutes
            var varName = when (done.kind) { ItemKind.C25K -> "RUN / WALK"; ItemKind.WALK -> "BRISK WALK"; else -> "" }
            when (done.kind) {
                ItemKind.LADDER -> {
                    val e = dao.progressionNow(done.ladderId!!)!!
                    var st = e.toState()
                    if (done.loadKg != null) st = st.copy(loadKg = done.loadKg)   // calibration / manual change
                    sets = if (deload) Progression.deloadSets(st.sets) else st.sets
                    target = st.target
                    varName = Exercises.ladder(st.ladderId).steps[st.step].name
                    if (!deload) {
                        val (ns, ev) = Progression.apply(st, done.rating, ctx)
                        st = ns; event = ev
                    }
                    dao.upsertProgression(listOf(ProgressionEntity.from(st)))
                    nextTarget = dev.niklas.ironcore.domain.Format.variationName(st) + " // " + dev.niklas.ironcore.domain.Format.target(st, ctx)
                }
                ItemKind.C25K -> {
                    val c = dao.cardioNow() ?: CardioEntity()
                    if (done.rating != Rating.FAILED) {
                        dao.upsertCardio(c.copy(c25kIndex = c.c25kIndex + 1)); event = ProgEvent.TARGET_UP
                    } else event = ProgEvent.REPEAT
                    varName = dev.niklas.ironcore.domain.C25K.run(c.c25kIndex).label
                }
                ItemKind.WALK -> {}
            }

            // XP
            val base = Rewards.baseXp(done.kind, sets, done.minutes)
            val xp = Rewards.questXp(base, done.rating, day.quests, streak)
            val allKeys = (doneKeys + done.key).toSet()
            val clearsNow = !day.cleared && directiveKeys.isNotEmpty() && allKeys.containsAll(directiveKeys)
            val bonus = if (clearsNow) Leveling.DIRECTIVE_CLEAR_BONUS else 0

            // attributes: +1 point to the quest's stat (none on FAILED), WIL for the first activity of the day and for a clear
            var p = pl
            val stat = Rewards.statFor(done.kind, done.ladderId)
            val before = p.stats
            if (done.rating != Rating.FAILED) {
                val (v, pts) = Rewards.addPoints(p.stats[stat.ordinal], p.points[stat.ordinal], 1)
                p = p.withStat(stat.ordinal, v, pts)
            }
            val wilGain = (if (day.quests == 0 && !day.restCheckIn) 1 else 0) + (if (clearsNow) 1 else 0)
            if (wilGain > 0) {
                val (v, pts) = Rewards.addPoints(p.wil, p.wilP, wilGain)
                p = p.withStat(Stat.WIL.ordinal, v, pts)
            }
            val statRaised = p.stats[stat.ordinal] > before[stat.ordinal]

            // level
            val (lvl, rest, gained) = Leveling.addXp(p.level, p.xp, xp + bonus)
            val statsBeforeLevel = p.lastStats
            p = p.copy(level = lvl, xp = rest, totalXp = p.totalXp + xp + bonus, streak = streak,
                bestStreak = maxOf(p.bestStreak, streak), lastActiveEpochDay = today)
            if (gained > 0) p = p.snapshotStats()
            dao.upsertPlayer(p)

            dao.insertQuest(QuestLogEntity(
                epochDay = today, itemKey = done.key, kind = done.kind, variationName = varName, sets = sets,
                target = target, loadKg = done.loadKg, minutes = done.minutes, rating = done.rating, xp = xp + bonus,
                deload = deload, timestamp = System.currentTimeMillis()
            ))
            dao.upsertDay(day.copy(
                xp = day.xp + xp + bonus, quests = day.quests + 1, cleared = day.cleared || clearsNow,
                activeMin = day.activeMin + done.minutes
            ))
            QuestResult(xp, bonus, stat, statRaised, event, nextTarget, streak, streakUp, pl.level, lvl, statsBeforeLevel, p.stats)
        }

    /** Rest day check-in: small XP, +1 VIT point, keeps the streak. Once per day. */
    suspend fun restCheckIn(): QuestResult? = db.withTransaction {
        val today = today()
        val day = dao.dayNow(today) ?: DayLogEntity(today)
        if (day.restCheckIn) return@withTransaction null
        val pl = dao.playerNow()!!
        val streak = Rewards.streakAfterActivity(pl.lastActiveEpochDay, pl.streak, today)
        val streakUp = streak > Rewards.effectiveStreak(pl.lastActiveEpochDay, pl.streak, today)
        val (v, pts) = Rewards.addPoints(pl.vit, pl.vitP, 1)
        var p = pl.withStat(Stat.VIT.ordinal, v, pts)
        val statRaised = p.vit > pl.vit
        val (lvl, rest, gained) = Leveling.addXp(p.level, p.xp, Leveling.REST_XP)
        val before = p.lastStats
        p = p.copy(level = lvl, xp = rest, totalXp = p.totalXp + Leveling.REST_XP, streak = streak,
            bestStreak = maxOf(p.bestStreak, streak), lastActiveEpochDay = today)
        if (gained > 0) p = p.snapshotStats()
        dao.upsertPlayer(p)
        dao.upsertDay(day.copy(xp = day.xp + Leveling.REST_XP, restCheckIn = true))
        QuestResult(Leveling.REST_XP, 0, Stat.VIT, statRaised, ProgEvent.NONE, null, streak, streakUp, pl.level, lvl, before, p.stats)
    }

    suspend fun setTrainAnyway(on: Boolean) {
        val today = today()
        val day = dao.dayNow(today) ?: DayLogEntity(today)
        dao.upsertDay(day.copy(trainAnyway = on))
    }

    suspend fun wipe() = dao.wipe()
}
