package br.com.fiap.letssign.data.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import br.com.fiap.letssign.R
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.domain.model.CarimboSnapshot
import br.com.fiap.letssign.domain.model.SignatureStyle
import br.com.fiap.letssign.util.DateFormatter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Uma linha do histórico, já formatada para o manifesto. */
data class LinhaManifesto(val dataHora: String, val descricao: String)

/** Dados da página de manifesto (adicionada quando o documento é concluído). */
data class Manifesto(
    val tituloDocumento: String,
    val nomeArquivo: String,
    val paginas: Int,
    val hashOriginal: String,
    val assinaturas: List<CarimboSnapshot>,
    val eventos: List<LinhaManifesto>
)

/**
 * Gera uma CÓPIA do PDF original com os carimbos de assinatura nos campos.
 * O PDF original nunca é alterado (ele é a "prova" do que foi enviado).
 */
@Singleton
class PdfStamper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Cor "Tinta" (#0B1726) em valores de 0 a 1, como o PDF usa
    private val tintaR = 0.043f
    private val tintaG = 0.090f
    private val tintaB = 0.149f

    private val pasta: File
        get() = File(context.filesDir, "documents/assinados").apply { mkdirs() }

    fun gerar(
        documentId: Long,
        original: File,
        campos: List<SignatureFieldEntity>,
        assinaturas: Map<Long, CarimboSnapshot>,   // chave = id do participante (signerId)
        manifesto: Manifesto?
    ): File {
        val destino = File(pasta, "doc_${documentId}_${System.currentTimeMillis()}.pdf")

        PDDocument.load(original).use { pdf ->
            val fonte: PDFont = PDType1Font.HELVETICA
            val negrito: PDFont = PDType1Font.HELVETICA_BOLD
            // A fonte cursiva só é carregada se alguém usou assinatura digitada
            val cursiva: PDFont by lazy {
                context.resources.openRawResource(R.font.greatvibes_regular).use { PDType0Font.load(pdf, it) }
            }

            campos.forEach { campo ->
                val carimbo = assinaturas[campo.signerId] ?: return@forEach // ainda não assinou
                if (campo.pagina >= pdf.numberOfPages) return@forEach

                val pagina = pdf.getPage(campo.pagina)
                val caixa = pagina.cropBox
                val larguraPagina = caixa.width
                val alturaPagina = caixa.height

                // De proporção (0 a 1, origem no TOPO) para pontos do PDF (origem EMBAIXO)
                val x = caixa.lowerLeftX + campo.x * larguraPagina
                val largura = campo.largura * larguraPagina
                val altura = campo.altura * alturaPagina
                val y = caixa.lowerLeftY + alturaPagina * (1f - campo.y - campo.altura)

                PDPageContentStream(pdf, pagina, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    desenharCarimbo(pdf, cs, carimbo, x, y, largura, altura, fonte, negrito) { cursiva }
                }
            }

            if (manifesto != null) adicionarManifesto(pdf, manifesto, fonte, negrito)
            pdf.save(destino)
        }
        return destino
    }

    // ======================= Carimbo no campo =======================

    private fun desenharCarimbo(
        pdf: PDDocument,
        cs: PDPageContentStream,
        c: CarimboSnapshot,
        x: Float, y: Float, w: Float, h: Float,
        fonte: PDFont, negrito: PDFont,
        cursiva: () -> PDFont
    ) {
        val margem = h * 0.06f
        val alturaTexto = h * 0.38f              // parte de baixo: textos
        val alturaVisual = h - alturaTexto - margem // parte de cima: assinatura
        val yVisual = y + alturaTexto

        // 1. A assinatura (imagem desenhada ou texto cursivo)
        val imagem = c.imagemPath
            ?.takeIf { c.estilo != SignatureStyle.DIGITADA && File(it).exists() }
            ?.let { BitmapFactory.decodeFile(it) }
            ?.let { recortarTransparente(it) }

        if (imagem != null) {
            val img = LosslessFactory.createFromImage(pdf, imagem)
            val escala = minOf((w - 2 * margem) / imagem.width, alturaVisual / imagem.height)
            val iw = imagem.width * escala
            val ih = imagem.height * escala
            cs.drawImage(img, x + margem, yVisual + (alturaVisual - ih) / 2, iw, ih)
        } else {
            val f = cursiva()
            val texto = seguro(f, c.textoDigitado ?: c.nome)
            var tamanho = alturaVisual * 0.85f
            while (tamanho > 4f && larguraTexto(f, texto, tamanho) > w - 2 * margem) tamanho -= 0.5f
            escrever(cs, f, texto, tamanho, x + margem, yVisual + alturaVisual * 0.2f)
        }

        // 2. Linha de base
        cs.setStrokingColor(tintaR, tintaG, tintaB)
        cs.setLineWidth(0.4f)
        cs.moveTo(x + margem, y + alturaTexto)
        cs.lineTo(x + w - margem, y + alturaTexto)
        cs.stroke()

        // 3. Textos do carimbo
        val tamanhoTexto = (alturaTexto / 2.6f).coerceIn(3.5f, 7f)
        linhaQueCabe(
            cs, negrito,
            listOfNotNull(c.nome, c.cargo, c.identificacao).joinToString(" · "),
            tamanhoTexto, x + margem, y + alturaTexto - tamanhoTexto * 1.1f, w - 2 * margem
        )
        linhaQueCabe(
            cs, fonte,
            "Assinado em ${DateFormatter.dataHora(c.assinadoEm)} · cód. ${c.codigo}",
            tamanhoTexto * 0.9f, x + margem, y + alturaTexto - tamanhoTexto * 2.3f, w - 2 * margem
        )
    }

    /** Corta as bordas transparentes do desenho, para a assinatura ocupar bem o campo. */
    private fun recortarTransparente(bmp: Bitmap): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (py in 0 until h) {
            for (px in 0 until w) {
                if ((pixels[py * w + px] ushr 24) > 0) {
                    if (px < minX) minX = px
                    if (px > maxX) maxX = px
                    if (py < minY) minY = py
                    if (py > maxY) maxY = py
                }
            }
        }
        if (maxX < 0) return bmp
        val folga = 8
        minX = (minX - folga).coerceAtLeast(0)
        minY = (minY - folga).coerceAtLeast(0)
        maxX = (maxX + folga).coerceAtMost(w - 1)
        maxY = (maxY + folga).coerceAtMost(h - 1)
        return Bitmap.createBitmap(bmp, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    // ======================= Página de manifesto =======================

    private fun adicionarManifesto(pdf: PDDocument, m: Manifesto, fonte: PDFont, negrito: PDFont) {
        val margem = 50f
        val larguraUtil = PDRectangle.A4.width - 2 * margem
        var pagina = PDPage(PDRectangle.A4)
        pdf.addPage(pagina)
        var cs = PDPageContentStream(pdf, pagina)
        var y = PDRectangle.A4.height - margem

        fun garantirEspaco(espaco: Float) {
            if (y - espaco < margem) {
                cs.close()
                pagina = PDPage(PDRectangle.A4)
                pdf.addPage(pagina)
                cs = PDPageContentStream(pdf, pagina)
                y = PDRectangle.A4.height - margem
            }
        }

        fun paragrafo(texto: String, f: PDFont = fonte, tamanho: Float = 10f) {
            quebrarEmLinhas(texto, f, tamanho, larguraUtil).forEach { linha ->
                garantirEspaco(tamanho * 1.5f)
                y -= tamanho * 1.5f
                escrever(cs, f, linha, tamanho, margem, y)
            }
        }

        fun espaco(valor: Float = 8f) {
            y -= valor
        }

        fun separador() {
            garantirEspaco(12f)
            y -= 6f
            cs.setStrokingColor(0.8f, 0.83f, 0.88f)
            cs.setLineWidth(0.6f)
            cs.moveTo(margem, y)
            cs.lineTo(margem + larguraUtil, y)
            cs.stroke()
            y -= 6f
        }

        paragrafo("MANIFESTO DE ASSINATURAS", negrito, 16f)
        paragrafo("Ratifica · Registro de autenticidade e integridade do documento", fonte, 9f)
        separador()

        paragrafo("Documento: ${m.tituloDocumento}", negrito, 11f)
        paragrafo("Arquivo: ${m.nomeArquivo} · ${m.paginas} página(s)", fonte, 10f)
        espaco()
        paragrafo("Impressão digital (SHA-256) do documento original:", fonte, 9f)
        paragrafo(m.hashOriginal, fonte, 8f)
        separador()

        paragrafo("ASSINATURAS (${m.assinaturas.size})", negrito, 12f)
        m.assinaturas.forEach { a ->
            espaco(4f)
            paragrafo("${a.nome} — ${a.papel}", negrito, 10f)
            paragrafo("E-mail: ${a.email} · ${a.identificacao}", fonte, 9f)
            paragrafo("Empresa: ${a.empresa}" + (a.cargo?.let { " · Cargo: $it" } ?: ""), fonte, 9f)
            paragrafo(
                "Assinado em ${DateFormatter.dataHora(a.assinadoEm)} · Dispositivo: ${a.dispositivo} · Código: ${a.codigo}",
                fonte, 9f
            )
            paragrafo("Autenticação: confirmação de senha e aceite no momento da assinatura.", fonte, 8f)
        }
        separador()

        paragrafo("HISTÓRICO DO DOCUMENTO", negrito, 12f)
        m.eventos.forEach { e -> paragrafo("${e.dataHora} — ${e.descricao}", fonte, 9f) }
        separador()

        paragrafo(
            "Este manifesto é parte integrante do documento. Qualquer alteração no conteúdo original " +
                    "altera a impressão digital acima e pode ser verificada no aplicativo Ratifica.",
            fonte, 8f
        )
        cs.close()
    }

    // ======================= Utilitários de texto =======================

    private fun escrever(cs: PDPageContentStream, f: PDFont, texto: String, tamanho: Float, x: Float, y: Float) {
        cs.beginText()
        cs.setNonStrokingColor(tintaR, tintaG, tintaB)
        cs.setFont(f, tamanho)
        cs.newLineAtOffset(x, y)
        cs.showText(texto)
        cs.endText()
    }

    /** Escreve uma linha; se não couber, corta e coloca "…". */
    private fun linhaQueCabe(
        cs: PDPageContentStream, f: PDFont, textoOriginal: String,
        tamanho: Float, x: Float, y: Float, larguraMax: Float
    ) {
        var texto = seguro(f, textoOriginal)
        while (texto.length > 1 && larguraTexto(f, texto, tamanho) > larguraMax) {
            texto = texto.dropLast(2) + "…"
        }
        escrever(cs, f, texto, tamanho, x, y)
    }

    private fun quebrarEmLinhas(texto: String, f: PDFont, tamanho: Float, larguraMax: Float): List<String> {
        val linhas = mutableListOf<String>()
        var atual = ""
        seguro(f, texto).split(" ").forEach { palavra ->
            val tentativa = if (atual.isEmpty()) palavra else "$atual $palavra"
            if (larguraTexto(f, tentativa, tamanho) <= larguraMax) {
                atual = tentativa
            } else {
                if (atual.isNotEmpty()) linhas += atual
                atual = palavra
            }
        }
        if (atual.isNotEmpty()) linhas += atual
        return linhas
    }

    private fun larguraTexto(f: PDFont, texto: String, tamanho: Float): Float =
        f.getStringWidth(texto) / 1000f * tamanho

    /** Troca por "?" caracteres que a fonte do PDF não consegue desenhar (ex.: emojis). */
    private fun seguro(f: PDFont, texto: String): String = buildString {
        texto.forEach { c -> append(if (runCatching { f.encode(c.toString()) }.isSuccess) c else '?') }
    }
}