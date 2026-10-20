package br.com.fiap.letssign.data.file

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Salva as imagens de assinatura no armazenamento INTERNO do app
 * (outros apps não têm acesso a essa pasta).
 */
@Singleton
class SignatureFileStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val folder: File
        get() = File(context.filesDir, "signatures").apply { mkdirs() }

    /**
     * Cada versão ganha um nome novo (com data/hora), em vez de sobrescrever a anterior.
     * Assim, documentos já assinados continuam apontando para a imagem da época.
     */
    suspend fun save(userId: Long, tipo: String, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val file = File(folder, "${userId}_${tipo}_${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.absolutePath
        }
}