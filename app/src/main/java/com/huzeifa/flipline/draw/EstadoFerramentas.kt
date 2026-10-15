package com.huzeifa.flipline.draw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Ferramenta, cor e espessuras escolhidas. A interface observa estes valores. */
class EstadoFerramentas {
    var borracha by mutableStateOf(false)
    var cor by mutableIntStateOf(0xFF000000.toInt())
    var larguraPincel by mutableFloatStateOf(8f)
    var larguraBorracha by mutableFloatStateOf(40f)
}
