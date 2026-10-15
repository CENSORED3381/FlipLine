package com.huzeifa.flipline.draw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Armazenamento SAF: os projetos .flipline são ZIPs com metadados e um PNG por camada. */
object ProjetosFlipline {
    private const val FORMATO = 1
    private const val MAX_CAMADAS = 12
    private const val MAX_BYTES_ENTRADA = 120 * 1024 * 1024
    private const val MAX_BYTES_IMAGEM = 10 * 1024 * 1024

    fun salvarProjeto(context: Context, uri: Uri, largura: Int, altura: Int, camadas: List<DadosCamada>) {
        require(camadas.isNotEmpty() && camadas.size <= MAX_CAMADAS) { "O projeto tem uma quantidade inválida de camadas." }
        val jsonCamadas = JSONArray()
        camadas.forEachIndexed { indice, camada ->
            jsonCamadas.put(JSONObject().apply {
                put("name", camada.nome.take(80))
                put("visible", camada.visivel)
                put("opacity", camada.opacidade.coerceIn(0, 255))
                put("blend", camada.modoMesclagem.name)
                put("file", "layer_$indice.png")
            })
        }
        val documento = JSONObject().apply {
            put("formatVersion", FORMATO)
            put("width", largura)
            put("height", altura)
            put("layers", jsonCamadas)
        }.toString().toByteArray(Charsets.UTF_8)

        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IllegalStateException("Não foi possível abrir o destino para guardar o projeto.")
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry("document.json"))
            zip.write(documento)
            zip.closeEntry()
            camadas.forEachIndexed { indice, camada ->
                zip.putNextEntry(ZipEntry("layer_$indice.png"))
                val buffer = ByteArrayOutputStream()
                if (!camada.bitmap.compress(Bitmap.CompressFormat.PNG, 100, buffer)) {
                    throw IllegalStateException("Não foi possível codificar a camada ${indice + 1}.")
                }
                zip.write(buffer.toByteArray())
                zip.closeEntry()
            }
        }
    }

    fun exportarPng(context: Context, uri: Uri, bitmap: Bitmap) {
        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IllegalStateException("Não foi possível abrir o destino para exportar o PNG.")
        output.use {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                throw IllegalStateException("A exportação PNG falhou.")
            }
            it.flush()
        }
    }

    fun abrirProjeto(context: Context, uri: Uri): List<DadosCamada> {
        val entradas = HashMap<String, ByteArray>()
        var total = 0L
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Não foi possível ler o ficheiro selecionado.")
        ZipInputStream(input).use { zip ->
            var quantidadeEntradas = 0
            while (true) {
                val entrada = zip.nextEntry ?: break
                quantidadeEntradas++
                if (quantidadeEntradas > MAX_CAMADAS + 1) throw IllegalArgumentException("O projeto contém demasiadas entradas.")
                val nome = entrada.name
                if (nome.contains("..") || nome.startsWith("/") || nome.contains('\\')) {
                    throw IllegalArgumentException("O projeto contém um caminho inválido.")
                }
                if (nome != "document.json" && !nome.matches(Regex("layer_[0-9]{1,2}\\.png"))) {
                    throw IllegalArgumentException("O projeto contém uma entrada não suportada: $nome")
                }
                if (entradas.containsKey(nome)) throw IllegalArgumentException("O projeto contém entradas duplicadas.")
                val limite = if (nome == "document.json") 128 * 1024 else MAX_BYTES_IMAGEM
                val bytes = lerLimitado(zip, limite)
                total += bytes.size
                if (total > MAX_BYTES_ENTRADA) throw IllegalArgumentException("O projeto é demasiado grande.")
                entradas[nome] = bytes
                zip.closeEntry()
            }
        }
        val dadosJson = entradas["document.json"] ?: throw IllegalArgumentException("O ficheiro não é um projeto FlipLine válido.")
        val json = JSONObject(String(dadosJson, Charsets.UTF_8))
        if (json.optInt("formatVersion", -1) != FORMATO) throw IllegalArgumentException("Versão de projeto não suportada.")
        val largura = json.optInt("width", 0)
        val altura = json.optInt("height", 0)
        // A Fase 3 trabalha com tela fixa de 1200x1600; rejeitar dimensões externas
        // evita alocações exageradas ao abrir arquivos ZIP não confiáveis.
        if (largura != 1200 || altura != 1600) {
            throw IllegalArgumentException("Dimensões não suportadas. Esta versão usa uma tela de 1200 x 1600.")
        }
        val lista = json.optJSONArray("layers") ?: throw IllegalArgumentException("O projeto não contém camadas.")
        if (lista.length() !in 1..MAX_CAMADAS) throw IllegalArgumentException("O projeto deve conter entre 1 e $MAX_CAMADAS camadas.")
        if (entradas.size != lista.length() + 1) throw IllegalArgumentException("O projeto contém imagens de camada não referenciadas.")

        val resultado = ArrayList<DadosCamada>(lista.length())
        try {
        for (indice in 0 until lista.length()) {
            val camadaJson = lista.getJSONObject(indice)
            val esperado = "layer_$indice.png"
            if (camadaJson.optString("file") != esperado) throw IllegalArgumentException("Referência de camada inválida.")
            val bytes = entradas[esperado] ?: throw IllegalArgumentException("Falta a imagem da camada ${indice + 1}.")
            val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opcoes)
            if (opcoes.outWidth != largura || opcoes.outHeight != altura) {
                throw IllegalArgumentException("A camada ${indice + 1} não corresponde ao tamanho do documento.")
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inMutable = true
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: throw IllegalArgumentException("Não foi possível descodificar a camada ${indice + 1}.")
            resultado.add(
                DadosCamada(
                    nome = camadaJson.optString("name", "Camada ${indice + 1}").take(80),
                    bitmap = bitmap,
                    visivel = camadaJson.optBoolean("visible", true),
                    opacidade = camadaJson.optInt("opacity", 255).coerceIn(0, 255),
                    modoMesclagem = runCatching { ModoMesclagem.valueOf(camadaJson.optString("blend", "NORMAL")) }
                        .getOrDefault(ModoMesclagem.NORMAL),
                    bitmapBasePng = bytes
                )
            )
        }
        } catch (erro: Exception) {
            resultado.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
            throw erro
        }
        return resultado
    }

    private fun lerLimitado(input: ZipInputStream, maximo: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val quantidade = input.read(buffer)
            if (quantidade < 0) break
            total += quantidade
            if (total > maximo) throw IllegalArgumentException("Uma entrada do projeto excede o limite permitido.")
            output.write(buffer, 0, quantidade)
        }
        return output.toByteArray()
    }
}
