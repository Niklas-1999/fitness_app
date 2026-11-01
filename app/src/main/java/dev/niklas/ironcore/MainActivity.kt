package dev.niklas.ironcore

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.niklas.ironcore.audio.Fx
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.ui.boot.BootScreen
import dev.niklas.ironcore.ui.fx.CyberBackdrop
import dev.niklas.ironcore.ui.main.MainActions
import dev.niklas.ironcore.ui.main.MainScreen
import dev.niklas.ironcore.ui.onboarding.OnboardingFlow
import dev.niklas.ironcore.ui.onboarding.OnboardingVM
import dev.niklas.ironcore.ui.theme.CyberTheme
import dev.niklas.ironcore.ui.theme.ThemeState

class MainActivity : ComponentActivity() {
    private lateinit var fx: Fx
    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        fx = Fx(this).also { it.view = window.decorView }
        dev.niklas.ironcore.notify.Reminders.schedule(this)
        val theme = ThemeState(this)
        setContent {
            CompositionLocalProvider(LocalFx provides fx) {
                CyberTheme(theme) {
                    var booted by rememberSaveable { mutableStateOf(false) }
                    val state by app.state.collectAsStateWithLifecycle()
                    CyberBackdrop {
                        if (!booted) BootScreen("IRONCORE", "NEURAL TRAINING TERMINAL // BUILD 0.2") { booted = true }
                        else AnimatedContent(
                            state::class,
                            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(300)) },
                            label = "root"
                        ) { kind ->
                            when (kind) {
                                AppState.NeedsSetup::class -> {
                                    val vm: OnboardingVM = viewModel()
                                    OnboardingFlow(vm, onRestore = { app.importData(it) { recreate() } }, restoreMessage = app.dataMessage) { setup, plan -> app.initialize(setup, plan) }
                                }
                                AppState.Ready::class -> (state as? AppState.Ready)?.let {
                                    MainScreen(it, app.lastResult, { app.lastResult = null }, MainActions(
                                        complete = { item, r, load, min -> app.complete(item, r, load, min) },
                                        reroll = { app.reroll(it) },
                                        fuel = dev.niklas.ironcore.ui.fuel.FuelActions({}, { app.deleteFoodLog(it) }, { app.addWater(it) }, { app.logWeight(it) }),
                                        logFood = { f, g, save -> app.logFood(f, g, save) },
                                        deleteFood = { app.deleteFood(it) },
                                        restCheckIn = { app.restCheckIn() },
                                        trainAnyway = { on -> app.trainAnyway(on) },
                                        reinitialize = { app.wipe() },
                                        profile = { dbg -> dev.niklas.ironcore.ui.profile.ProfileActions(
                                            saveCosmetics = { app.saveCosmetics(it) }, setHandle = { app.setHandle(it) },
                                            setImage = { u, b -> app.setImage(u, b) }, clearImage = { app.clearImage(it) },
                                            reinitialize = { app.wipe() }, debugRun = dbg,
                                            exportData = { app.exportData(it) }, importData = { app.importData(it) { recreate() } },
                                            dataMessage = app.dataMessage
                                        ) },
                                        achievementsSeen = { app.markAchievementsSeen() }
                                    ))
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.refreshDay()   // day rollover while the app was in the background
    }

    override fun onDestroy() {
        fx.release()
        super.onDestroy()
    }
}
