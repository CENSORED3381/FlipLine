package com.huzeifa.flipline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.huzeifa.flipline.draw.MotorDesenho
import com.huzeifa.flipline.ui.FliplineSplash
import com.huzeifa.flipline.ui.HomeScreen
import com.huzeifa.flipline.ui.TelaDesenho
import com.huzeifa.flipline.ui.theme.FliplineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            FliplineTheme {
                FliplineApp()
            }
        }
    }
}

private enum class Tela { Splash, Home, Desenho }

@Composable
fun FliplineApp() {
    var tela by remember { mutableStateOf(Tela.Splash) }
    var motor by remember { mutableStateOf<MotorDesenho?>(null) }

    BackHandler(enabled = tela == Tela.Desenho) {
        tela = Tela.Home
    }

    val motorAtual = motor
    when {
        tela == Tela.Splash -> FliplineSplash(onFinished = { tela = Tela.Home })

        tela == Tela.Desenho && motorAtual != null -> TelaDesenho(
            motor = motorAtual,
            onVoltar = { tela = Tela.Home }
        )

        else -> HomeScreen(
            temDesenho = motorAtual != null && motorAtual.temConteudo(),
            onNovoDesenho = {
                motor = MotorDesenho()
                tela = Tela.Desenho
            },
            onContinuar = { tela = Tela.Desenho }
        )
    }
}
