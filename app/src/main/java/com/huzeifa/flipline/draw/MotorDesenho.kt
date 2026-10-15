package com.huzeifa.flipline.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix

/**
 * Guarda o desenho: o bitmap, a lista de traços (histórico) e a posição
 * do zoom/giro. Desfazer e refazer funcionam reconstruindo o bitmap
 * a partir dos traços.
 */
class MotorDesenho(
    val largura: Int = 1200,
    val altura: Int = 1600
) {
    val bitmap: Bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    val tela: Canvas = Canvas(bitmap)

    /** Transformação do desenho para a tela (zoom, giro e posição). */
    val matrizVista = Matrix()
    var vistaAjustada = false

    val ferramentas = EstadoFerramentas()

    /** Avisa a interface quando o histórico muda. */
    var aoMudar: (() -> Unit)? = null

    private val tracos = ArrayList<Traco>()
    private val refazerPilha = ArrayList<Traco>()

    fun temConteudo(): Boolean = tracos.isNotEmpty()

    fun podeDesfazer(): Boolean = tracos.isNotEmpty()

    fun podeRefazer(): Boolean = refazerPilha.isNotEmpty()

    fun registrar(traco: Traco) {
        tracos.add(traco)
        refazerPilha.clear()
        aoMudar?.invoke()
    }

    fun desfazer() {
        if (tracos.isEmpty()) return
        refazerPilha.add(tracos.removeAt(tracos.size - 1))
        reconstruir()
        aoMudar?.invoke()
    }

    fun refazer() {
        if (refazerPilha.isEmpty()) return
        tracos.add(refazerPilha.removeAt(refazerPilha.size - 1))
        reconstruir()
        aoMudar?.invoke()
    }

    /** Apaga o bitmap e desenha de novo todos os traços do histórico. */
    fun reconstruir() {
        bitmap.eraseColor(Color.TRANSPARENT)
        for (traco in tracos) {
            desenharTraco(traco)
        }
    }

    private fun desenharTraco(traco: Traco) {
        val p = traco.pontos
        if (p.tamanho == 0) return
        val r = RenderizadorTraco(tela, traco)
        r.iniciar(p.x(0), p.y(0))
        for (i in 1 until p.tamanho) {
            r.adicionar(p.x(i), p.y(i))
        }
        r.finalizar()
    }
}
