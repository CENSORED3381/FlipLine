package com.huzeifa.flipline.draw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.max

/**
 * Área de desenho da Fase 2.
 * - 1 dedo: desenha com estabilização opcional e cursor deslocado opcional.
 * - 2 dedos em movimento: zoom, deslocamento e rotação (se ativados).
 * - toque rápido com 2 dedos: desfaz; com 3 dedos: refaz.
 * - contactos grandes podem ser rejeitados para evitar traços da palma.
 */
class VistaDesenho(context: Context) : View(context) {

    var motor: MotorDesenho? = null
        set(valor) {
            field = valor
            if (valor != null && width > 0 && !valor.vistaAjustada) ajustarVista()
            invalidate()
        }

    var borracha: Boolean = false
    var cor: Int = Color.BLACK
    var larguraTraco: Float = 8f

    var estabilizacaoAtiva: Boolean = true
    var intensidadeEstabilizacao: Float = 0.55f
    var cursorDeslocadoAtivo: Boolean = false
    var distanciaCursorDp: Float = 64f
    var atalhosGestosAtivos: Boolean = true
    var rejeicaoPalmaAtiva: Boolean = true
    var zoomGestualAtivo: Boolean = true

    private val densidade = resources.displayMetrics.density
    private val pincelFundo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pincelBitmap = Paint(Paint.FILTER_BITMAP_FLAG)
    private val pincelCursorExterior = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f * densidade
    }
    private val pincelCursorInterior = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * densidade
    }
    private val inversa = Matrix()
    private val ponto = FloatArray(2)
    private val pontoFiltrado = FloatArray(2)
    private val pontoDeslocado = FloatArray(2)
    private val valoresMatriz = FloatArray(9)

    // Traço em andamento.
    private var traco: Traco? = null
    private var renderizador: RenderizadorTraco? = null
    private var filtro: FiltroTraco? = null
    private var pontosDesenhados = 0
    private var inicioToque = 0L

    // Cursor do modo deslocado, sempre desenhado em coordenadas da vista.
    private var cursorVisivel = false
    private var cursorX = 0f
    private var cursorY = 0f

    // Estado dos gestos de múltiplos dedos.
    private var emGesto = false
    private var precisaBase = false
    private var ultimoCx = 0f
    private var ultimoCy = 0f
    private var ultimaDist = 0f
    private var ultimoAngulo = 0f
    private var inicioGestoToqueMs = 0L
    private var maximoPonteiros = 0
    private var gestoMoveu = false
    private val posicoesIniciais = HashMap<Int, Pair<Float, Float>>()
    private var bloquearInteracao = false

    override fun onSizeChanged(w: Int, h: Int, antigoW: Int, antigoH: Int) {
        super.onSizeChanged(w, h, antigoW, antigoH)
        val m = motor ?: return
        if (!m.vistaAjustada) ajustarVista()
    }

    /** Centraliza o papel e remove zoom e rotação. */
    fun ajustarVista() {
        val m = motor ?: return
        if (width == 0 || height == 0) return
        val escala = min(width.toFloat() / m.largura, height.toFloat() / m.altura) * 0.95f
        m.matrizVista.reset()
        m.matrizVista.postScale(escala, escala)
        m.matrizVista.postTranslate(
            (width - m.largura * escala) / 2f,
            (height - m.altura * escala) / 2f
        )
        m.vistaAjustada = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawColor(0xFF2B2B2B.toInt())
        val m = motor
        if (m != null) {
            canvas.save()
            canvas.concat(m.matrizVista)
            canvas.drawRect(0f, 0f, m.largura.toFloat(), m.altura.toFloat(), pincelFundo)
            canvas.drawBitmap(m.bitmap, 0f, 0f, pincelBitmap)
            canvas.restore()
        }
        if (cursorDeslocadoAtivo && cursorVisivel && !emGesto) desenharCursor(canvas)
        canvas.restore()
    }

    private fun desenharCursor(canvas: Canvas) {
        val raio = 7f * densidade
        canvas.drawCircle(cursorX, cursorY, raio, pincelCursorExterior)
        canvas.drawCircle(cursorX, cursorY, raio, pincelCursorInterior)
        val folga = 10f * densidade
        val tamanho = 3f * densidade
        canvas.drawLine(cursorX - folga, cursorY, cursorX - tamanho, cursorY, pincelCursorExterior)
        canvas.drawLine(cursorX + tamanho, cursorY, cursorX + folga, cursorY, pincelCursorExterior)
        canvas.drawLine(cursorX, cursorY - folga, cursorX, cursorY - tamanho, pincelCursorExterior)
        canvas.drawLine(cursorX, cursorY + tamanho, cursorX, cursorY + folga, pincelCursorExterior)
        canvas.drawLine(cursorX - folga, cursorY, cursorX - tamanho, cursorY, pincelCursorInterior)
        canvas.drawLine(cursorX + tamanho, cursorY, cursorX + folga, cursorY, pincelCursorInterior)
        canvas.drawLine(cursorX, cursorY - folga, cursorX, cursorY - tamanho, pincelCursorInterior)
        canvas.drawLine(cursorX, cursorY + tamanho, cursorX, cursorY + folga, pincelCursorInterior)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val m = motor ?: return false

        if (bloquearInteracao) {
            if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) {
                bloquearInteracao = false
                limparEstadoGesto()
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            return true
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                limparEstadoGesto()
                if (rejeicaoPalmaAtiva && contactoParecePalma(e, 0)) {
                    bloquearInteracao = true
                    esconderCursor()
                    return true
                }
                iniciarTraco(m, e)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (rejeicaoPalmaAtiva && contactoGrandePresente(e)) {
                    cancelarTraco(m)
                    bloquearInteracao = true
                    limparEstadoGesto()
                    esconderCursor()
                    return true
                }
                if (!emGesto) {
                    cancelarTraco(m)
                    emGesto = true
                    inicioGestoToqueMs = e.eventTime
                    maximoPonteiros = e.pointerCount
                    gestoMoveu = false
                    posicoesIniciais.clear()
                    guardarPosicoesIniciais(e)
                } else {
                    maximoPonteiros = max(maximoPonteiros, e.pointerCount)
                    guardarPosicoesIniciais(e)
                }
                cursorVisivel = false
                precisaBase = true
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (emGesto) {
                    atualizarMovimentoDoGesto(e)
                    if (zoomGestualAtivo && e.pointerCount == 2) {
                        atualizarGesto(m, e)
                    } else if (e.pointerCount != 2) {
                        precisaBase = true
                    }
                } else {
                    continuarTraco(m, e)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (emGesto) {
                    atualizarMovimentoDoGesto(e)
                    // Os IDs de ponteiro podem ser reutilizados depois de um UP;
                    // remove a posição antiga para não contaminar um novo contacto.
                    posicoesIniciais.remove(e.getPointerId(e.actionIndex))
                    precisaBase = true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (emGesto) {
                    atualizarMovimentoDoGesto(e)
                    executarAtalhoSeToqueRapido(m, e.eventTime)
                    limparEstadoGesto()
                } else {
                    finalizarTraco(m, e)
                }
                esconderCursor()
                parent?.requestDisallowInterceptTouchEvent(false)
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelarTraco(m)
                limparEstadoGesto()
                esconderCursor()
                bloquearInteracao = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    // ---------- Desenho e filtragem ----------

    private fun iniciarTraco(m: MotorDesenho, e: MotionEvent) {
        val novo = Traco(cor, larguraTraco.coerceAtLeast(1f), borracha)
        traco = novo
        renderizador = null
        pontosDesenhados = 0
        inicioToque = e.eventTime
        filtro = if (estabilizacaoAtiva) FiltroTraco(intensidadeEstabilizacao) else null
        filtro?.iniciar(e.x, e.y, e.eventTime)
        adicionarAmostra(m, novo, e.x, e.y, e.eventTime)
        invalidate()
    }

    private fun continuarTraco(m: MotorDesenho, e: MotionEvent) {
        val atual = traco ?: return
        for (i in 0 until e.historySize) {
            adicionarAmostra(
                m,
                atual,
                e.getHistoricalX(i),
                e.getHistoricalY(i),
                e.getHistoricalEventTime(i)
            )
        }
        adicionarAmostra(m, atual, e.x, e.y, e.eventTime)
        if (renderizador != null || e.eventTime - inicioToque >= ATRASO_INICIO_MS) desenharPendentes(m)
        invalidate()
    }

    private fun adicionarAmostra(m: MotorDesenho, t: Traco, xCru: Float, yCru: Float, tempoMs: Long) {
        val f = filtro
        val x: Float
        val y: Float
        if (estabilizacaoAtiva && f != null) {
            f.filtrar(xCru, yCru, tempoMs, pontoFiltrado)
            x = pontoFiltrado[0]
            y = pontoFiltrado[1]
        } else {
            x = xCru
            y = yCru
        }
        aplicarDeslocamentoCursor(x, y, pontoDeslocado)
        adicionarPontoNaTela(m, t, pontoDeslocado[0], pontoDeslocado[1])
        if (cursorDeslocadoAtivo) {
            cursorX = pontoDeslocado[0]
            cursorY = pontoDeslocado[1]
            cursorVisivel = true
        }
    }

    private fun aplicarDeslocamentoCursor(x: Float, y: Float, destino: FloatArray) {
        if (!cursorDeslocadoAtivo || width <= 0 || height <= 0) {
            destino[0] = x
            destino[1] = y
            return
        }
        val deslocamentoX = 22f * densidade
        val deslocamentoY = distanciaCursorDp.coerceIn(32f, 104f) * densidade
        destino[0] = (x + deslocamentoX).coerceIn(0f, width.toFloat())
        destino[1] = (y - deslocamentoY).coerceIn(0f, height.toFloat())
    }

    private fun adicionarPontoNaTela(m: MotorDesenho, t: Traco, x: Float, y: Float) {
        if (!m.matrizVista.invert(inversa)) return
        ponto[0] = x
        ponto[1] = y
        inversa.mapPoints(ponto)
        val n = t.pontos.tamanho
        if (n > 0) {
            val dx = ponto[0] - t.pontos.x(n - 1)
            val dy = ponto[1] - t.pontos.y(n - 1)
            if (dx * dx + dy * dy < 0.09f) return
        }
        t.pontos.adicionar(ponto[0], ponto[1])
    }

    private fun desenharPendentes(m: MotorDesenho) {
        val t = traco ?: return
        var r = renderizador
        if (r == null) {
            if (t.pontos.tamanho == 0) return
            r = RenderizadorTraco(m.tela, t)
            renderizador = r
            r.iniciar(t.pontos.x(0), t.pontos.y(0))
            pontosDesenhados = 1
        }
        while (pontosDesenhados < t.pontos.tamanho) {
            r.adicionar(t.pontos.x(pontosDesenhados), t.pontos.y(pontosDesenhados))
            pontosDesenhados++
        }
    }

    private fun finalizarTraco(m: MotorDesenho, e: MotionEvent) {
        val t = traco ?: return
        // Inclui a amostra final, que pode ser ligeiramente posterior ao último MOVE.
        adicionarAmostra(m, t, e.x, e.y, e.eventTime)
        desenharPendentes(m)
        renderizador?.finalizar()
        m.registrar(t)
        traco = null
        renderizador = null
        filtro = null
        invalidate()
    }

    private fun cancelarTraco(m: MotorDesenho) {
        if (traco == null) return
        val jaDesenhou = renderizador != null
        traco = null
        renderizador = null
        filtro = null
        if (jaDesenhou) m.reconstruir()
        invalidate()
    }

    private fun esconderCursor() {
        cursorVisivel = false
        invalidate()
    }

    // ---------- Gestos de dois dedos ----------

    private fun guardarPosicoesIniciais(e: MotionEvent) {
        for (i in 0 until e.pointerCount) {
            val id = e.getPointerId(i)
            if (!posicoesIniciais.containsKey(id)) {
                posicoesIniciais[id] = Pair(e.getX(i), e.getY(i))
            }
        }
    }

    private fun atualizarMovimentoDoGesto(e: MotionEvent) {
        if (gestoMoveu) return
        val limite = LIMIAR_MOVIMENTO_TOQUE_DP * densidade
        val limite2 = limite * limite
        for (i in 0 until e.pointerCount) {
            val inicial = posicoesIniciais[e.getPointerId(i)] ?: continue
            val dx = e.getX(i) - inicial.first
            val dy = e.getY(i) - inicial.second
            if (dx * dx + dy * dy > limite2) {
                gestoMoveu = true
                return
            }
        }
    }

    private fun lerBase(e: MotionEvent) {
        if (e.pointerCount < 2) return
        val dx = e.getX(1) - e.getX(0)
        val dy = e.getY(1) - e.getY(0)
        ultimoCx = (e.getX(0) + e.getX(1)) / 2f
        ultimoCy = (e.getY(0) + e.getY(1)) / 2f
        ultimaDist = hypot(dx, dy)
        ultimoAngulo = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    }

    private fun atualizarGesto(m: MotorDesenho, e: MotionEvent) {
        if (precisaBase) {
            lerBase(e)
            precisaBase = false
            return
        }
        val dx = e.getX(1) - e.getX(0)
        val dy = e.getY(1) - e.getY(0)
        val novoCx = (e.getX(0) + e.getX(1)) / 2f
        val novoCy = (e.getY(0) + e.getY(1)) / 2f
        val novaDist = hypot(dx, dy)
        val novoAngulo = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

        if (ultimaDist > 1f && novaDist > 1f) {
            val atual = escalaAtual(m.matrizVista).coerceAtLeast(0.0001f)
            var fator = novaDist / ultimaDist
            val alvo = (atual * fator).coerceIn(ESCALA_MIN, ESCALA_MAX)
            fator = alvo / atual

            var giro = novoAngulo - ultimoAngulo
            while (giro > 180f) giro -= 360f
            while (giro < -180f) giro += 360f

            m.matrizVista.postScale(fator, fator, ultimoCx, ultimoCy)
            m.matrizVista.postRotate(giro, ultimoCx, ultimoCy)
            m.matrizVista.postTranslate(novoCx - ultimoCx, novoCy - ultimoCy)
        }
        ultimoCx = novoCx
        ultimoCy = novoCy
        ultimaDist = novaDist
        ultimoAngulo = novoAngulo
        invalidate()
    }

    private fun executarAtalhoSeToqueRapido(m: MotorDesenho, fimMs: Long) {
        if (!atalhosGestosAtivos || gestoMoveu) return
        val duracao = fimMs - inicioGestoToqueMs
        if (duracao < 0L || duracao > DURACAO_MAX_TOQUE_MS) return
        when (maximoPonteiros) {
            2 -> m.desfazer()
            3 -> m.refazer()
        }
    }

    private fun limparEstadoGesto() {
        emGesto = false
        precisaBase = false
        inicioGestoToqueMs = 0L
        maximoPonteiros = 0
        gestoMoveu = false
        posicoesIniciais.clear()
        ultimaDist = 0f
        ultimoAngulo = 0f
    }

    // ---------- Rejeição conservadora da palma ----------

    private fun contactoParecePalma(e: MotionEvent, indice: Int): Boolean {
        if (indice !in 0 until e.pointerCount) return false
        val larguraDp = e.getTouchMajor(indice) / densidade
        val alturaDp = e.getTouchMinor(indice) / densidade
        // Valores zero/indisponíveis são comuns em alguns aparelhos: nesses casos não rejeitamos.
        if (larguraDp <= 0f || alturaDp <= 0f) return false
        return larguraDp >= LIMIAR_PALMA_LARGO_DP ||
            (larguraDp >= LIMIAR_PALMA_DUPLA_DP && alturaDp >= LIMIAR_PALMA_ALTO_DP)
    }

    private fun contactoGrandePresente(e: MotionEvent): Boolean {
        for (i in 0 until e.pointerCount) {
            if (contactoParecePalma(e, i)) return true
        }
        return false
    }

    private fun escalaAtual(matriz: Matrix): Float {
        matriz.getValues(valoresMatriz)
        return hypot(valoresMatriz[Matrix.MSCALE_X], valoresMatriz[Matrix.MSKEW_Y])
    }

    private companion object {
        const val ATRASO_INICIO_MS = 40L
        const val DURACAO_MAX_TOQUE_MS = 450L
        const val LIMIAR_MOVIMENTO_TOQUE_DP = 14f
        const val LIMIAR_PALMA_LARGO_DP = 72f
        const val LIMIAR_PALMA_DUPLA_DP = 48f
        const val LIMIAR_PALMA_ALTO_DP = 34f
        const val ESCALA_MIN = 0.05f
        const val ESCALA_MAX = 30f
    }
}
