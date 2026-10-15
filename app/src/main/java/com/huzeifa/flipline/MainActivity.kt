package com.huzeifa.flipline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.huzeifa.flipline.ui.FliplineSplash
import com.huzeifa.flipline.ui.HomeScreen
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

@Composable
fun FliplineApp() {
    var mostrarSplash by rememberSaveable { mutableStateOf(true) }

    if (mostrarSplash) {
        FliplineSplash(onFinished = { mostrarSplash = false })
    } else {
        HomeScreen()
    }
}
