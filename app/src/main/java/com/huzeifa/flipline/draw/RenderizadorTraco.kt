package com.huzeifa.flipline.draw

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Desenha um traço aos poucos, suavizando com curvas (pontos médios).
 * O mesmo código serve para o desenho ao vivo e para refazer tudo
 * depois de um desfazer, então o resultado é sempre idêntico.
 */
class RenderizadorTraco(private val tela: Canvas, traco: Traco) {

    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = traco.largura
        if (traco.borracha) {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            color = traco.cor
        }
    }

    private val caminho = Path()
    private var anteriorX = 0f
    private var anteriorY = 0f
    private var meioX = 0f
    private var meioY = 0f

    fun iniciar(x: Float, y: Float) {
        tela.drawPoint(x, y, pincel)
        anteriorX = x
        anteriorY = y
        meioX = x
        meioY = y
    }

    fun adicionar(x: Float, y: Float) {
        val novoMeioX = (anteriorX + x) / 2f
        val novoMeioY = (anteriorY + y) / 2f
        caminho.rewind()
        caminho.moveTo(meioX, meioY)
        caminho.quadTo(anteriorX, anteriorY, novoMeioX, novoMeioY)
        tela.drawPath(caminho, pincel)
        meioX = novoMeioX
        meioY = novoMeioY
        anteriorX = x
        anteriorY = y
    }

    fun finalizar() {
        caminho.rewind()
        caminho.moveTo(meioX, meioY)
        caminho.lineTo(anteriorX, anteriorY)
        tela.drawPath(caminho, pincel)
    }
}
