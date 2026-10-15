package com.huzeifa.flipline.draw

/** Lista compacta de pontos (x, y) em coordenadas do documento. */
class PontosBuffer {
    private var dados = FloatArray(128)
    var tamanho = 0
        private set

    fun adicionar(x: Float, y: Float) {
        if (tamanho * 2 + 2 > dados.size) dados = dados.copyOf(dados.size * 2)
        dados[tamanho * 2] = x
        dados[tamanho * 2 + 1] = y
        tamanho++
    }

    fun x(i: Int): Float = dados[i * 2]
    fun y(i: Int): Float = dados[i * 2 + 1]
    fun copiar(): PontosBuffer = PontosBuffer().also { destino ->
        for (i in 0 until tamanho) destino.adicionar(x(i), y(i))
    }
}

/** Um traço completo, incluindo opcionalmente a sua cópia simétrica vertical. */
class Traco(
    val cor: Int,
    val largura: Float,
    val borracha: Boolean,
    val pontos: PontosBuffer = PontosBuffer(),
    var simetriaVertical: Boolean = false,
    var larguraDocumento: Int = 1200,
    /** Formas geométricas usam segmentos retos para manter cantos e contornos precisos. */
    var segmentosLineares: Boolean = false
)
