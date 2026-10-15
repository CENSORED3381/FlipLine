package com.huzeifa.flipline.draw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Canvas raster com desenho multitoque, ferramentas de forma, balde, conta-gotas e seleção. */
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
    var ferramenta: FerramentaDesenho = FerramentaDesenho.PINCEL
    var gradeAtiva = false
    var reguasAtivas = false
    var encaixeAtivo = false
    var simetriaAtiva = false
    var assistenciaFormaAtiva = true
    var aoCapturarCor: ((Int) -> Unit)? = null

    var estabilizacaoAtiva = true
    var intensidadeEstabilizacao: Float = 0.55f
    var cursorDeslocadoAtivo: Boolean = false
    var distanciaCursorDp: Float = 64f
    var atalhosGestosAtivos: Boolean = true
    var rejeicaoPalmaAtiva: Boolean = true
    var zoomGestualAtivo: Boolean = true

    private val densidade = resources.displayMetrics.density
    private val pincelFundo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pincelBitmap = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pincelGuia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x40757575
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val pincelTextoGuia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB06B6B6B.toInt(); textSize = 20f; style = Paint.Style.FILL
    }
    private val pincelSimetria = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAA00ACC1.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 7f), 0f)
    }
    private val pincelSelecao = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF6B35.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(9f, 6f), 0f)
    }
    private val pincelPrevia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val pincelCursorExterior = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f * densidade
    }
    private val pincelCursorInterior = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 1.3f * densidade
    }
    private val inversa = Matrix()
    private val ponto = FloatArray(2)
    private val pontoFiltrado = FloatArray(2)
    private val pontoDeslocado = FloatArray(2)
    private val valoresMatriz = FloatArray(9)

    private enum class Modo { NENHUM, TRACO, FORMA, SELECAO_NOVA, SELECAO_MOVER, ACIONADA }
    private var modo = Modo.NENHUM
    private var traco: Traco? = null
    private var renderizador: RenderizadorTraco? = null
    private var filtro: FiltroTraco? = null
    private var pontosDesenhados = 0
    private var inicioToque = 0L

    private var cursorVisivel = false
    private var cursorX = 0f
    private var cursorY = 0f

    private var inicioFormaX = 0f
    private var inicioFormaY = 0f
    private var fimFormaX = 0f
    private var fimFormaY = 0f
    private var selecao: RectF? = null
    private var inicioSelecaoX = 0f
    private var inicioSelecaoY = 0f
    private var deslocamentoSelecaoX = 0f
    private var deslocamentoSelecaoY = 0f

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

    fun ajustarVista() {
        val m = motor ?: return
        if (width == 0 || height == 0) return
        val escala = min(width.toFloat() / m.largura, height.toFloat() / m.altura) * 0.95f
        m.matrizVista.reset()
        m.matrizVista.postScale(escala, escala)
        m.matrizVista.postTranslate((width - m.largura * escala) / 2f, (height - m.altura * escala) / 2f)
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
            // Durante o traço livre o bitmap ativo é atualizado diretamente; fora dele a
            // composição já fica em cache e não precisa de misturar todas as camadas a cada frame.
            m.atualizarComposicao(forcar = modo == Modo.TRACO)
            canvas.save()
            canvas.concat(m.matrizVista)
            canvas.drawRect(0f, 0f, m.largura.toFloat(), m.altura.toFloat(), pincelFundo)
            canvas.drawBitmap(m.bitmap, 0f, 0f, pincelBitmap)
            if (gradeAtiva) {
                val passo = 100
                var x = 0
                while (x <= m.largura) { canvas.drawLine(x.toFloat(), 0f, x.toFloat(), m.altura.toFloat(), pincelGuia); x += passo }
                var y = 0
                while (y <= m.altura) { canvas.drawLine(0f, y.toFloat(), m.largura.toFloat(), y.toFloat(), pincelGuia); y += passo }
            }
            if (reguasAtivas) desenharReguas(canvas, m.largura, m.altura)
            if (simetriaAtiva) canvas.drawLine(m.largura / 2f, 0f, m.largura / 2f, m.altura.toFloat(), pincelSimetria)
            desenharPreviaForma(canvas)
            desenharSelecao(canvas)
            canvas.restore()
        }
        if (cursorDeslocadoAtivo && cursorVisivel && !emGesto) desenharCursor(canvas)
        canvas.restore()
    }

    private fun desenharPreviaForma(canvas: Canvas) {
        if (modo != Modo.FORMA) return
        val caminho = criarCaminhoForma(ferramenta, inicioFormaX, inicioFormaY, fimFormaX, fimFormaY)
        pincelPrevia.color = if (borracha) Color.WHITE else cor
        pincelPrevia.strokeWidth = larguraTraco.coerceAtLeast(1f)
        canvas.drawPath(caminho, pincelPrevia)
        if (simetriaAtiva) {
            val m = motor ?: return
            val matriz = Matrix().apply { setScale(-1f, 1f, m.largura / 2f, m.altura / 2f) }
            caminho.transform(matriz)
            canvas.drawPath(caminho, pincelPrevia)
        }
    }

    private fun desenharSelecao(canvas: Canvas) {
        val ret = selecao ?: return
        val exibido = RectF(ret)
        if (modo == Modo.SELECAO_MOVER) {
            exibido.offset(deslocamentoSelecaoX, deslocamentoSelecaoY)
        }
        canvas.drawRect(exibido, pincelSelecao)
        if (modo == Modo.SELECAO_NOVA) {
            canvas.drawRect(ret, pincelSelecao)
        }
    }

    private fun desenharReguas(canvas: Canvas, largura: Int, altura: Int) {
        var x = 0
        while (x <= largura) {
            val maior = x % 100 == 0
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), if (maior) 30f else 14f, pincelGuia)
            if (maior) canvas.drawText(x.toString(), x + 3f, 48f, pincelTextoGuia)
            x += 50
        }
        var y = 0
        while (y <= altura) {
            val maior = y % 100 == 0
            canvas.drawLine(0f, y.toFloat(), if (maior) 30f else 14f, y.toFloat(), pincelGuia)
            if (maior && y > 0) canvas.drawText(y.toString(), 34f, y - 4f, pincelTextoGuia)
            y += 50
        }
    }

    private fun aplicarEncaixe(ponto: FloatArray): FloatArray {
        if (!encaixeAtivo) return ponto
        val m = motor ?: return ponto
        val passo = 50f
        return floatArrayOf(
            (ponto[0] / passo).roundToInt().toFloat() * passo,
            (ponto[1] / passo).roundToInt().toFloat() * passo
        ).also {
            it[0] = it[0].coerceIn(0f, m.largura.toFloat())
            it[1] = it[1].coerceIn(0f, m.altura.toFloat())
        }
    }

    private fun desenharCursor(canvas: Canvas) {
        val raio = 7f * densidade
        canvas.drawCircle(cursorX, cursorY, raio, pincelCursorExterior)
        canvas.drawCircle(cursorX, cursorY, raio, pincelCursorInterior)
        val folga = 10f * densidade; val tamanho = 3f * densidade
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
                bloquearInteracao = false; limparEstadoGesto(); modo = Modo.NENHUM
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            return true
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                limparEstadoGesto()
                if (rejeicaoPalmaAtiva && contactoParecePalma(e, 0)) {
                    bloquearInteracao = true; esconderCursor(); return true
                }
                val docCru = coordenadaDocumento(e.x, e.y, aplicarCursor = ferramenta == FerramentaDesenho.PINCEL || ferramenta == FerramentaDesenho.BORRACHA)
                val doc = if (ferramenta == FerramentaDesenho.LINHA || ferramenta == FerramentaDesenho.RETANGULO ||
                    ferramenta == FerramentaDesenho.ELIPSE || ferramenta == FerramentaDesenho.SELECAO) aplicarEncaixe(docCru) else docCru
                when (ferramenta) {
                    FerramentaDesenho.BALDE -> {
                        m.preencher(doc[0].toInt(), doc[1].toInt(), cor)
                        modo = Modo.ACIONADA; invalidate()
                    }
                    FerramentaDesenho.CONTA_GOTAS -> {
                        val corCapturada = m.obterCor(doc[0].toInt(), doc[1].toInt(), camadaComposta = true)
                        cor = corCapturada; aoCapturarCor?.invoke(corCapturada)
                        modo = Modo.ACIONADA; invalidate()
                    }
                    FerramentaDesenho.LINHA, FerramentaDesenho.RETANGULO, FerramentaDesenho.ELIPSE -> {
                        modo = Modo.FORMA
                        inicioFormaX = doc[0]; inicioFormaY = doc[1]
                        fimFormaX = doc[0]; fimFormaY = doc[1]
                        inicioToque = e.eventTime; invalidate()
                    }
                    FerramentaDesenho.SELECAO -> iniciarSelecao(doc[0], doc[1])
                    else -> {
                        modo = Modo.TRACO
                        iniciarTraco(m, e)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (rejeicaoPalmaAtiva && contactoGrandePresente(e)) {
                    cancelarTraco(m); modo = Modo.NENHUM; bloquearInteracao = true
                    limparEstadoGesto(); esconderCursor(); return true
                }
                if (modo == Modo.TRACO) cancelarTraco(m)
                if (modo == Modo.FORMA || modo == Modo.SELECAO_NOVA || modo == Modo.SELECAO_MOVER) {
                    modo = Modo.NENHUM
                    deslocamentoSelecaoX = 0f; deslocamentoSelecaoY = 0f
                }
                if (!emGesto) {
                    emGesto = true; inicioGestoToqueMs = e.eventTime
                    maximoPonteiros = e.pointerCount; gestoMoveu = false
                    posicoesIniciais.clear(); guardarPosicoesIniciais(e)
                } else {
                    maximoPonteiros = max(maximoPonteiros, e.pointerCount); guardarPosicoesIniciais(e)
                }
                cursorVisivel = false; precisaBase = true; invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (emGesto) {
                    atualizarMovimentoDoGesto(e)
                    if (zoomGestualAtivo && e.pointerCount == 2) atualizarGesto(m, e)
                    else if (e.pointerCount != 2) precisaBase = true
                } else when (modo) {
                    Modo.TRACO -> continuarTraco(m, e)
                    Modo.FORMA -> {
                        val doc = aplicarEncaixe(coordenadaDocumento(e.x, e.y))
                        fimFormaX = doc[0]; fimFormaY = doc[1]; invalidate()
                    }
                    Modo.SELECAO_NOVA -> {
                        val doc = if (encaixeAtivo) aplicarEncaixe(coordenadaDocumento(e.x, e.y)) else coordenadaDocumento(e.x, e.y)
                        selecao = retanguloNormalizado(inicioSelecaoX, inicioSelecaoY, doc[0], doc[1]); invalidate()
                    }
                    Modo.SELECAO_MOVER -> {
                        val doc = coordenadaDocumento(e.x, e.y)
                        deslocamentoSelecaoX = doc[0] - inicioSelecaoX
                        deslocamentoSelecaoY = doc[1] - inicioSelecaoY; invalidate()
                    }
                    else -> Unit
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (emGesto) {
                    atualizarMovimentoDoGesto(e)
                    posicoesIniciais.remove(e.getPointerId(e.actionIndex)); precisaBase = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (emGesto) {
                    atualizarMovimentoDoGesto(e)
                    executarAtalhoSeToqueRapido(m, e.eventTime); limparEstadoGesto()
                } else when (modo) {
                    Modo.TRACO -> finalizarTraco(m, e)
                    Modo.FORMA -> finalizarForma(m, e)
                    Modo.SELECAO_NOVA -> finalizarNovaSelecao(e)
                    Modo.SELECAO_MOVER -> finalizarMovimentoSelecao(m, e)
                    else -> Unit
                }
                modo = Modo.NENHUM; esconderCursor()
                parent?.requestDisallowInterceptTouchEvent(false); invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelarTraco(m); modo = Modo.NENHUM; limparEstadoGesto(); esconderCursor()
                bloquearInteracao = false; deslocamentoSelecaoX = 0f; deslocamentoSelecaoY = 0f
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun iniciarSelecao(x: Float, y: Float) {
        val atual = selecao
        if (atual != null && atual.contains(x, y)) {
            modo = Modo.SELECAO_MOVER
            inicioSelecaoX = x; inicioSelecaoY = y
        } else {
            modo = Modo.SELECAO_NOVA
            inicioSelecaoX = x; inicioSelecaoY = y
            selecao = RectF(x, y, x, y)
        }
        deslocamentoSelecaoX = 0f; deslocamentoSelecaoY = 0f; invalidate()
    }

    private fun finalizarNovaSelecao(e: MotionEvent) {
        val docCru = coordenadaDocumento(e.x, e.y)
        val doc = if (encaixeAtivo) aplicarEncaixe(docCru) else docCru
        val ret = retanguloNormalizado(inicioSelecaoX, inicioSelecaoY, doc[0], doc[1])
        selecao = if (ret.width() >= 3f && ret.height() >= 3f) ret else null
        invalidate()
    }

    private fun finalizarMovimentoSelecao(m: MotorDesenho, e: MotionEvent) {
        val ret = selecao ?: return
        val doc = coordenadaDocumento(e.x, e.y)
        val dx = (doc[0] - inicioSelecaoX).toInt()
        val dy = (doc[1] - inicioSelecaoY).toInt()
        if (m.moverConteudoSelecionado(ret.left.toInt(), ret.top.toInt(), ret.right.toInt(), ret.bottom.toInt(), dx, dy)) {
            ret.offset(dx.toFloat(), dy.toFloat())
            if (ret.right < 0f || ret.bottom < 0f || ret.left > m.largura || ret.top > m.altura) selecao = null
            else {
                val larguraRet = ret.width().coerceAtMost(m.largura.toFloat())
                val alturaRet = ret.height().coerceAtMost(m.altura.toFloat())
                val novoLeft = ret.left.coerceIn(0f, m.largura - larguraRet)
                val novoTop = ret.top.coerceIn(0f, m.altura - alturaRet)
                selecao = RectF(novoLeft, novoTop, novoLeft + larguraRet, novoTop + alturaRet)
            }
        }
        deslocamentoSelecaoX = 0f; deslocamentoSelecaoY = 0f
    }

    // ---------- Traço livre e estabilização ----------

    private fun iniciarTraco(m: MotorDesenho, e: MotionEvent) {
        val novo = Traco(cor, larguraTraco.coerceAtLeast(1f), borracha).apply {
            simetriaVertical = simetriaAtiva
            larguraDocumento = m.largura
        }
        traco = novo; renderizador = null; pontosDesenhados = 0; inicioToque = e.eventTime
        filtro = if (estabilizacaoAtiva) FiltroTraco(intensidadeEstabilizacao) else null
        filtro?.iniciar(e.x, e.y, e.eventTime)
        adicionarAmostra(m, novo, e.x, e.y, e.eventTime)
        invalidate()
    }

    private fun continuarTraco(m: MotorDesenho, e: MotionEvent) {
        val atual = traco ?: return
        for (i in 0 until e.historySize) {
            adicionarAmostra(m, atual, e.getHistoricalX(i), e.getHistoricalY(i), e.getHistoricalEventTime(i))
        }
        adicionarAmostra(m, atual, e.x, e.y, e.eventTime)
        if (renderizador != null || e.eventTime - inicioToque >= ATRASO_INICIO_MS) desenharPendentes(m)
        invalidate()
    }

    private fun adicionarAmostra(m: MotorDesenho, t: Traco, xCru: Float, yCru: Float, tempoMs: Long) {
        val f = filtro
        val x: Float; val y: Float
        if (estabilizacaoAtiva && f != null) {
            f.filtrar(xCru, yCru, tempoMs, pontoFiltrado); x = pontoFiltrado[0]; y = pontoFiltrado[1]
        } else { x = xCru; y = yCru }
        aplicarDeslocamentoCursor(x, y, pontoDeslocado)
        adicionarPontoNaTela(m, t, pontoDeslocado[0], pontoDeslocado[1])
        if (cursorDeslocadoAtivo) { cursorX = pontoDeslocado[0]; cursorY = pontoDeslocado[1]; cursorVisivel = true }
    }

    private fun aplicarDeslocamentoCursor(x: Float, y: Float, destino: FloatArray) {
        if (!cursorDeslocadoAtivo || width <= 0 || height <= 0) {
            destino[0] = x; destino[1] = y; return
        }
        destino[0] = (x + 22f * densidade).coerceIn(0f, width.toFloat())
        destino[1] = (y - distanciaCursorDp.coerceIn(32f, 104f) * densidade).coerceIn(0f, height.toFloat())
    }

    private fun adicionarPontoNaTela(m: MotorDesenho, t: Traco, x: Float, y: Float) {
        val doc = coordenadaDocumento(x, y)
        val n = t.pontos.tamanho
        if (n > 0) {
            val dx = doc[0] - t.pontos.x(n - 1); val dy = doc[1] - t.pontos.y(n - 1)
            if (dx * dx + dy * dy < 0.09f) return
        }
        t.pontos.adicionar(doc[0], doc[1])
    }

    private fun desenharPendentes(m: MotorDesenho) {
        val t = traco ?: return
        var r = renderizador
        if (r == null) {
            if (t.pontos.tamanho == 0) return
            r = RenderizadorTraco(m.tela, t); renderizador = r
            r.iniciar(t.pontos.x(0), t.pontos.y(0)); pontosDesenhados = 1
        }
        while (pontosDesenhados < t.pontos.tamanho) {
            r.adicionar(t.pontos.x(pontosDesenhados), t.pontos.y(pontosDesenhados)); pontosDesenhados++
        }
    }

    private fun finalizarTraco(m: MotorDesenho, e: MotionEvent) {
        val t = traco ?: return
        adicionarAmostra(m, t, e.x, e.y, e.eventTime)
        val assistido = if (assistenciaFormaAtiva && e.eventTime - inicioToque >= ATRASO_FORMA_MS) detectarFormaAssistida(t) else null
        if (assistido != null) {
            traco = null; renderizador = null; filtro = null
            m.reconstruir()
            m.aplicarTraco(assistido)
        } else {
            desenharPendentes(m); renderizador?.finalizar(); m.registrar(t)
            traco = null; renderizador = null; filtro = null
        }
        invalidate()
    }

    private fun detectarFormaAssistida(original: Traco): Traco? {
        val p = original.pontos
        if (p.tamanho < 5) return null
        val x0 = p.x(0); val y0 = p.y(0); val xn = p.x(p.tamanho - 1); val yn = p.y(p.tamanho - 1)
        var minX = x0; var maxX = x0; var minY = y0; var maxY = y0
        for (i in 1 until p.tamanho) {
            minX = min(minX, p.x(i)); maxX = max(maxX, p.x(i))
            minY = min(minY, p.y(i)); maxY = max(maxY, p.y(i))
        }
        val largura = maxX - minX; val altura = maxY - minY
        if (largura < 12f && altura < 12f) return null
        val distanciaFecho = hypot(xn - x0, yn - y0)
        val maiorDimensao = max(largura, altura)
        if (distanciaFecho <= maiorDimensao * 0.28f && min(largura, altura) >= 12f) {
            return criarTracoForma(original, FerramentaDesenho.ELIPSE, minX, minY, maxX, maxY)
        }
        val dx = xn - x0; val dy = yn - y0; val comprimento = hypot(dx, dy)
        if (comprimento < 20f) return null
        var desvioMaximo = 0f
        for (i in 1 until p.tamanho - 1) {
            val desvio = abs(dy * (p.x(i) - x0) - dx * (p.y(i) - y0)) / comprimento
            desvioMaximo = max(desvioMaximo, desvio)
        }
        return if (desvioMaximo <= max(6f, comprimento * 0.09f)) {
            criarTracoForma(original, FerramentaDesenho.LINHA, x0, y0, xn, yn)
        } else null
    }

    private fun finalizarForma(m: MotorDesenho, e: MotionEvent) {
        val doc = aplicarEncaixe(coordenadaDocumento(e.x, e.y))
        fimFormaX = doc[0]; fimFormaY = doc[1]
        val t = criarTracoForma(
            Traco(cor, larguraTraco.coerceAtLeast(1f), borracha).apply {
                simetriaVertical = simetriaAtiva; larguraDocumento = m.largura
            }, ferramenta, inicioFormaX, inicioFormaY, fimFormaX, fimFormaY
        )
        if (t.pontos.tamanho >= 2) m.aplicarTraco(t)
        invalidate()
    }

    private fun criarTracoForma(
        base: Traco, tipo: FerramentaDesenho, x0: Float, y0: Float, x1: Float, y1: Float
    ): Traco {
        val resultado = Traco(base.cor, base.largura, base.borracha).apply {
            simetriaVertical = base.simetriaVertical
            larguraDocumento = base.larguraDocumento
            segmentosLineares = true
        }
        when (tipo) {
            FerramentaDesenho.LINHA -> { resultado.pontos.adicionar(x0, y0); resultado.pontos.adicionar(x1, y1) }
            FerramentaDesenho.RETANGULO -> {
                val l = min(x0, x1); val r = max(x0, x1); val t = min(y0, y1); val b = max(y0, y1)
                resultado.pontos.adicionar(l, t); resultado.pontos.adicionar(r, t)
                resultado.pontos.adicionar(r, b); resultado.pontos.adicionar(l, b); resultado.pontos.adicionar(l, t)
            }
            FerramentaDesenho.ELIPSE -> {
                val l = min(x0, x1); val r = max(x0, x1); val t = min(y0, y1); val b = max(y0, y1)
                val cx = (l + r) / 2f; val cy = (t + b) / 2f
                val rx = max(0.5f, (r - l) / 2f); val ry = max(0.5f, (b - t) / 2f)
                for (i in 0..64) {
                    val angulo = 2.0 * Math.PI * i / 64.0
                    resultado.pontos.adicionar(cx + (kotlin.math.cos(angulo) * rx).toFloat(), cy + (kotlin.math.sin(angulo) * ry).toFloat())
                }
            }
            else -> { resultado.pontos.adicionar(x0, y0); resultado.pontos.adicionar(x1, y1) }
        }
        return resultado
    }

    private fun criarCaminhoForma(tipo: FerramentaDesenho, x0: Float, y0: Float, x1: Float, y1: Float): Path {
        val caminho = Path()
        when (tipo) {
            FerramentaDesenho.LINHA -> { caminho.moveTo(x0, y0); caminho.lineTo(x1, y1) }
            FerramentaDesenho.RETANGULO -> caminho.addRect(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), Path.Direction.CW)
            FerramentaDesenho.ELIPSE -> caminho.addOval(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), Path.Direction.CW)
            else -> Unit
        }
        return caminho
    }

    private fun cancelarTraco(m: MotorDesenho) {
        if (traco == null) return
        val jaDesenhou = renderizador != null
        traco = null; renderizador = null; filtro = null
        if (jaDesenhou) m.reconstruir()
        invalidate()
    }

    private fun esconderCursor() { cursorVisivel = false; invalidate() }

    private fun coordenadaDocumento(x: Float, y: Float, aplicarCursor: Boolean = false): FloatArray {
        val m = motor ?: return floatArrayOf(0f, 0f)
        val tx: Float; val ty: Float
        if (aplicarCursor && cursorDeslocadoAtivo) {
            tx = (x + 22f * densidade).coerceIn(0f, width.toFloat())
            ty = (y - distanciaCursorDp.coerceIn(32f, 104f) * densidade).coerceIn(0f, height.toFloat())
        } else { tx = x; ty = y }
        if (!m.matrizVista.invert(inversa)) return floatArrayOf(0f, 0f)
        ponto[0] = tx; ponto[1] = ty; inversa.mapPoints(ponto)
        return floatArrayOf(ponto[0].coerceIn(0f, m.largura.toFloat()), ponto[1].coerceIn(0f, m.altura.toFloat()))
    }

    private fun retanguloNormalizado(x0: Float, y0: Float, x1: Float, y1: Float) =
        RectF(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1))

    // ---------- Gestos multitoque da Fase 2 ----------

    private fun guardarPosicoesIniciais(e: MotionEvent) {
        for (i in 0 until e.pointerCount) {
            val id = e.getPointerId(i)
            if (!posicoesIniciais.containsKey(id)) posicoesIniciais[id] = Pair(e.getX(i), e.getY(i))
        }
    }

    private fun atualizarMovimentoDoGesto(e: MotionEvent) {
        if (gestoMoveu) return
        val limite = LIMIAR_MOVIMENTO_TOQUE_DP * densidade; val limite2 = limite * limite
        for (i in 0 until e.pointerCount) {
            val inicial = posicoesIniciais[e.getPointerId(i)] ?: continue
            val dx = e.getX(i) - inicial.first; val dy = e.getY(i) - inicial.second
            if (dx * dx + dy * dy > limite2) { gestoMoveu = true; return }
        }
    }

    private fun lerBase(e: MotionEvent) {
        if (e.pointerCount < 2) return
        val dx = e.getX(1) - e.getX(0); val dy = e.getY(1) - e.getY(0)
        ultimoCx = (e.getX(0) + e.getX(1)) / 2f; ultimoCy = (e.getY(0) + e.getY(1)) / 2f
        ultimaDist = hypot(dx, dy); ultimoAngulo = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    }

    private fun atualizarGesto(m: MotorDesenho, e: MotionEvent) {
        if (precisaBase) { lerBase(e); precisaBase = false; return }
        val dx = e.getX(1) - e.getX(0); val dy = e.getY(1) - e.getY(0)
        val novoCx = (e.getX(0) + e.getX(1)) / 2f; val novoCy = (e.getY(0) + e.getY(1)) / 2f
        val novaDist = hypot(dx, dy); val novoAngulo = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        if (ultimaDist > 1f && novaDist > 1f) {
            val atual = escalaAtual(m.matrizVista).coerceAtLeast(0.0001f)
            var fator = novaDist / ultimaDist; val alvo = (atual * fator).coerceIn(ESCALA_MIN, ESCALA_MAX); fator = alvo / atual
            var giro = novoAngulo - ultimoAngulo
            while (giro > 180f) giro -= 360f
            while (giro < -180f) giro += 360f
            m.matrizVista.postScale(fator, fator, ultimoCx, ultimoCy)
            m.matrizVista.postRotate(giro, ultimoCx, ultimoCy)
            m.matrizVista.postTranslate(novoCx - ultimoCx, novoCy - ultimoCy)
        }
        ultimoCx = novoCx; ultimoCy = novoCy; ultimaDist = novaDist; ultimoAngulo = novoAngulo
        invalidate()
    }

    private fun executarAtalhoSeToqueRapido(m: MotorDesenho, fimMs: Long) {
        if (!atalhosGestosAtivos || gestoMoveu) return
        val duracao = fimMs - inicioGestoToqueMs
        if (duracao < 0L || duracao > DURACAO_MAX_TOQUE_MS) return
        when (maximoPonteiros) { 2 -> m.desfazer(); 3 -> m.refazer() }
    }

    private fun limparEstadoGesto() {
        emGesto = false; precisaBase = false; inicioGestoToqueMs = 0L; maximoPonteiros = 0; gestoMoveu = false
        posicoesIniciais.clear(); ultimaDist = 0f; ultimoAngulo = 0f
    }

    // ---------- Rejeição conservadora da palma ----------

    private fun contactoParecePalma(e: MotionEvent, indice: Int): Boolean {
        if (indice !in 0 until e.pointerCount) return false
        val larguraDp = e.getTouchMajor(indice) / densidade; val alturaDp = e.getTouchMinor(indice) / densidade
        if (larguraDp <= 0f || alturaDp <= 0f) return false
        return larguraDp >= LIMIAR_PALMA_LARGO_DP || (larguraDp >= LIMIAR_PALMA_DUPLA_DP && alturaDp >= LIMIAR_PALMA_ALTO_DP)
    }

    private fun contactoGrandePresente(e: MotionEvent): Boolean {
        for (i in 0 until e.pointerCount) if (contactoParecePalma(e, i)) return true
        return false
    }

    private fun escalaAtual(matriz: Matrix): Float {
        matriz.getValues(valoresMatriz)
        return hypot(valoresMatriz[Matrix.MSCALE_X], valoresMatriz[Matrix.MSKEW_Y])
    }

    private companion object {
        const val ATRASO_INICIO_MS = 40L
        const val ATRASO_FORMA_MS = 550L
        const val DURACAO_MAX_TOQUE_MS = 450L
        const val LIMIAR_MOVIMENTO_TOQUE_DP = 14f
        const val LIMIAR_PALMA_LARGO_DP = 72f
        const val LIMIAR_PALMA_DUPLA_DP = 48f
        const val LIMIAR_PALMA_ALTO_DP = 34f
        const val ESCALA_MIN = 0.05f
        const val ESCALA_MAX = 30f
    }
}
