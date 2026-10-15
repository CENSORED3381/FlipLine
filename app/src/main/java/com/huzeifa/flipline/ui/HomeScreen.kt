package com.huzeifa.flipline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huzeifa.flipline.R

@Composable
fun HomeScreen(
    temDesenho: Boolean,
    onNovoDesenho: () -> Unit,
    onContinuar: () -> Unit,
    onConfiguracoes: () -> Unit,
    onAbrirProjeto: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.slogan),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (temDesenho) {
            Button(onClick = onContinuar) {
                Text(stringResource(R.string.continuar_desenho))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onNovoDesenho) {
                Text(stringResource(R.string.novo_desenho))
            }
        } else {
            Button(onClick = onNovoDesenho) {
                Text(stringResource(R.string.novo_desenho))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onAbrirProjeto) {
            Text(stringResource(R.string.meus_projetos))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onConfiguracoes) {
            Text(stringResource(R.string.configuracoes))
        }

    }
}
