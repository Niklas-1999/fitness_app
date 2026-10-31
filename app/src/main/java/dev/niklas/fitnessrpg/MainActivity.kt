package dev.niklas.fitnessrpg

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.niklas.fitnessrpg.audio.Fx
import dev.niklas.fitnessrpg.audio.LocalFx
import dev.niklas.fitnessrpg.ui.boot.BootScreen
import dev.niklas.fitnessrpg.ui.demo.HudDemo
import dev.niklas.fitnessrpg.ui.fx.CyberBackdrop
import dev.niklas.fitnessrpg.ui.theme.CyberTheme
import dev.niklas.fitnessrpg.ui.theme.ThemeState

class MainActivity : ComponentActivity() {
    private lateinit var fx: Fx

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        fx = Fx(this).also { it.view = window.decorView }
        val theme = ThemeState(this)
        setContent {
            CompositionLocalProvider(LocalFx provides fx) {
                CyberTheme(theme) {
                    var booted by rememberSaveable { mutableStateOf(false) }
                    CyberBackdrop {
                        AnimatedVisibility(booted, enter = fadeIn(tween(600))) { HudDemo() }
                        if (!booted) BootScreen("FITNESS_RPG", "NEURAL TRAINING TERMINAL // BUILD 0.1") { booted = true }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        fx.release()
        super.onDestroy()
    }
}
