package dev.niklas.ironcore

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.niklas.ironcore.data.DayLogEntity
import dev.niklas.ironcore.data.IronDb
import dev.niklas.ironcore.data.PlanSlotEntity
import dev.niklas.ironcore.data.PlayerEntity
import dev.niklas.ironcore.data.QuestDone
import dev.niklas.ironcore.data.QuestLogEntity
import dev.niklas.ironcore.data.QuestResult
import dev.niklas.ironcore.data.Repo
import dev.niklas.ironcore.domain.Forecast
import dev.niklas.ironcore.domain.ItemKind
import dev.niklas.ironcore.domain.Nutrition
import dev.niklas.ironcore.domain.NutritionTargets
import dev.niklas.ironcore.domain.PlanGenerator
import dev.niklas.ironcore.domain.Plan
import dev.niklas.ironcore.domain.ProgState
import dev.niklas.ironcore.domain.Rating
import dev.niklas.ironcore.domain.Rewards
import dev.niklas.ironcore.domain.SessionType
import dev.niklas.ironcore.domain.Setup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One quest of today's directive. key = ladderId, "c25k" or "walk". */
data class TodayItem(val key: String, val kind: ItemKind, val ladderId: String?, val minutes: Int)

sealed interface AppState {
    data object Loading : AppState
    data object NeedsSetup : AppState
    data class Ready(
        val setup: Setup,
        val player: PlayerEntity,
        val slots: Map<Int, List<PlanSlotEntity>>,
        val states: Map<String, ProgState>,
        val c25kIndex: Int,
        val planStartEpochDay: Long,
        val nutrition: NutritionTargets,
        val todayEpoch: Long,
        val day: DayLogEntity?,
        val log: List<QuestLogEntity>,          // last 14 days, newest first
        val weekActiveMin: Int,
        val directive: List<TodayItem>,
        val directiveType: SessionType?,
    ) : AppState {
        val today: LocalDate get() = LocalDate.ofEpochDay(todayEpoch)
        val todayDow get() = today.dayOfWeek.value
        val isTrainingDay get() = slots.containsKey(todayDow)
        val week get() = ((todayEpoch - planStartEpochDay) / 7 + 1).toInt()
        val deloadWeek get() = week % Forecast.DELOAD_EVERY_WEEKS == 0
        val weeksToDeload get() = Forecast.DELOAD_EVERY_WEEKS - (week % Forecast.DELOAD_EVERY_WEEKS)
        val doneToday: Map<String, QuestLogEntity> get() = log.filter { it.epochDay == todayEpoch }.associateBy { it.itemKey }
        val streak get() = Rewards.effectiveStreak(player.lastActiveEpochDay, player.streak, todayEpoch)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repo(IronDb.get(app))
    private val todayFlow = MutableStateFlow(LocalDate.now().toEpochDay())

    /** Reward of the last committed quest / check-in; the UI shows it and clears it. */
    var lastResult by mutableStateOf<QuestResult?>(null)

    fun refreshDay() { todayFlow.value = LocalDate.now().toEpochDay() }

    private val core = combine(repo.profile, repo.player, repo.plan, repo.progression, repo.cardio) { a, b, c, d, e -> listOf(a, b, c, d, e) }

    private val logs = todayFlow.flatMapLatest { today ->
        combine(repo.questsSince(today - 13), repo.daysSince(today - 6)) { q, d -> Triple(today, q, d) }
    }

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<AppState> = combine(core, logs) { c, (today, quests, days) ->
        val prof = c[0] as dev.niklas.ironcore.data.ProfileEntity?
        val player = c[1] as PlayerEntity?
        if (prof == null || player == null) return@combine AppState.NeedsSetup
        val plan = c[2] as List<PlanSlotEntity>
        val prog = c[3] as List<dev.niklas.ironcore.data.ProgressionEntity>
        val cardio = c[4] as dev.niklas.ironcore.data.CardioEntity?
        val s = prof.toSetup()
        val slots = plan.groupBy { it.dayOfWeek }
        val day = days.firstOrNull { it.epochDay == today }
        val dow = LocalDate.ofEpochDay(today).dayOfWeek.value
        val todaySlots = slots[dow]
        val (directive, type) = when {
            todaySlots != null -> todaySlots.map { TodayItem(keyOf(it.kind, it.ladderId), it.kind, it.ladderId, it.minutes) } to todaySlots.first().sessionType
            day?.trainAnyway == true -> {
                // recovery: ladders trained in the 2 days before today are excluded
                val recent = quests.filter { it.epochDay in (today - 2) until today && it.kind == ItemKind.LADDER }.map { it.itemKey }.toSet()
                val extra = PlanGenerator.extraSession(plan.mapNotNull { it.ladderId }, recent, s.sessionMin)
                extra.items.map { TodayItem(keyOf(it.kind, it.ladderId), it.kind, it.ladderId, it.minutes) } to extra.type
            }
            else -> emptyList<TodayItem>() to null
        }
        AppState.Ready(
            s, player, slots, prog.associate { it.ladderId to it.toState() }, cardio?.c25kIndex ?: 0, prof.planStartEpochDay,
            Nutrition.targets(s.sex, s.age, s.weightKg, s.heightCm, s.activity, s.goal, s.intensity, s.targetWeightKg, s.pregnant),
            today, day, quests, days.sumOf { it.activeMin }, directive, type
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    private fun keyOf(kind: ItemKind, ladderId: String?) = when (kind) {
        ItemKind.LADDER -> ladderId!!
        ItemKind.C25K -> "c25k"
        ItemKind.WALK -> "walk"
    }

    fun initialize(setup: Setup, plan: Plan) = viewModelScope.launch { repo.initialize(setup, plan) }
    fun wipe() = viewModelScope.launch { repo.wipe() }

    fun complete(item: TodayItem, rating: Rating, loadKg: Float? = null, minutes: Int = 0) = viewModelScope.launch {
        val s = state.value as? AppState.Ready ?: return@launch
        lastResult = repo.completeQuest(
            QuestDone(item.key, item.kind, item.ladderId, rating, loadKg, minutes),
            s.setup.progContext, s.deloadWeek, s.directive.map { it.key }
        )
    }

    fun restCheckIn() = viewModelScope.launch { repo.restCheckIn()?.let { lastResult = it } }
    fun trainAnyway(on: Boolean) = viewModelScope.launch { repo.setTrainAnyway(on) }
}
