package dev.niklas.ironcore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.niklas.ironcore.data.IronDb
import dev.niklas.ironcore.data.PlanSlotEntity
import dev.niklas.ironcore.data.PlayerEntity
import dev.niklas.ironcore.data.Repo
import dev.niklas.ironcore.domain.Forecast
import dev.niklas.ironcore.domain.Nutrition
import dev.niklas.ironcore.domain.NutritionTargets
import dev.niklas.ironcore.domain.Plan
import dev.niklas.ironcore.domain.ProgState
import dev.niklas.ironcore.domain.Setup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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
        val nutrition: NutritionTargets
    ) : AppState {
        val today: LocalDate get() = LocalDate.now()
        val todayDow get() = today.dayOfWeek.value
        val week get() = ((today.toEpochDay() - planStartEpochDay) / 7 + 1).toInt()
        val deloadWeek get() = week % Forecast.DELOAD_EVERY_WEEKS == 0
        val weeksToDeload get() = Forecast.DELOAD_EVERY_WEEKS - (week % Forecast.DELOAD_EVERY_WEEKS)
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repo(IronDb.get(app).dao())

    val state: StateFlow<AppState> = combine(repo.profile, repo.player, repo.plan, repo.progression, repo.cardio) { prof, player, plan, prog, cardio ->
        if (prof == null || player == null) AppState.NeedsSetup
        else {
            val s = prof.toSetup()
            AppState.Ready(
                s, player, plan.groupBy { it.dayOfWeek }, prog.associate { it.ladderId to it.toState() },
                cardio?.c25kIndex ?: 0, prof.planStartEpochDay,
                Nutrition.targets(s.sex, s.age, s.weightKg, s.heightCm, s.activity, s.goal, s.intensity, s.targetWeightKg, s.pregnant)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    fun initialize(setup: Setup, plan: Plan) = viewModelScope.launch { repo.initialize(setup, plan) }
    fun wipe() = viewModelScope.launch { repo.wipe() }
}
