package br.com.fiap.letssign.data.repository

import android.net.Uri
import androidx.room.withTransaction
import br.com.fiap.letssign.data.file.DocumentFileStorage
import br.com.fiap.letssign.data.file.PdfInfo
import br.com.fiap.letssign.data.file.PdfInvalidoException
import br.com.fiap.letssign.data.local.AppDatabase
import br.com.fiap.letssign.data.local.dao.AuditEventDao
import br.com.fiap.letssign.data.local.dao.DocumentDao
import br.com.fiap.letssign.data.local.entity.AuditEventEntity
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.domain.model.AuditEventType
import br.com.fiap.letssign.domain.repository.DocumentRepository
import br.com.fiap.letssign.util.FileHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class RoomDocumentRepository @Inject constructor(
    private val database: AppDatabase,
    private val documentDao: DocumentDao,
    private val auditEventDao: AuditEventDao,
    private val storage: DocumentFileStorage
) : DocumentRepository {

    override fun observeMyDocuments(ownerId: Long): Flow<List<DocumentEntity>> =
        documentDao.observeByOwner(ownerId)

    override fun observeDocument(id: Long): Flow<DocumentEntity?> =
        documentDao.observeById(id)

    override suspend fun getDocument(id: Long, userId: Long): DocumentEntity? =
        if (documentDao.countAccess(id, userId) > 0) documentDao.getById(id) else null

    override suspend fun inspectPdf(uri: Uri): Result<PdfInfo> = try {
        Result.success(storage.inspect(uri))
    } catch (e: PdfInvalidoException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(Exception("Não foi possível processar o arquivo."))
    }

    override suspend fun createDraft(ownerId: Long, titulo: String, uri: Uri): Result<Long> =
        withContext(Dispatchers.IO) {
            var arquivo: File? = null
            try {
                // 1. Valida de novo (o arquivo pode ter mudado desde a prévia)
                val info = storage.inspect(uri)

                // 2. Copia para a pasta interna e calcula a "impressão digital"
                arquivo = storage.importPdf(uri)
                val hash = FileHasher.sha256(arquivo)

                // 3. Documento + evento de auditoria na MESMA transação:
                //    ou salva os dois, ou nenhum.
                val id = database.withTransaction {
                    val novoId = documentDao.insert(
                        DocumentEntity(
                            titulo = titulo.trim(),
                            ownerId = ownerId,
                            nomeArquivo = info.nomeArquivo,
                            caminhoArquivo = arquivo.absolutePath,
                            quantidadePaginas = info.paginas,
                            hashOriginal = hash
                        )
                    )
                    auditEventDao.insert(
                        AuditEventEntity(
                            documentId = novoId,
                            userId = ownerId,
                            tipo = AuditEventType.CRIADO,
                            detalhe = "Arquivo ${info.nomeArquivo} · ${info.paginas} página(s)"
                        )
                    )
                    novoId
                }
                Result.success(id)
            } catch (e: PdfInvalidoException) {
                arquivo?.delete()
                Result.failure(e)
            } catch (e: Exception) {
                arquivo?.delete()
                Result.failure(Exception("Não foi possível importar o arquivo. Tente novamente."))
            }
        }
}