package com.huzeifa.flipline.draw

import kotlin.math.abs
import kotlin.math.PI

/**
 * Filtro One Euro bidimensional para reduzir tremores sem introduzir
 * um atraso fixo elevado. O tempo recebido é o eventTime do MotionEvent (ms).
 */
class FiltroTraco(intensidade: Float) {
    private val intensidade = intensidade.coerceIn(0f, 1f)
    private val corteMinimo = 3.0f - 2.35f * this.intensidade
    private val beta = 0.025f + 0.15f * this.intensidade
    private val corteDerivada = 1.0f

    private var inicializado = false
    private var ultimoTempo = 0L
    private var xCru = 0f
    private var yCru = 0f
    private var xFiltrado = 0f
    private var yFiltrado = 0f
    private var derivadaX = 0f
    private var derivadaY = 0f

    fun iniciar(x: Float, y: Float, tempoMs: Long) {
        xCru = x
        yCru = y
        xFiltrado = x
        yFiltrado = y
        derivadaX = 0f
        derivadaY = 0f
        ultimoTempo = tempoMs
        inicializado = true
    }

    /** Escreve o ponto filtrado no array fornecido para evitar alocações por amostra. */
    fun filtrar(x: Float, y: Float, tempoMs: Long, destino: FloatArray) {
        if (!inicializado) {
            iniciar(x, y, tempoMs)
            destino[0] = x
            destino[1] = y
            return
        }

        // Eventos com o mesmo timestamp podem ocorrer em dispositivos diferentes.
        val deltaMs = (tempoMs - ultimoTempo).coerceAtLeast(1L)
        val dt = (deltaMs / 1000f).coerceIn(1f / 240f, 0.1f)

        val derivadaCruaX = (x - xCru) / dt
        val derivadaCruaY = (y - yCru) / dt
        val alphaDerivada = alpha(corteDerivada, dt)
        derivadaX = lowPass(derivadaX, derivadaCruaX, alphaDerivada)
        derivadaY = lowPass(derivadaY, derivadaCruaY, alphaDerivada)

        val corteX = corteMinimo + beta * abs(derivadaX)
        val corteY = corteMinimo + beta * abs(derivadaY)
        xFiltrado = lowPass(xFiltrado, x, alpha(corteX, dt))
        yFiltrado = lowPass(yFiltrado, y, alpha(corteY, dt))

        xCru = x
        yCru = y
        ultimoTempo = tempoMs
        destino[0] = xFiltrado
        destino[1] = yFiltrado
    }

    private fun alpha(corte: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * corte.coerceAtLeast(0.01f))
        return 1f / (1f + tau / dt)
    }

    private fun lowPass(anterior: Float, atual: Float, alpha: Float): Float =
        alpha * atual + (1f - alpha) * anterior
}
