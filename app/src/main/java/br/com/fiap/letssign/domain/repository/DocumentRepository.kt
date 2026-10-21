package br.com.fiap.letssign.domain.repository

import android.net.Uri
import br.com.fiap.letssign.data.file.PdfInfo
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeMyDocuments(ownerId: Long): Flow<List<DocumentEntity>>
    fun observeDocument(id: Long): Flow<DocumentEntity?>

    /** Retorna o documento só se o usuário tiver acesso (dono ou signatário de doc enviado). */
    suspend fun getDocument(id: Long, userId: Long): DocumentEntity?

    /** Analisa o PDF escolhido (validação + miniatura), sem salvar nada. */
    suspend fun inspectPdf(uri: Uri): Result<PdfInfo>

    /** Etapa 1: copia o PDF, calcula o hash e cria o documento como RASCUNHO. */
    suspend fun createDraft(ownerId: Long, titulo: String, uri: Uri): Result<Long>
}