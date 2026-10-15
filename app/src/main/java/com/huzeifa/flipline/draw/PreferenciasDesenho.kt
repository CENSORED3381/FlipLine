package com.huzeifa.flipline.draw

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Preferências da Fase 2. Os valores são observáveis pelo Compose e persistidos
 * imediatamente, para que continuem iguais quando o app for fechado/reaberto.
 */
class PreferenciasDesenho(context: Context) {
    private val armazenamento = context.applicationContext.getSharedPreferences(
        NOME_PREFERENCIAS,
        Context.MODE_PRIVATE
    )

    private val estadoEstabilizacao = mutableStateOf(
        armazenamento.getBoolean(CHAVE_ESTABILIZACAO, true)
    )
    var estabilizacaoAtiva: Boolean
        get() = estadoEstabilizacao.value
        set(value) {
            estadoEstabilizacao.value = value
            armazenamento.edit().putBoolean(CHAVE_ESTABILIZACAO, value).apply()
        }

    private val estadoIntensidade = mutableFloatStateOf(
        armazenamento.getFloat(CHAVE_INTENSIDADE, 0.55f).coerceIn(0f, 1f)
    )
    var intensidadeEstabilizacao: Float
        get() = estadoIntensidade.floatValue
        set(value) {
            val segura = value.coerceIn(0f, 1f)
            estadoIntensidade.floatValue = segura
            armazenamento.edit().putFloat(CHAVE_INTENSIDADE, segura).apply()
        }

    private val estadoCursorDeslocado = mutableStateOf(
        armazenamento.getBoolean(CHAVE_CURSOR_DESLOCADO, false)
    )
    var cursorDeslocadoAtivo: Boolean
        get() = estadoCursorDeslocado.value
        set(value) {
            estadoCursorDeslocado.value = value
            armazenamento.edit().putBoolean(CHAVE_CURSOR_DESLOCADO, value).apply()
        }

    private val estadoDistanciaCursor = mutableFloatStateOf(
        armazenamento.getFloat(CHAVE_DISTANCIA_CURSOR, 64f).coerceIn(32f, 104f)
    )
    var distanciaCursorDp: Float
        get() = estadoDistanciaCursor.floatValue
        set(value) {
            val segura = value.coerceIn(32f, 104f)
            estadoDistanciaCursor.floatValue = segura
            armazenamento.edit().putFloat(CHAVE_DISTANCIA_CURSOR, segura).apply()
        }

    private val estadoAtalhos = mutableStateOf(
        armazenamento.getBoolean(CHAVE_ATALHOS_GESTOS, true)
    )
    var atalhosGestosAtivos: Boolean
        get() = estadoAtalhos.value
        set(value) {
            estadoAtalhos.value = value
            armazenamento.edit().putBoolean(CHAVE_ATALHOS_GESTOS, value).apply()
        }

    private val estadoRejeicaoPalma = mutableStateOf(
        armazenamento.getBoolean(CHAVE_REJEICAO_PALMA, true)
    )
    var rejeicaoPalmaAtiva: Boolean
        get() = estadoRejeicaoPalma.value
        set(value) {
            estadoRejeicaoPalma.value = value
            armazenamento.edit().putBoolean(CHAVE_REJEICAO_PALMA, value).apply()
        }

    private val estadoZoomGestual = mutableStateOf(
        armazenamento.getBoolean(CHAVE_ZOOM_GESTUAL, true)
    )
    var zoomGestualAtivo: Boolean
        get() = estadoZoomGestual.value
        set(value) {
            estadoZoomGestual.value = value
            armazenamento.edit().putBoolean(CHAVE_ZOOM_GESTUAL, value).apply()
        }

    fun reporPadroes() {
        estabilizacaoAtiva = true
        intensidadeEstabilizacao = 0.55f
        cursorDeslocadoAtivo = false
        distanciaCursorDp = 64f
        atalhosGestosAtivos = true
        rejeicaoPalmaAtiva = true
        zoomGestualAtivo = true
    }

    private companion object {
        const val NOME_PREFERENCIAS = "flipline_desenho_v2"
        const val CHAVE_ESTABILIZACAO = "estabilizacao_ativa"
        const val CHAVE_INTENSIDADE = "intensidade_estabilizacao"
        const val CHAVE_CURSOR_DESLOCADO = "cursor_deslocado_ativo"
        const val CHAVE_DISTANCIA_CURSOR = "distancia_cursor_dp"
        const val CHAVE_ATALHOS_GESTOS = "atalhos_gestos_ativos"
        const val CHAVE_REJEICAO_PALMA = "rejeicao_palma_ativa"
        const val CHAVE_ZOOM_GESTUAL = "zoom_gestual_ativo"
    }
}
