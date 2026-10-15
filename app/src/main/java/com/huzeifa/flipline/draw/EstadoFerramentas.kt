package com.huzeifa.flipline.draw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class FerramentaDesenho {
    PINCEL, BORRACHA, BALDE, CONTA_GOTAS, LINHA, RETANGULO, ELIPSE, SELECAO
}

enum class ModoMesclagem { NORMAL, MULTIPLICAR, TELA }

/** Ferramenta, cor, espessuras e ajudas de desenho reativas à interface. */
class EstadoFerramentas {
    var ferramenta by mutableStateOf(FerramentaDesenho.PINCEL)
    var borracha: Boolean
        get() = ferramenta == FerramentaDesenho.BORRACHA
        set(value) { ferramenta = if (value) FerramentaDesenho.BORRACHA else FerramentaDesenho.PINCEL }
    var cor by mutableIntStateOf(0xFF000000.toInt())
    var larguraPincel by mutableFloatStateOf(8f)
    var larguraBorracha by mutableFloatStateOf(40f)
    var gradeAtiva by mutableStateOf(false)
    var reguasAtivas by mutableStateOf(false)
    var encaixeAtivo by mutableStateOf(false)
    var simetriaAtiva by mutableStateOf(false)
    var assistenciaFormaAtiva by mutableStateOf(true)
}
