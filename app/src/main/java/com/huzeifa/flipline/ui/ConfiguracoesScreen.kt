package com.huzeifa.flipline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huzeifa.flipline.R
import com.huzeifa.flipline.draw.PreferenciasDesenho
import java.util.Locale

@Composable
fun ConfiguracoesScreen(
    preferencias: PreferenciasDesenho,
    onVoltar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onVoltar) { Text(stringResource(R.string.voltar)) }
            Text(
                text = stringResource(R.string.configuracoes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.fase2_titulo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.fase2_descricao),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            LinhaPreferencia(
                titulo = stringResource(R.string.estabilizacao_titulo),
                descricao = stringResource(R.string.estabilizacao_descricao),
                selecionado = preferencias.estabilizacaoAtiva,
                aoMudar = { preferencias.estabilizacaoAtiva = it }
            )
            if (preferencias.estabilizacaoAtiva) {
                Text(
                    text = stringResource(R.string.intensidade_estabilizacao) + ": " +
                        String.format(Locale.getDefault(), "%.0f%%", preferencias.intensidadeEstabilizacao * 100f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = preferencias.intensidadeEstabilizacao,
                    onValueChange = { preferencias.intensidadeEstabilizacao = it },
                    valueRange = 0f..1f
                )
                Text(
                    text = stringResource(R.string.intensidade_ajuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LinhaPreferencia(
                titulo = stringResource(R.string.cursor_deslocado_titulo),
                descricao = stringResource(R.string.cursor_deslocado_descricao),
                selecionado = preferencias.cursorDeslocadoAtivo,
                aoMudar = { preferencias.cursorDeslocadoAtivo = it }
            )
            if (preferencias.cursorDeslocadoAtivo) {
                Text(
                    text = stringResource(R.string.distancia_cursor) + ": " +
                        preferencias.distanciaCursorDp.toInt() + " dp",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = preferencias.distanciaCursorDp,
                    onValueChange = { preferencias.distanciaCursorDp = it },
                    valueRange = 32f..104f
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LinhaPreferencia(
                titulo = stringResource(R.string.atalhos_gestos_titulo),
                descricao = stringResource(R.string.atalhos_gestos_descricao),
                selecionado = preferencias.atalhosGestosAtivos,
                aoMudar = { preferencias.atalhosGestosAtivos = it }
            )
            LinhaPreferencia(
                titulo = stringResource(R.string.rejeicao_palma_titulo),
                descricao = stringResource(R.string.rejeicao_palma_descricao),
                selecionado = preferencias.rejeicaoPalmaAtiva,
                aoMudar = { preferencias.rejeicaoPalmaAtiva = it }
            )
            LinhaPreferencia(
                titulo = stringResource(R.string.zoom_gestual_titulo),
                descricao = stringResource(R.string.zoom_gestual_descricao),
                selecionado = preferencias.zoomGestualAtivo,
                aoMudar = { preferencias.zoomGestualAtivo = it }
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.gestos_dica),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { preferencias.reporPadroes() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.repor_configuracoes))
            }
        }
    }
}

@Composable
private fun LinhaPreferencia(
    titulo: String,
    descricao: String,
    selecionado: Boolean,
    aoMudar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = descricao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = selecionado, onCheckedChange = aoMudar)
    }
}
