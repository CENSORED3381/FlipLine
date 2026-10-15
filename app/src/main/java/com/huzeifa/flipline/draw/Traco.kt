package com.huzeifa.flipline.draw

/** Lista crescente de pontos (x, y) em coordenadas do desenho. */
class PontosBuffer {
    private var dados = FloatArray(128)
    var tamanho = 0
        private set

    fun adicionar(x: Float, y: Float) {
        if (tamanho * 2 + 2 > dados.size) {
            dados = dados.copyOf(dados.size * 2)
        }
        dados[tamanho * 2] = x
        dados[tamanho * 2 + 1] = y
        tamanho++
    }

    fun x(i: Int): Float = dados[i * 2]

    fun y(i: Int): Float = dados[i * 2 + 1]
}

/** Um traço completo: usado para desenhar, desfazer e refazer. */
class Traco(
    val cor: Int,
    val largura: Float,
    val borracha: Boolean,
    val pontos: PontosBuffer = PontosBuffer()
)
