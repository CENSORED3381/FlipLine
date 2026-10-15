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

/**
 * Área de desenho.
 * - 1 dedo: desenha.
 * - 2 dedos: zoom, mover e girar o papel.
 */
class VistaDesenho(context: Context) : View(context) {

    var motor: MotorDesenho? = null
        set(valor) {
            field = valor
            if (valor != null && width > 0 && !valor.vistaAjustada) {
                ajustarVista()
            }
            invalidate()
        }

    var borracha: Boolean = false
    var cor: Int = Color.BLACK
    var larguraTraco: Float = 8f

    private val pincelFundo = Paint().apply { color = Color.WHITE }
    private val pincelBitmap = Paint(Paint.FILTER_BITMAP_FLAG)
    private val inversa = Matrix()
    private val ponto = FloatArray(2)
    private val valoresMatriz = FloatArray(9)

    // Traço em andamento
    private var traco: Traco? = null
    private var renderizador: RenderizadorTraco? = null
    private var pontosDesenhados = 0
    private var inicioToque = 0L

    // Gesto de dois dedos
    private var emGesto = false
    private var precisaBase = false
    private var ultimoCx = 0f
    private var ultimoCy = 0f
    private var ultimaDist = 0f
    private var ultimoAngulo = 0f

    override fun onSizeChanged(w: Int, h: Int, antigoW: Int, antigoH: Int) {
        super.onSizeChanged(w, h, antigoW, antigoH)
        val m = motor ?: return
        if (!m.vistaAjustada) ajustarVista()
    }

    /** Centraliza o papel e remove zoom e giro. */
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
        canvas.drawColor(0xFF2B2B2B.toInt())
        val m = motor ?: return
        canvas.save()
        canvas.concat(m.matrizVista)
        canvas.drawRect(0f, 0f, m.largura.toFloat(), m.altura.toFloat(), pincelFundo)
        canvas.drawBitmap(m.bitmap, 0f, 0f, pincelBitmap)
        canvas.restore()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val m = motor ?: return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                emGesto = false
                iniciarTraco(m, e)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!emGesto) {
                    cancelarTraco(m)
                    emGesto = true
                }
                precisaBase = true
            }

            MotionEvent.ACTION_MOVE -> {
                if (emGesto) {
                    if (e.pointerCount >= 2) atualizarGesto(m, e)
                } else {
                    continuarTraco(m, e)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (emGesto) precisaBase = true
            }

            MotionEvent.ACTION_UP -> {
                if (!emGesto) finalizarTraco(m)
                emGesto = false
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelarTraco(m)
                emGesto = false
            }
        }
        return true
    }

    // ---------- Desenho ----------

    private fun paraDesenho(m: MotorDesenho, x: Float, y: Float) {
        m.matrizVista.invert(inversa)
        ponto[0] = x
        ponto[1] = y
        inversa.mapPoints(ponto)
    }

    private fun adicionarPonto(m: MotorDesenho, t: Traco, x: Float, y: Float) {
        paraDesenho(m, x, y)
        val n = t.pontos.tamanho
        if (n > 0) {
            val dx = ponto[0] - t.pontos.x(n - 1)
            val dy = ponto[1] - t.pontos.y(n - 1)
            if (dx * dx + dy * dy < 0.09f) return
        }
        t.pontos.adicionar(ponto[0], ponto[1])
    }

    private fun iniciarTraco(m: MotorDesenho, e: MotionEvent) {
        val t = Traco(cor, larguraTraco, borracha)
        paraDesenho(m, e.x, e.y)
        t.pontos.adicionar(ponto[0], ponto[1])
        traco = t
        renderizador = null
        pontosDesenhados = 0
        inicioToque = e.eventTime
    }

    private fun continuarTraco(m: MotorDesenho, e: MotionEvent) {
        val t = traco ?: return
        for (i in 0 until e.historySize) {
            adicionarPonto(m, t, e.getHistoricalX(i), e.getHistoricalY(i))
        }
        adicionarPonto(m, t, e.x, e.y)
        // Espera um instante antes de desenhar: se um segundo dedo chegar,
        // o traço é descartado sem deixar marca.
        if (renderizador != null || e.eventTime - inicioToque >= ATRASO_INICIO_MS) {
            desenharPendentes(m)
        }
        invalidate()
    }

    private fun desenharPendentes(m: MotorDesenho) {
        val t = traco ?: return
        var r = renderizador
        if (r == null) {
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

    private fun finalizarTraco(m: MotorDesenho) {
        val t = traco ?: return
        desenharPendentes(m)
        renderizador?.finalizar()
        m.registrar(t)
        traco = null
        renderizador = null
        invalidate()
    }

    private fun cancelarTraco(m: MotorDesenho) {
        if (traco == null) return
        val jaDesenhou = renderizador != null
        traco = null
        renderizador = null
        if (jaDesenhou) m.reconstruir()
        invalidate()
    }

    // ---------- Zoom, mover e girar ----------

    private fun lerBase(e: MotionEvent) {
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
            var fator = novaDist / ultimaDist
            val atual = escalaAtual(m.matrizVista)
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

    private fun escalaAtual(matriz: Matrix): Float {
        matriz.getValues(valoresMatriz)
        return hypot(valoresMatriz[Matrix.MSCALE_X], valoresMatriz[Matrix.MSKEW_Y])
    }

    private companion object {
        const val ATRASO_INICIO_MS = 40L
        const val ESCALA_MIN = 0.05f
        const val ESCALA_MAX = 30f
    }
}
