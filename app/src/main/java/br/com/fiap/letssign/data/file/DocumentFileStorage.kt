package br.com.fiap.letssign.data.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Informações do PDF escolhido, usadas na prévia antes de criar o documento. */
data class PdfInfo(
    val nomeArquivo: String,
    val tamanhoBytes: Long,
    val paginas: Int,
    val capa: Bitmap
)

/** Erro "amigável": a mensagem já está pronta para mostrar ao usuário. */
class PdfInvalidoException(mensagem: String) : Exception(mensagem)

@Singleton
class DocumentFileStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val TAMANHO_MAXIMO = 20L * 1024 * 1024 // 20 MB
    }

    private val folder: File
        get() = File(context.filesDir, "documents").apply { mkdirs() }

    /**
     * Analisa o PDF escolhido SEM copiar: confere tamanho, se é PDF de verdade,
     * se abre (não corrompido / sem senha) e gera a miniatura da 1ª página.
     */
    suspend fun inspect(uri: Uri): PdfInfo = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val nome = getDisplayName(uri) ?: "documento.pdf"

        val pfd = try {
            resolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            null
        } ?: throw PdfInvalidoException("Não foi possível abrir o arquivo selecionado.")

        try {
            val tamanho = pfd.statSize
            if (tamanho == 0L) throw PdfInvalidoException("O arquivo está vazio.")
            if (tamanho > TAMANHO_MAXIMO) throw PdfInvalidoException("O arquivo passa de 20 MB.")

            // Todo PDF verdadeiro começa com os bytes "%PDF-"
            val cabecalho = ByteArray(5)
            resolver.openInputStream(uri)?.use { it.read(cabecalho) }
            if (String(cabecalho) != "%PDF-") {
                throw PdfInvalidoException("O arquivo não é um PDF válido.")
            }

            val renderer = try {
                PdfRenderer(pfd)
            } catch (e: SecurityException) {
                throw PdfInvalidoException("O PDF está protegido por senha. Remova a senha e tente novamente.")
            } catch (e: IOException) {
                throw PdfInvalidoException("O arquivo está corrompido ou não pôde ser processado.")
            }

            renderer.use { r ->
                if (r.pageCount == 0) throw PdfInvalidoException("O PDF não tem páginas.")
                PdfInfo(
                    nomeArquivo = nome,
                    tamanhoBytes = tamanho,
                    paginas = r.pageCount,
                    capa = renderPage(r, 0, 600)
                )
            }
        } finally {
            runCatching { pfd.close() }
        }
    }

    /** Copia o PDF para a pasta INTERNA do app (o original no celular não é alterado). */
    suspend fun importPdf(uri: Uri): File = withContext(Dispatchers.IO) {
        val destino = File(folder, "doc_${System.currentTimeMillis()}.pdf")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Não foi possível ler o arquivo")
        input.use { entrada -> destino.outputStream().use { saida -> entrada.copyTo(saida) } }
        destino
    }

    /** Desenha uma página do PDF como imagem, com a largura pedida (em pixels). */
    fun renderPage(file: File, pageIndex: Int, larguraPx: Int): Bitmap {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return PdfRenderer(pfd).use { r -> renderPage(r, pageIndex, larguraPx) }
    }

    private fun renderPage(renderer: PdfRenderer, pageIndex: Int, larguraPx: Int): Bitmap =
        renderer.openPage(pageIndex).use { page ->
            val escala = larguraPx.toFloat() / page.width
            val bitmap = Bitmap.createBitmap(
                larguraPx,
                (page.height * escala).toInt(),
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.WHITE) // PDF sem fundo ficaria transparente
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }

    private fun getDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
}