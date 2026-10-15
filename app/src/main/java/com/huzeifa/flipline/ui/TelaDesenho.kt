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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import com.huzeifa.flipline.draw.FerramentaDesenho
import com.huzeifa.flipline.draw.MotorDesenho
import com.huzeifa.flipline.draw.ModoMesclagem
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
    onVoltar: () -> Unit,
    onGuardarProjeto: () -> Unit,
    onAbrirProjeto: () -> Unit,
    onExportarPng: () -> Unit
) {
    val f = motor.ferramentas
    val estabilizacaoAtiva = preferencias.estabilizacaoAtiva
    val intensidadeEstabilizacao = preferencias.intensidadeEstabilizacao
    val cursorDeslocadoAtivo = preferencias.cursorDeslocadoAtivo
    val distanciaCursorDp = preferencias.distanciaCursorDp
    val atalhosGestosAtivos = preferencias.atalhosGestosAtivos
    val rejeicaoPalmaAtiva = preferencias.rejeicaoPalmaAtiva
    val zoomGestualAtivo = preferencias.zoomGestualAtivo
    var versao by remember { mutableIntStateOf(0) }
    var vista by remember { mutableStateOf<VistaDesenho?>(null) }
    var mostrarCamadas by remember { mutableStateOf(false) }

    DisposableEffect(motor) {
        motor.aoMudar = { versao++ }
        onDispose { motor.aoMudar = null }
    }

    val podeDesfazer = remember(versao) { motor.podeDesfazer() }
    val podeRefazer = remember(versao) { motor.podeRefazer() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onVoltar) { Text(stringResource(R.string.voltar)) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { motor.desfazer() }, enabled = podeDesfazer) { Text(stringResource(R.string.desfazer)) }
            TextButton(onClick = { motor.refazer() }, enabled = podeRefazer) { Text(stringResource(R.string.refazer)) }
        }

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { contexto ->
                VistaDesenho(contexto).also { it.motor = motor; vista = it }
            },
            update = { v ->
                v.borracha = f.borracha
                v.cor = f.cor
                v.larguraTraco = if (f.borracha) f.larguraBorracha else f.larguraPincel
                v.ferramenta = f.ferramenta
                v.gradeAtiva = f.gradeAtiva
                v.reguasAtivas = f.reguasAtivas
                v.encaixeAtivo = f.encaixeAtivo
                v.simetriaAtiva = f.simetriaAtiva
                v.assistenciaFormaAtiva = f.assistenciaFormaAtiva
                v.aoCapturarCor = { corCapturada -> f.cor = corCapturada }
                v.estabilizacaoAtiva = estabilizacaoAtiva
                v.intensidadeEstabilizacao = intensidadeEstabilizacao
                v.cursorDeslocadoAtivo = cursorDeslocadoAtivo
                v.distanciaCursorDp = distanciaCursorDp
                v.atalhosGestosAtivos = atalhosGestosAtivos
                v.rejeicaoPalmaAtiva = rejeicaoPalmaAtiva
                v.zoomGestualAtivo = zoomGestualAtivo
            }
        )

        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onGuardarProjeto, contentPadding = PaddingValues(horizontal = 7.dp)) { Text(stringResource(R.string.guardar)) }
                TextButton(onClick = onAbrirProjeto, contentPadding = PaddingValues(horizontal = 7.dp)) { Text(stringResource(R.string.abrir)) }
                TextButton(onClick = onExportarPng, contentPadding = PaddingValues(horizontal = 7.dp)) { Text(stringResource(R.string.exportar_png)) }
                TextButton(onClick = { mostrarCamadas = true }, contentPadding = PaddingValues(horizontal = 7.dp)) {
                    Text("Camadas (${motor.camadas.size})")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotaoFerramenta(stringResource(R.string.pincel), f.ferramenta == FerramentaDesenho.PINCEL, { f.ferramenta = FerramentaDesenho.PINCEL })
                BotaoFerramenta(stringResource(R.string.borracha), f.ferramenta == FerramentaDesenho.BORRACHA, { f.ferramenta = FerramentaDesenho.BORRACHA })
                BotaoFerramenta(stringResource(R.string.balde), f.ferramenta == FerramentaDesenho.BALDE, { f.ferramenta = FerramentaDesenho.BALDE })
                BotaoFerramenta(stringResource(R.string.conta_gotas), f.ferramenta == FerramentaDesenho.CONTA_GOTAS, { f.ferramenta = FerramentaDesenho.CONTA_GOTAS })
                BotaoFerramenta(stringResource(R.string.linha), f.ferramenta == FerramentaDesenho.LINHA, { f.ferramenta = FerramentaDesenho.LINHA })
                BotaoFerramenta(stringResource(R.string.retangulo), f.ferramenta == FerramentaDesenho.RETANGULO, { f.ferramenta = FerramentaDesenho.RETANGULO })
                BotaoFerramenta(stringResource(R.string.elipse), f.ferramenta == FerramentaDesenho.ELIPSE, { f.ferramenta = FerramentaDesenho.ELIPSE })
                BotaoFerramenta(stringResource(R.string.selecao), f.ferramenta == FerramentaDesenho.SELECAO, { f.ferramenta = FerramentaDesenho.SELECAO })
                BotaoFerramenta(stringResource(R.string.ajustar), false, { vista?.ajustarVista() })
            }

            if (!f.borracha && (f.ferramenta == FerramentaDesenho.PINCEL || f.ferramenta == FerramentaDesenho.LINHA || f.ferramenta == FerramentaDesenho.RETANGULO || f.ferramenta == FerramentaDesenho.ELIPSE)) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (c in paleta) {
                        val escolhida = c == f.cor
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(c)).border(
                                width = if (escolhida) 3.dp else 1.dp,
                                color = if (escolhida) MaterialTheme.colorScheme.primary else Color(0xFF6B6B6B),
                                shape = CircleShape
                            ).clickable { f.cor = c }
                        )
                    }
                }
            }

            val largura = if (f.borracha) f.larguraBorracha else f.larguraPincel
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${stringResource(R.string.espessura)}: ${largura.toInt()}", color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                if (f.ferramenta == FerramentaDesenho.BORRACHA) {
                    TextButton(onClick = { f.borracha = false }) { Text(stringResource(R.string.pincel)) }
                }
            }
            Slider(
                value = largura,
                onValueChange = { if (f.borracha) f.larguraBorracha = it else f.larguraPincel = it },
                valueRange = if (f.borracha) 4f..160f else 1f..80f
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniToggle(stringResource(R.string.grade), f.gradeAtiva) { f.gradeAtiva = it }
                MiniToggle(stringResource(R.string.reguas), f.reguasAtivas) { f.reguasAtivas = it }
                MiniToggle(stringResource(R.string.snap), f.encaixeAtivo) { f.encaixeAtivo = it }
                MiniToggle(stringResource(R.string.simetria), f.simetriaAtiva) { f.simetriaAtiva = it }
                MiniToggle(stringResource(R.string.forma_assistida), f.assistenciaFormaAtiva) { f.assistenciaFormaAtiva = it }
            }
        }
    }

    if (mostrarCamadas) {
        GestorCamadas(
            motor = motor,
            versao = versao,
            onFechar = { mostrarCamadas = false }
        )
    }
}

@Composable
private fun BotaoFerramenta(texto: String, selecionado: Boolean, onClick: () -> Unit) {
    val folga = PaddingValues(horizontal = 8.dp, vertical = 1.dp)
    if (selecionado) Button(onClick = onClick, contentPadding = folga) { Text(texto, maxLines = 1) }
    else OutlinedButton(onClick = onClick, contentPadding = folga) { Text(texto, maxLines = 1) }
}

@Composable
private fun MiniToggle(texto: String, valor: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { onChange(!valor) }.padding(end = 4.dp)) {
        Checkbox(checked = valor, onCheckedChange = null)
        Text(texto, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun GestorCamadas(motor: MotorDesenho, versao: Int, onFechar: () -> Unit) {
    // A versão no parâmetro força a atualização da lista após operações do motor.
    val ignorar = versao
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(stringResource(R.string.gestor_camadas)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.camadas_dica), style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { motor.adicionarCamada() },
                    enabled = motor.camadas.size < 12,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.nova_camada)) }
                motor.camadas.indices.reversed().forEach { indice ->
                    val camada = motor.camadas[indice]
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (indice == motor.indiceCamadaAtiva) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { motor.selecionarCamada(indice) }, modifier = Modifier.weight(1f)) {
                                Text(camada.nome, maxLines = 1)
                            }
                            TextButton(onClick = { motor.definirVisibilidade(indice, !camada.visivel) }) {
                                Text(if (camada.visivel) stringResource(R.string.ocultar) else stringResource(R.string.mostrar))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            TextButton(onClick = { motor.moverCamada(indice, -1) }, enabled = indice > 0) { Text("↓") }
                            TextButton(onClick = { motor.moverCamada(indice, 1) }, enabled = indice < motor.camadas.lastIndex) { Text("↑") }
                            TextButton(onClick = { motor.removerCamada(indice) }, enabled = motor.camadas.size > 1) { Text(stringResource(R.string.remover)) }
                        }
                        Text("${stringResource(R.string.opacidade)}: ${(camada.opacidade * 100 / 255)}%", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = camada.opacidade.toFloat(),
                            onValueChange = { motor.definirOpacidade(indice, it.toInt()) },
                            valueRange = 0f..255f
                        )
                        val proximoModo = when (camada.modoMesclagem) {
                            ModoMesclagem.NORMAL -> ModoMesclagem.MULTIPLICAR
                            ModoMesclagem.MULTIPLICAR -> ModoMesclagem.TELA
                            ModoMesclagem.TELA -> ModoMesclagem.NORMAL
                        }
                        TextButton(onClick = { motor.definirModoMesclagem(indice, proximoModo) }) {
                            Text("${stringResource(R.string.mesclagem)}: ${when (camada.modoMesclagem) {
                                ModoMesclagem.NORMAL -> stringResource(R.string.modo_normal)
                                ModoMesclagem.MULTIPLICAR -> stringResource(R.string.modo_multiplicar)
                                ModoMesclagem.TELA -> stringResource(R.string.modo_tela)
                            }}")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onFechar) { Text(stringResource(R.string.concluir)) } }
    )
}
