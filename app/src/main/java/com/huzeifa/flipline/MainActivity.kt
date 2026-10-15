package com.huzeifa.flipline

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.huzeifa.flipline.draw.MotorDesenho
import com.huzeifa.flipline.draw.PreferenciasDesenho
import com.huzeifa.flipline.draw.ProjetosFlipline
import com.huzeifa.flipline.ui.ConfiguracoesScreen
import com.huzeifa.flipline.ui.FliplineSplash
import com.huzeifa.flipline.ui.HomeScreen
import com.huzeifa.flipline.ui.TelaDesenho
import com.huzeifa.flipline.ui.theme.FliplineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { FliplineTheme { FliplineApp() } }
    }
}

private enum class Tela { Splash, Home, Desenho, Configuracoes }

@Composable
fun FliplineApp() {
    val contexto = LocalContext.current
    val preferencias = remember { PreferenciasDesenho(contexto) }
    val scope = rememberCoroutineScope()
    var tela by remember { mutableStateOf(Tela.Splash) }
    var motor by remember { mutableStateOf<MotorDesenho?>(null) }

    val guardarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val alvo = motor
        if (uri != null && alvo != null) {
            val camadas = alvo.criarSnapshotCamadas()
            scope.launch {
                val resultado = runCatching {
                    withContext(Dispatchers.IO) {
                        ProjetosFlipline.salvarProjeto(contexto, uri, alvo.largura, alvo.altura, camadas)
                    }
                }
                camadas.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
                resultado.onSuccess {
                    Toast.makeText(contexto, "Projeto FlipLine guardado.", Toast.LENGTH_SHORT).show()
                }.onFailure { erro ->
                    Toast.makeText(contexto, "Falha ao guardar: ${erro.message ?: "erro desconhecido"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val exportarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val alvo = motor
        if (uri != null && alvo != null) {
            val imagem = alvo.criarSnapshotComposto()
            scope.launch {
                val resultado = runCatching { withContext(Dispatchers.IO) { ProjetosFlipline.exportarPng(contexto, uri, imagem) } }
                if (!imagem.isRecycled) imagem.recycle()
                resultado.onSuccess {
                    Toast.makeText(contexto, "PNG exportado com transparência.", Toast.LENGTH_SHORT).show()
                }.onFailure { erro ->
                    Toast.makeText(contexto, "Falha ao exportar PNG: ${erro.message ?: "erro desconhecido"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val abrirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val resultado = runCatching { withContext(Dispatchers.IO) { ProjetosFlipline.abrirProjeto(contexto, uri) } }
                resultado.onSuccess { dados ->
                    val alvo = motor ?: MotorDesenho()
                    var transferido = false
                    try {
                        alvo.substituirCamadas(dados)
                        transferido = true // o motor passa a ser dono dos bitmaps carregados
                        motor = alvo
                        tela = Tela.Desenho
                        Toast.makeText(contexto, "Projeto FlipLine aberto.", Toast.LENGTH_SHORT).show()
                    } catch (erro: Exception) {
                        Toast.makeText(contexto, "Não foi possível carregar o projeto: ${erro.message ?: "erro desconhecido"}", Toast.LENGTH_LONG).show()
                    } finally {
                        if (!transferido) dados.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
                    }
                }.onFailure { erro ->
                    Toast.makeText(contexto, "Ficheiro inválido ou danificado: ${erro.message ?: "erro desconhecido"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    BackHandler(enabled = tela == Tela.Desenho || tela == Tela.Configuracoes) { tela = Tela.Home }

    val motorAtual = motor
    when {
        tela == Tela.Splash -> FliplineSplash(onFinished = { tela = Tela.Home })
        tela == Tela.Desenho && motorAtual != null -> TelaDesenho(
            motor = motorAtual,
            preferencias = preferencias,
            onVoltar = { tela = Tela.Home },
            onGuardarProjeto = { guardarLauncher.launch("Meu desenho.flipline") },
            onAbrirProjeto = { abrirLauncher.launch(arrayOf("*/*")) },
            onExportarPng = { exportarLauncher.launch("Meu desenho.png") }
        )
        tela == Tela.Configuracoes -> ConfiguracoesScreen(preferencias = preferencias, onVoltar = { tela = Tela.Home })
        else -> HomeScreen(
            temDesenho = motorAtual != null && motorAtual.temConteudo(),
            onNovoDesenho = { motor = MotorDesenho(); tela = Tela.Desenho },
            onContinuar = { tela = Tela.Desenho },
            onConfiguracoes = { tela = Tela.Configuracoes },
            onAbrirProjeto = { abrirLauncher.launch(arrayOf("*/*")) }
        )
    }
}
