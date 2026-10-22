package br.com.fiap.letssign.domain.repository

import android.net.Uri
import br.com.fiap.letssign.data.file.PdfInfo
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow
import br.com.fiap.letssign.data.local.entity.DocumentSignerEntity
import br.com.fiap.letssign.data.local.entity.SignerWithUser
import br.com.fiap.letssign.domain.model.SignerRole
import br.com.fiap.letssign.data.local.entity.DocumentListItem
import android.graphics.Bitmap
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity

interface DocumentRepository {
    fun observeMyDocuments(ownerId: Long): Flow<List<DocumentEntity>>
    fun observeDocument(id: Long): Flow<DocumentEntity?>

    /** Retorna o documento só se o usuário tiver acesso (dono ou signatário de doc enviado). */
    suspend fun getDocument(id: Long, userId: Long): DocumentEntity?

    /** Analisa o PDF escolhido (validação + miniatura), sem salvar nada. */
    suspend fun inspectPdf(uri: Uri): Result<PdfInfo>

    /** Etapa 1: copia o PDF, calcula o hash e cria o documento como RASCUNHO. */
    suspend fun createDraft(ownerId: Long, titulo: String, uri: Uri): Result<Long>

    // ----- Etapa 2: participantes (só enquanto o documento for RASCUNHO) -----
    fun observeParticipants(documentId: Long): Flow<List<SignerWithUser>>

    fun observeMyDocumentsWithProgress(ownerId: Long): Flow<List<DocumentListItem>>

    suspend fun addParticipant(documentId: Long, ownerId: Long, userId: Long, papel: SignerRole): Result<Unit>
    suspend fun updateParticipant(ownerId: Long, participante: DocumentSignerEntity): Result<Unit>
    suspend fun removeParticipant(ownerId: Long, participante: DocumentSignerEntity): Result<Unit>
    suspend fun replaceParticipant(ownerId: Long, participanteId: Long, novoUserId: Long): Result<Unit>

    // ----- Etapa 3: campos de assinatura -----
    fun observeFields(documentId: Long): Flow<List<SignatureFieldEntity>>
    suspend fun addField(ownerId: Long, campo: SignatureFieldEntity): Result<Long>
    suspend fun updateField(ownerId: Long, campo: SignatureFieldEntity): Result<Unit>
    suspend fun deleteField(ownerId: Long, campo: SignatureFieldEntity): Result<Unit>

    /** Desenha uma página do PDF como imagem (só para quem tem acesso ao documento). */
    suspend fun renderPage(documentId: Long, userId: Long, pagina: Int, larguraPx: Int): Bitmap?

}