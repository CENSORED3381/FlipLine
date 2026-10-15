package com.huzeifa.flipline.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** Dados exportáveis de uma camada. O bitmap é uma cópia independente. */
data class DadosCamada(
    val nome: String,
    val bitmap: Bitmap,
    val visivel: Boolean = true,
    val opacidade: Int = 255,
    val modoMesclagem: ModoMesclagem = ModoMesclagem.NORMAL,
    /** PNG original usado como base de undo ao abrir um projeto; evita recompressão no UI thread. */
    val bitmapBasePng: ByteArray? = null
)

class CamadaDesenho internal constructor(
    val id: Long,
    var nome: String,
    val bitmap: Bitmap,
    internal var bitmapBasePng: ByteArray?,
    var visivel: Boolean = true,
    var opacidade: Int = 255,
    var modoMesclagem: ModoMesclagem = ModoMesclagem.NORMAL
) {
    val tela: Canvas = Canvas(bitmap)
}

/**
 * Motor raster em camadas. Os comandos mantêm histórico global; cada comando pertence
 * a uma camada e a reconstrução reproduz a ordem original dos traços e preenchimentos.
 */
class MotorDesenho(
    val largura: Int = 1200,
    val altura: Int = 1600
) {
    val bitmap: Bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    private val telaComposicao = Canvas(bitmap)
    private val pincelCamada = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pincelLimpar = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    val matrizVista = Matrix()
    var vistaAjustada = false
    val ferramentas = EstadoFerramentas()
    var aoMudar: (() -> Unit)? = null

    val camadas: MutableList<CamadaDesenho> = mutableListOf()
    var indiceCamadaAtiva: Int = 0
        private set
    val camadaAtiva: CamadaDesenho get() = camadas[indiceCamadaAtiva.coerceIn(0, camadas.lastIndex)]
    val tela: Canvas get() = camadaAtiva.tela

    private sealed class Acao(open val camadaId: Long)
    private data class AcaoTracos(override val camadaId: Long, val tracos: List<Traco>) : Acao(camadaId)
    private data class AcaoPreenchimento(
        override val camadaId: Long,
        val indices: IntArray,
        val cor: Int
    ) : Acao(camadaId)
    private data class AcaoBitmap(
        override val camadaId: Long,
        val depoisPng: ByteArray
    ) : Acao(camadaId)

    private val historico = ArrayList<Acao>()
    private val pilhaRefazer = ArrayList<Acao>()
    // Mantém a composição em cache durante a navegação/idle e evita recompor sem alterações.
    private var composicaoSuja = true
    private var proximoIdCamada = 1L

    init {
        adicionarCamadaInterna("Camada 1", notificar = false)
        atualizarComposicao()
    }

    fun temConteudo(): Boolean {
        if (historico.isNotEmpty()) return true
        return camadas.any { camada ->
            val pixels = IntArray(camada.bitmap.width * camada.bitmap.height)
            camada.bitmap.getPixels(pixels, 0, camada.bitmap.width, 0, 0, camada.bitmap.width, camada.bitmap.height)
            pixels.any { Color.alpha(it) != 0 }
        }
    }

    fun podeDesfazer(): Boolean = historico.isNotEmpty()
    fun podeRefazer(): Boolean = pilhaRefazer.isNotEmpty()

    /** Regista um traço que já foi desenhado na camada ativa. */
    fun registrar(traco: Traco) = registrarGrupo(listOf(traco))

    fun registrarGrupo(tracos: List<Traco>) {
        if (tracos.isEmpty()) return
        historico.add(AcaoTracos(camadaAtiva.id, tracos.toList()))
        pilhaRefazer.clear()
        composicaoSuja = true
        atualizarComposicao()
        aoMudar?.invoke()
    }

    /** Adiciona um traço ao bitmap ativo e regista-o como uma ação anulável. */
    fun aplicarTraco(traco: Traco) {
        desenharTraco(camadaAtiva.tela, traco)
        registrar(traco)
    }

    fun desfazer() {
        if (historico.isEmpty()) return
        val acao = historico.removeAt(historico.lastIndex)
        pilhaRefazer.add(acao)
        reconstruirCamada(acao.camadaId)
        composicaoSuja = true
        atualizarComposicao()
        aoMudar?.invoke()
    }

    fun refazer() {
        if (pilhaRefazer.isEmpty()) return
        val acao = pilhaRefazer.removeAt(pilhaRefazer.lastIndex)
        historico.add(acao)
        reconstruirCamada(acao.camadaId)
        composicaoSuja = true
        atualizarComposicao()
        aoMudar?.invoke()
    }

    /** Reconstrói todas as camadas a partir da imagem base e do histórico aplicado. */
    fun reconstruir() {
        camadas.forEach { reconstruirCamada(it.id) }
        composicaoSuja = true
        atualizarComposicao()
        aoMudar?.invoke()
    }

    fun atualizarComposicao(forcar: Boolean = false) {
        if (!composicaoSuja && !forcar) return
        bitmap.eraseColor(Color.TRANSPARENT)
        for (camada in camadas) {
            if (!camada.visivel || camada.opacidade <= 0) continue
            pincelCamada.alpha = camada.opacidade.coerceIn(0, 255)
            pincelCamada.xfermode = when (camada.modoMesclagem) {
                ModoMesclagem.NORMAL -> null
                ModoMesclagem.MULTIPLICAR -> android.graphics.PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                ModoMesclagem.TELA -> android.graphics.PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
            telaComposicao.drawBitmap(camada.bitmap, 0f, 0f, pincelCamada)
        }
        pincelCamada.alpha = 255
        pincelCamada.xfermode = null
        composicaoSuja = false
    }

    fun adicionarCamada(): Boolean {
        if (camadas.size >= 12) return false
        adicionarCamadaInterna("Camada ${camadas.size + 1}")
        return true
    }

    private fun adicionarCamadaInterna(nome: String, notificar: Boolean = true) {
        val nova = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
        // Camadas vazias usam base nula: evita alocar um segundo bitmap completo por camada.
        camadas.add(CamadaDesenho(proximoIdCamada++, nome, nova, null))
        indiceCamadaAtiva = camadas.lastIndex
        composicaoSuja = true
        atualizarComposicao()
        if (notificar) aoMudar?.invoke()
    }

    fun selecionarCamada(indice: Int) {
        if (indice !in camadas.indices || indice == indiceCamadaAtiva) return
        indiceCamadaAtiva = indice
        aoMudar?.invoke()
    }

    fun definirVisibilidade(indice: Int, visivel: Boolean) {
        val camada = camadas.getOrNull(indice) ?: return
        if (camada.visivel == visivel) return
        camada.visivel = visivel
        composicaoSuja = true
        atualizarComposicao(); aoMudar?.invoke()
    }

    fun definirOpacidade(indice: Int, opacidade: Int) {
        val camada = camadas.getOrNull(indice) ?: return
        camada.opacidade = opacidade.coerceIn(0, 255)
        composicaoSuja = true
        atualizarComposicao(); aoMudar?.invoke()
    }

    fun definirModoMesclagem(indice: Int, modo: ModoMesclagem) {
        val camada = camadas.getOrNull(indice) ?: return
        if (camada.modoMesclagem == modo) return
        camada.modoMesclagem = modo
        composicaoSuja = true
        atualizarComposicao(); aoMudar?.invoke()
    }

    /** A lista é armazenada de baixo para cima; mover para cima aumenta o índice. */
    fun moverCamada(indice: Int, delta: Int) {
        val destino = indice + delta
        if (indice !in camadas.indices || destino !in camadas.indices || indice == destino) return
        val idAtivo = camadaAtiva.id
        val camada = camadas.removeAt(indice)
        camadas.add(destino, camada)
        indiceCamadaAtiva = camadas.indexOfFirst { it.id == idAtivo }.coerceAtLeast(0)
        composicaoSuja = true
        atualizarComposicao(); aoMudar?.invoke()
    }

    fun removerCamada(indice: Int): Boolean {
        if (camadas.size <= 1 || indice !in camadas.indices) return false
        val idAtivo = camadaAtiva.id
        val removida = camadas.removeAt(indice)
        historico.removeAll { it.camadaId == removida.id }
        pilhaRefazer.removeAll { it.camadaId == removida.id }
        indiceCamadaAtiva = camadas.indexOfFirst { it.id == idAtivo }.let { if (it >= 0) it else indice.coerceAtMost(camadas.lastIndex) }
        if (!removida.bitmap.isRecycled) removida.bitmap.recycle()
        composicaoSuja = true
        atualizarComposicao(); aoMudar?.invoke()
        return true
    }

    /** Preenchimento por inundação. O array de visited impede ciclos e cada pixel entra uma vez. */
    fun preencher(x: Int, y: Int, cor: Int, tolerancia: Int = 18): Boolean {
        val camada = camadaAtiva
        val px = x.coerceIn(0, largura - 1)
        val py = y.coerceIn(0, altura - 1)
        val total = largura * altura
        val pixels = IntArray(total)
        camada.bitmap.getPixels(pixels, 0, largura, 0, 0, largura, altura)
        val alvo = pixels[py * largura + px]
        if (alvo == cor || coresSemelhantes(alvo, cor, 0)) return false

        val visitados = BooleanArray(total)
        val pilha = IntArray(total)
        val alterados = IntArray(total)
        var topo = 0
        var quantidade = 0
        val inicio = py * largura + px
        pilha[topo++] = inicio
        visitados[inicio] = true

        while (topo > 0) {
            val indice = pilha[--topo]
            if (!coresSemelhantes(pixels[indice], alvo, tolerancia)) continue
            alterados[quantidade++] = indice
            pixels[indice] = cor
            val linha = indice / largura
            val coluna = indice - linha * largura
            if (coluna > 0) topo = empilharSeSemelhante(indice - 1, alvo, tolerancia, pixels, visitados, pilha, topo)
            if (coluna + 1 < largura) topo = empilharSeSemelhante(indice + 1, alvo, tolerancia, pixels, visitados, pilha, topo)
            if (linha > 0) topo = empilharSeSemelhante(indice - largura, alvo, tolerancia, pixels, visitados, pilha, topo)
            if (linha + 1 < altura) topo = empilharSeSemelhante(indice + largura, alvo, tolerancia, pixels, visitados, pilha, topo)
        }
        if (quantidade == 0) return false
        camada.bitmap.setPixels(pixels, 0, largura, 0, 0, largura, altura)
        historico.add(AcaoPreenchimento(camada.id, alterados.copyOf(quantidade), cor))
        pilhaRefazer.clear()
        composicaoSuja = true
        atualizarComposicao(); aoMudar?.invoke()
        return true
    }

    private fun empilharSeSemelhante(
        indice: Int, alvo: Int, tolerancia: Int, pixels: IntArray,
        visitados: BooleanArray, pilha: IntArray, topoAtual: Int
    ): Int {
        if (!visitados[indice] && coresSemelhantes(pixels[indice], alvo, tolerancia)) {
            visitados[indice] = true
            pilha[topoAtual] = indice
            return topoAtual + 1
        }
        return topoAtual
    }

    private fun coresSemelhantes(a: Int, b: Int, tolerancia: Int): Boolean {
        val alphaA = Color.alpha(a)
        val alphaB = Color.alpha(b)
        // Pixels totalmente transparentes são uma classe própria: não atravessar
        // as bordas antialias do contorno ao preencher uma região transparente.
        if (alphaA == 0 || alphaB == 0) return alphaA == 0 && alphaB == 0
        return abs(alphaA - alphaB) <= tolerancia &&
            abs(Color.red(a) - Color.red(b)) <= tolerancia &&
            abs(Color.green(a) - Color.green(b)) <= tolerancia &&
            abs(Color.blue(a) - Color.blue(b)) <= tolerancia
    }

    fun obterCor(x: Int, y: Int, camadaComposta: Boolean = false): Int {
        val px = x.coerceIn(0, largura - 1)
        val py = y.coerceIn(0, altura - 1)
        atualizarComposicao()
        val amostrada = (if (camadaComposta) bitmap else camadaAtiva.bitmap).getPixel(px, py)
        if (!camadaComposta) return amostrada
        // Reflete a cor visível sobre o papel branco, incluindo transparência.
        val a = Color.alpha(amostrada)
        val r = (Color.red(amostrada) * a + 255 * (255 - a)) / 255
        val g = (Color.green(amostrada) * a + 255 * (255 - a)) / 255
        val b = (Color.blue(amostrada) * a + 255 * (255 - a)) / 255
        return Color.rgb(r, g, b)
    }

    /** Move o conteúdo da seleção na camada ativa e guarda um snapshot para undo/redo. */
    fun moverConteudoSelecionado(esquerda: Int, topo: Int, direita: Int, baixo: Int, dx: Int, dy: Int): Boolean {
        val l = floor(esquerda.toDouble()).toInt().coerceIn(0, largura)
        val t = floor(topo.toDouble()).toInt().coerceIn(0, altura)
        val r = ceil(direita.toDouble()).toInt().coerceIn(0, largura)
        val b = ceil(baixo.toDouble()).toInt().coerceIn(0, altura)
        if (r <= l || b <= t || (dx == 0 && dy == 0)) return false
        val camada = camadaAtiva
        val bufferAntes = java.io.ByteArrayOutputStream()
        if (!camada.bitmap.compress(Bitmap.CompressFormat.PNG, 100, bufferAntes)) return false
        val antesPng = bufferAntes.toByteArray()
        val recorte = Bitmap.createBitmap(camada.bitmap, l, t, r - l, b - t)
        val c = camada.tela
        c.drawRect(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat(), pincelLimpar)
        c.drawBitmap(recorte, l + dx.toFloat(), t + dy.toFloat(), null)
        recorte.recycle()
        val bufferSnapshot = java.io.ByteArrayOutputStream()
        if (!camada.bitmap.compress(Bitmap.CompressFormat.PNG, 100, bufferSnapshot)) {
            camada.bitmap.eraseColor(Color.TRANSPARENT)
            val antes = android.graphics.BitmapFactory.decodeByteArray(antesPng, 0, antesPng.size)
                ?: throw IllegalStateException("Não foi possível restaurar a seleção depois de uma falha.")
            camada.tela.drawBitmap(antes, 0f, 0f, null)
            antes.recycle()
            atualizarComposicao()
            return false
        }
        historico.add(AcaoBitmap(camada.id, bufferSnapshot.toByteArray()))
        pilhaRefazer.clear()
        composicaoSuja = true
        atualizarComposicao(); aoMudar?.invoke()
        return true
    }

    fun criarSnapshotCamadas(): List<DadosCamada> {
        atualizarComposicao()
        return camadas.map { camada ->
            DadosCamada(
                nome = camada.nome,
                bitmap = camada.bitmap.copy(Bitmap.Config.ARGB_8888, false),
                visivel = camada.visivel,
                opacidade = camada.opacidade,
                modoMesclagem = camada.modoMesclagem
            )
        }
    }

    fun criarSnapshotComposto(): Bitmap {
        atualizarComposicao()
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    /**
     * Substitui o documento aberto. Os bitmaps recebidos passam a ser propriedade do motor;
     * a base para desfazer é guardada comprimida em PNG para limitar o consumo de memória.
     */
    fun substituirCamadas(dados: List<DadosCamada>) {
        if (dados.isEmpty()) return
        require(dados.size <= 12) { "Este projeto excede o limite de 12 camadas da versão atual." }
        val aceites = dados
        val novasCamadas = ArrayList<CamadaDesenho>(aceites.size)
        var novoId = 1L
        for ((indice, dado) in aceites.withIndex()) {
            val atual = if (dado.bitmap.width == largura && dado.bitmap.height == altura) {
                dado.bitmap
            } else {
                val escalado = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
                Canvas(escalado).drawBitmap(dado.bitmap, null, android.graphics.Rect(0, 0, largura, altura), Paint(Paint.FILTER_BITMAP_FLAG))
                if (!dado.bitmap.isRecycled) dado.bitmap.recycle()
                escalado
            }
            val bytesBase = if (atual === dado.bitmap && dado.bitmapBasePng != null) {
                dado.bitmapBasePng
            } else {
                java.io.ByteArrayOutputStream().also { buffer ->
                    if (!atual.compress(Bitmap.CompressFormat.PNG, 100, buffer)) {
                        throw IllegalStateException("Não foi possível preparar a camada ${indice + 1} para o histórico.")
                    }
                }.toByteArray()
            }
            val camada = CamadaDesenho(
                novoId++, dado.nome.ifBlank { "Camada ${indice + 1}" }, atual, bytesBase,
                dado.visivel, dado.opacidade.coerceIn(0, 255), dado.modoMesclagem
            )
            novasCamadas.add(camada)
        }
        camadas.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
        camadas.clear(); camadas.addAll(novasCamadas)
        historico.clear(); pilhaRefazer.clear(); proximoIdCamada = novoId
        indiceCamadaAtiva = camadas.lastIndex
        composicaoSuja = true
        atualizarComposicao(); aoMudar?.invoke()
    }

    private fun reconstruirCamada(id: Long) {
        val camada = camadas.firstOrNull { it.id == id } ?: return
        camada.bitmap.eraseColor(Color.TRANSPARENT)
        camada.bitmapBasePng?.let { bytes ->
            val base = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalStateException("A imagem base da camada não pode ser reconstruída.")
            camada.tela.drawBitmap(base, 0f, 0f, null)
            base.recycle()
        }
        for (acao in historico) {
            if (acao.camadaId != id) continue
            when (acao) {
                is AcaoTracos -> acao.tracos.forEach { desenharTraco(camada.tela, it) }
                is AcaoPreenchimento -> aplicarPreenchimento(camada.bitmap, acao.indices, acao.cor)
                is AcaoBitmap -> {
                    camada.bitmap.eraseColor(Color.TRANSPARENT)
                    val depois = android.graphics.BitmapFactory.decodeByteArray(acao.depoisPng, 0, acao.depoisPng.size)
                        ?: throw IllegalStateException("O estado da seleção não pode ser reconstruído.")
                    camada.tela.drawBitmap(depois, 0f, 0f, null)
                    depois.recycle()
                }
            }
        }
    }

    private fun aplicarPreenchimento(alvo: Bitmap, indices: IntArray, cor: Int) {
        val px = IntArray(largura * altura)
        alvo.getPixels(px, 0, largura, 0, 0, largura, altura)
        for (i in indices) if (i in px.indices) px[i] = cor
        alvo.setPixels(px, 0, largura, 0, 0, largura, altura)
    }

    private fun desenharTraco(canvas: Canvas, traco: Traco) {
        val pontos = traco.pontos
        if (pontos.tamanho == 0) return
        val r = RenderizadorTraco(canvas, traco)
        r.iniciar(pontos.x(0), pontos.y(0))
        for (i in 1 until pontos.tamanho) r.adicionar(pontos.x(i), pontos.y(i))
        r.finalizar()
    }
}
