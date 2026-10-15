package com.huzeifa.flipline.draw

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/** Renderizador incremental partilhado pelo desenho ao vivo e pela reconstrução do histórico. */
class RenderizadorTraco(private val tela: Canvas, private val traco: Traco) {
    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = traco.largura.coerceAtLeast(1f)
        if (traco.borracha) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        else color = traco.cor
    }
    private val caminho = Path()
    private var anteriorX = 0f
    private var anteriorY = 0f
    private var meioX = 0f
    private var meioY = 0f

    fun iniciar(x: Float, y: Float) {
        desenharPonto(x, y)
        anteriorX = x; anteriorY = y; meioX = x; meioY = y
    }

    fun adicionar(x: Float, y: Float) {
        if (traco.segmentosLineares) {
            tela.drawLine(anteriorX, anteriorY, x, y, pincel)
            if (traco.simetriaVertical) {
                tela.drawLine(traco.larguraDocumento - anteriorX, anteriorY,
                    traco.larguraDocumento - x, y, pincel)
            }
            anteriorX = x; anteriorY = y; meioX = x; meioY = y
            return
        }
        val novoMeioX = (anteriorX + x) / 2f
        val novoMeioY = (anteriorY + y) / 2f
        caminho.rewind()
        caminho.moveTo(meioX, meioY)
        caminho.quadTo(anteriorX, anteriorY, novoMeioX, novoMeioY)
        tela.drawPath(caminho, pincel)
        if (traco.simetriaVertical) {
            caminho.rewind()
            caminho.moveTo(traco.larguraDocumento - meioX, meioY)
            caminho.quadTo(traco.larguraDocumento - anteriorX, anteriorY, traco.larguraDocumento - novoMeioX, novoMeioY)
            tela.drawPath(caminho, pincel)
        }
        meioX = novoMeioX; meioY = novoMeioY
        anteriorX = x; anteriorY = y
    }

    fun finalizar() {
        if (traco.segmentosLineares) return
        caminho.rewind(); caminho.moveTo(meioX, meioY); caminho.lineTo(anteriorX, anteriorY)
        tela.drawPath(caminho, pincel)
        if (traco.simetriaVertical) {
            caminho.rewind(); caminho.moveTo(traco.larguraDocumento - meioX, meioY)
            caminho.lineTo(traco.larguraDocumento - anteriorX, anteriorY)
            tela.drawPath(caminho, pincel)
        }
    }

    private fun desenharPonto(x: Float, y: Float) {
        tela.drawPoint(x, y, pincel)
        if (traco.simetriaVertical) tela.drawPoint(traco.larguraDocumento - x, y, pincel)
    }
}
