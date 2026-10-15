package com.huzeifa.flipline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.huzeifa.flipline.R
import com.huzeifa.flipline.draw.MotorDesenho
import com.huzeifa.flipline.draw.PreferenciasDesenho
import com.huzeifa.flipline.draw.VistaDesenho

private val paleta: List<Int> = listOf(
    0xFF000000L, 0xFFFFFFFFL, 0xFF8E8E8EL, 0xFFE53935L,
    0xFFFF6B35L, 0xFFFFC107L, 0xFF43A047L, 0xFF00ACC1L,
    0xFF1E88E5L, 0xFF5E35B1L, 0xFFD81B60L, 0xFF795548L
).map { it.toInt() }

@Composable
fun TelaDesenho(
    motor: MotorDesenho,
    preferencias: PreferenciasDesenho,
    onVoltar: () -> Unit
) {
    val f = motor.ferramentas
    // Ler as preferências durante a composição garante que o AndroidView seja
    // atualizado sempre que uma opção persistida mudar.
    val estabilizacaoAtiva = preferencias.estabilizacaoAtiva
    val intensidadeEstabilizacao = preferencias.intensidadeEstabilizacao
    val cursorDeslocadoAtivo = preferencias.cursorDeslocadoAtivo
    val distanciaCursorDp = preferencias.distanciaCursorDp
    val atalhosGestosAtivos = preferencias.atalhosGestosAtivos
    val rejeicaoPalmaAtiva = preferencias.rejeicaoPalmaAtiva
    val zoomGestualAtivo = preferencias.zoomGestualAtivo
    var versao by remember { mutableIntStateOf(0) }
    var vista by remember { mutableStateOf<VistaDesenho?>(null) }

    DisposableEffect(motor) {
        motor.aoMudar = { versao++ }
        onDispose { motor.aoMudar = null }
    }

    val podeDesfazer = remember(versao) { motor.podeDesfazer() }
    val podeRefazer = remember(versao) { motor.podeRefazer() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Barra de cima
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onVoltar) { Text(stringResource(R.string.voltar)) }
            Box(modifier = Modifier.weight(1f))
            TextButton(onClick = { motor.desfazer() }, enabled = podeDesfazer) {
                Text(stringResource(R.string.desfazer))
            }
            TextButton(onClick = { motor.refazer() }, enabled = podeRefazer) {
                Text(stringResource(R.string.refazer))
            }
        }

        // Área de desenho
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { contexto ->
                VistaDesenho(contexto).also {
                    it.motor = motor
                    vista = it
                }
            },
            update = { v ->
                v.borracha = f.borracha
                v.cor = f.cor
                v.larguraTraco = if (f.borracha) f.larguraBorracha else f.larguraPincel
                v.estabilizacaoAtiva = estabilizacaoAtiva
                v.intensidadeEstabilizacao = intensidadeEstabilizacao
                v.cursorDeslocadoAtivo = cursorDeslocadoAtivo
                v.distanciaCursorDp = distanciaCursorDp
                v.atalhosGestosAtivos = atalhosGestosAtivos
                v.rejeicaoPalmaAtiva = rejeicaoPalmaAtiva
                v.zoomGestualAtivo = zoomGestualAtivo
            }
        )

        // Painel de ferramentas
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotaoFerramenta(
                    texto = stringResource(R.string.pincel),
                    selecionado = !f.borracha,
                    onClick = { f.borracha = false },
                    modifier = Modifier.weight(1f)
                )
                BotaoFerramenta(
                    texto = stringResource(R.string.borracha),
                    selecionado = f.borracha,
                    onClick = { f.borracha = true },
                    modifier = Modifier.weight(1f)
                )
                BotaoFerramenta(
                    texto = stringResource(R.string.ajustar),
                    selecionado = false,
                    onClick = { vista?.ajustarVista() },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!f.borracha) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (c in paleta) {
                        val escolhida = c == f.cor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (escolhida) 3.dp else 1.dp,
                                    color = if (escolhida) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color(0xFF6B6B6B)
                                    },
                                    shape = CircleShape
                                )
                                .clickable { f.cor = c }
                        )
                    }
                }
            }

            val largura = if (f.borracha) f.larguraBorracha else f.larguraPincel
            Text(
                text = stringResource(R.string.espessura) + ": " + largura.toInt(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = largura,
                onValueChange = {
                    if (f.borracha) f.larguraBorracha = it else f.larguraPincel = it
                },
                valueRange = if (f.borracha) 4f..160f else 1f..80f
            )
        }
    }
}

@Composable
private fun BotaoFerramenta(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val folga = PaddingValues(horizontal = 4.dp)
    if (selecionado) {
        Button(onClick = onClick, modifier = modifier, contentPadding = folga) {
            Text(texto, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = folga) {
            Text(texto, maxLines = 1)
        }
    }
}
