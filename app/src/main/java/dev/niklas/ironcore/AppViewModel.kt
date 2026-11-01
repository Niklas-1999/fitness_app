package dev.niklas.ironcore

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.niklas.ironcore.data.DayLogEntity
import dev.niklas.ironcore.data.FoodEntity
import dev.niklas.ironcore.data.FoodLogEntity
import dev.niklas.ironcore.data.WeightLogEntity
import dev.niklas.ironcore.domain.Adaptive
import dev.niklas.ironcore.domain.Achievements
import dev.niklas.ironcore.domain.Progress
import dev.niklas.ironcore.data.AchievementEntity
import dev.niklas.ironcore.data.CosmeticsEntity
import dev.niklas.ironcore.data.Counters
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
        val canReroll: Boolean,
        val foods: List<FoodEntity>,
        val foodLog: List<FoodLogEntity>,           // last 28 days, newest first
        val weights: List<WeightLogEntity>,         // last 60 days
        val adaptive: Adaptive.Result,
        val recentLadders: Set<String>,
        val progress: Progress,
        val achievements: Map<String, AchievementEntity>,
        val cosmetics: CosmeticsEntity,
    ) : AppState {
        val unlockedCosmetics get() = Achievements.unlockedCosmetics(achievements.keys)
        val unseenAchievements get() = achievements.values.filter { !it.seen }.sortedBy { it.unlockedAt }
        val foodToday get() = foodLog.filter { it.epochDay == todayEpoch }
        val waterTodayMl get() = day?.waterMl ?: 0
        fun stateFor(id: String): ProgState = states[id] ?: PlanGenerator.initialState(id, setup)
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

    private data class Logs(val today: Long, val quests: List<QuestLogEntity>, val days: List<DayLogEntity>,
                            val food: List<FoodLogEntity>, val weights: List<WeightLogEntity>)

    private val logs = todayFlow.flatMapLatest { today ->
        combine(repo.questsSince(today - 13), repo.daysSince(today - 6), repo.foodLogSince(today - 27), repo.weightsSince(today - 59)) { q, d, f, w ->
            Logs(today, q, d, f, w)
        }
    }

    private data class Meta(val counters: Counters, val achievements: List<AchievementEntity>, val cosmetics: CosmeticsEntity?)
    private val meta = combine(repo.counters, repo.achievements, repo.cosmetics) { a, b, c -> Meta(a, b, c) }

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<AppState> = combine(core, logs, repo.foods, meta) { c, lg, foods, mt ->
        val today = lg.today; val quests = lg.quests; val days = lg.days; val foodLog = lg.food; val weights = lg.weights
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
        val (rawDirective, type) = when {
            todaySlots != null -> todaySlots.map { TodayItem(keyOf(it.kind, it.ladderId), it.kind, it.ladderId, it.minutes) } to todaySlots.first().sessionType
            day?.trainAnyway == true -> {
                // recovery: ladders trained in the 2 days before today are excluded
                val recent = quests.filter { it.epochDay in (today - 2) until today && it.kind == ItemKind.LADDER }.map { it.itemKey }.toSet()
                val extra = PlanGenerator.extraSession(plan.mapNotNull { it.ladderId }, recent, s.sessionMin)
                extra.items.map { TodayItem(keyOf(it.kind, it.ladderId), it.kind, it.ladderId, it.minutes) } to extra.type
            }
            else -> emptyList<TodayItem>() to null
        }
        // daily reroll replaces one ladder quest
        val directive = rawDirective.map { if (it.key == day?.rerollFrom && day.rerollTo != null) TodayItem(day.rerollTo, ItemKind.LADDER, day.rerollTo, 0) else it }
        val recentLadders = quests.filter { it.epochDay in (today - 2) until today && it.kind == ItemKind.LADDER }.map { it.itemKey }.toSet()
        // nutrition: adaptive TDEE from food + weight logs, targets on the current trend weight
        val weightMap = weights.associate { it.epochDay to it.kg }
        val intake = foodLog.groupBy { it.epochDay }.mapValues { (_, l) -> l.sumOf { it.kcal.toDouble() }.toInt() }
        val formula = Nutrition.targets(s.sex, s.age, s.weightKg, s.heightCm, s.activity, s.goal, s.intensity, s.targetWeightKg, s.pregnant)
        val adaptive = Adaptive.estimate(formula.tdee, today, weightMap, intake)
        val kgNow = adaptive.trendKg ?: s.weightKg
        val nutrition = Nutrition.targets(s.sex, s.age, kgNow, s.heightCm, s.activity, s.goal, s.intensity, s.targetWeightKg, s.pregnant,
            tdeeOverride = if (adaptive.confidence > 0f) adaptive.tdee else null)
        AppState.Ready(
            s, player, slots, prog.associate { it.ladderId to it.toState() }, cardio?.c25kIndex ?: 0, prof.planStartEpochDay,
            nutrition, today, day, quests, days.sumOf { it.activeMin }, directive, type,
            canReroll = day?.rerollFrom == null, foods = foods, foodLog = foodLog, weights = weights, adaptive = adaptive,
            recentLadders = recentLadders,
            progress = mt.counters.let { k ->
                Progress(k.quests, k.clears, k.restDays, player.bestStreak, player.level, k.foodLogs, k.weighIns, k.proteinDays,
                    k.waterDays, k.activeMinutes, cardio?.c25kIndex ?: 0, prog.associate { it.ladderId to it.step }, k.legQuests)
            },
            achievements = mt.achievements.associateBy { it.id },
            cosmetics = mt.cosmetics ?: CosmeticsEntity()
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    private fun keyOf(kind: ItemKind, ladderId: String?) = when (kind) {
        ItemKind.LADDER -> ladderId!!
        ItemKind.C25K -> "c25k"
        ItemKind.WALK -> "walk"
    }

    init {
        // achievements are evaluated reactively: any change in progress can unlock something
        viewModelScope.launch {
            state.collect { st ->
                if (st is AppState.Ready) {
                    val new = Achievements.newlyUnlocked(st.progress, st.achievements.keys)
                    if (new.isNotEmpty()) repo.unlock(new.map { it.id })
                }
            }
        }
    }

    /** Result line of the last export / import, shown in the DATA panel. */
    var dataMessage by mutableStateOf<String?>(null)

    fun exportData(uri: android.net.Uri) = viewModelScope.launch {
        dataMessage = runCatching { dev.niklas.ironcore.data.Backup.export(getApplication(), IronDb.get(getApplication()), uri) }
            .fold({ "EXPORT COMPLETE // ${it.rows} RECORDS SECURED" }, { "EXPORT FAILED // ${it.message}" })
    }

    /** Replaces all data; [onDone] restarts the activity so restored settings (theme, sound) apply. */
    fun importData(uri: android.net.Uri, onDone: () -> Unit) = viewModelScope.launch {
        runCatching { dev.niklas.ironcore.data.Backup.import(getApplication(), IronDb.get(getApplication()), uri) }
            .onSuccess { dataMessage = "RESTORE COMPLETE // ${it.rows} RECORDS"; dev.niklas.ironcore.notify.Reminders.schedule(getApplication()); onDone() }
            .onFailure { dataMessage = "RESTORE FAILED // ${it.message}" }
    }

    fun markAchievementsSeen() = viewModelScope.launch { repo.markAchievementsSeen() }
    fun saveCosmetics(c: CosmeticsEntity) = viewModelScope.launch { repo.saveCosmetics(c) }
    fun setHandle(h: String) = viewModelScope.launch { if (h.isNotBlank()) repo.setHandle(h.trim().uppercase()) }
    fun setImage(uri: android.net.Uri, banner: Boolean) = viewModelScope.launch {
        val s = state.value as? AppState.Ready ?: return@launch
        val path = dev.niklas.ironcore.ui.profile.ImageStore.save(getApplication(), uri, banner)
        val c = s.cosmetics
        repo.saveCosmetics(if (banner) c.copy(bannerPath = path) else c.copy(avatarPath = path))
    }
    fun clearImage(banner: Boolean) = viewModelScope.launch {
        val s = state.value as? AppState.Ready ?: return@launch
        val c = s.cosmetics
        repo.saveCosmetics(if (banner) c.copy(bannerPath = null) else c.copy(avatarPath = null))
    }

    fun initialize(setup: Setup, plan: Plan) = viewModelScope.launch { repo.initialize(setup, plan) }
    fun wipe() = viewModelScope.launch { repo.wipe() }

    fun complete(item: TodayItem, rating: Rating, loadKg: Float? = null, minutes: Int = 0) = viewModelScope.launch {
        val s = state.value as? AppState.Ready ?: return@launch
        lastResult = repo.completeQuest(
            QuestDone(item.key, item.kind, item.ladderId, rating, loadKg, minutes, item.ladderId?.let { s.stateFor(it) }),
            s.setup.progContext, s.deloadWeek, s.directive.map { it.key }
        )
    }

    fun logFood(food: FoodEntity, grams: Float, save: Boolean) = viewModelScope.launch {
        val s = state.value as? AppState.Ready ?: return@launch
        repo.logFood(food, grams, save, s.nutrition.proteinG)?.let { lastResult = it }
    }
    fun deleteFoodLog(id: Long) = viewModelScope.launch { repo.deleteFoodLog(id) }
    fun deleteFood(id: Long) = viewModelScope.launch { repo.deleteFood(id) }
    fun addWater(ml: Int) = viewModelScope.launch {
        val s = state.value as? AppState.Ready ?: return@launch
        repo.addWater(ml, (s.nutrition.waterL * 1000).toInt())?.let { lastResult = it }
    }
    fun logWeight(kg: Float) = viewModelScope.launch { repo.logWeight(kg) }

    /** Daily reroll of one directive ladder quest. */
    fun reroll(item: TodayItem) = viewModelScope.launch {
        val s = state.value as? AppState.Ready ?: return@launch
        if (!s.canReroll || item.kind != ItemKind.LADDER) return@launch
        val to = PlanGenerator.rerollTarget(item.ladderId!!, s.setup.equipment, s.directive.mapNotNull { it.ladderId }.toSet(),
            s.slots.values.flatten().mapNotNull { it.ladderId }, s.recentLadders) ?: return@launch
        repo.reroll(item.key, to)
    }

    fun restCheckIn() = viewModelScope.launch { repo.restCheckIn()?.let { lastResult = it } }
    fun trainAnyway(on: Boolean) = viewModelScope.launch { repo.setTrainAnyway(on) }
}
