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
import br.com.fiap.letssign.data.local.dao.DocumentSignerDao
import br.com.fiap.letssign.data.local.entity.DocumentSignerEntity
import br.com.fiap.letssign.data.local.entity.SignerWithUser
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerRole
import br.com.fiap.letssign.data.local.entity.DocumentListItem
import android.graphics.Bitmap
import br.com.fiap.letssign.data.local.dao.SignatureFieldDao
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity

class RoomDocumentRepository @Inject constructor(
    private val database: AppDatabase,
    private val documentDao: DocumentDao,
    private val auditEventDao: AuditEventDao,
    private val signerDao: DocumentSignerDao,
    private val fieldDao: SignatureFieldDao,
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

    // =================== Etapa 2: participantes ===================


    /** Regra de negócio: só dá para mexer em documento do próprio admin e ainda em RASCUNHO. */
    private suspend fun garantirRascunho(documentId: Long, ownerId: Long): DocumentEntity {
        val doc = documentDao.getById(documentId)
        if (doc == null || doc.ownerId != ownerId) {
            throw IllegalStateException("Documento não encontrado.")
        }
        if (doc.status != DocumentStatus.RASCUNHO) {
            throw IllegalStateException("Documento já enviado não pode ser alterado. Crie uma nova versão.")
        }
        return doc
    }

    /** Executa uma alteração e transforma erros em mensagens amigáveis. */
    private suspend fun executar(bloco: suspend () -> Unit): Result<Unit> = try {
        bloco()
        Result.success(Unit)
    } catch (e: IllegalArgumentException) {
        Result.failure(e)
    } catch (e: IllegalStateException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(Exception("Não foi possível salvar. Tente novamente."))
    }

    override fun observeParticipants(documentId: Long): Flow<List<SignerWithUser>> =
        signerDao.observeWithUserByDocument(documentId)

    override suspend fun addParticipant(
        documentId: Long,
        ownerId: Long,
        userId: Long,
        papel: SignerRole
    ): Result<Unit> = executar {
        garantirRascunho(documentId, ownerId)
        val atuais = signerDao.getByDocument(documentId)
        if (atuais.any { it.userId == userId }) {
            throw IllegalArgumentException("Esta pessoa já está no documento.")
        }
        signerDao.insert(
            DocumentSignerEntity(
                documentId = documentId,
                userId = userId,
                papel = papel,
                ordem = atuais.size
            )
        )
    }

    override suspend fun updateParticipant(
        ownerId: Long,
        participante: DocumentSignerEntity
    ): Result<Unit> = executar {
        garantirRascunho(participante.documentId, ownerId)
        signerDao.update(participante)
    }

    override suspend fun removeParticipant(
        ownerId: Long,
        participante: DocumentSignerEntity
    ): Result<Unit> = executar {
        garantirRascunho(participante.documentId, ownerId)
        signerDao.delete(participante) // os campos de assinatura dele somem junto (CASCADE)
    }

    /** Substituir = trocar a PESSOA, mantendo o papel, a ordem e os campos já posicionados. */
    override suspend fun replaceParticipant(
        ownerId: Long,
        participanteId: Long,
        novoUserId: Long
    ): Result<Unit> = executar {
        val atual = signerDao.getById(participanteId)
            ?: throw IllegalStateException("Participante não encontrado.")
        garantirRascunho(atual.documentId, ownerId)
        if (signerDao.getByDocumentAndUser(atual.documentId, novoUserId) != null) {
            throw IllegalArgumentException("Esta pessoa já está no documento.")
        }
        signerDao.update(atual.copy(userId = novoUserId))
    }

    override fun observeMyDocumentsWithProgress(ownerId: Long): Flow<List<DocumentListItem>> =
        documentDao.observeByOwnerWithProgress(ownerId)

    // =================== Etapa 3: campos de assinatura ===================

    override fun observeFields(documentId: Long): Flow<List<SignatureFieldEntity>> =
        fieldDao.observeByDocument(documentId)

    /** Regra de negócio: o campo precisa estar numa página que existe e DENTRO dos limites dela. */
    private fun validarCampo(campo: SignatureFieldEntity, doc: DocumentEntity) {
        if (campo.pagina !in 0 until doc.quantidadePaginas) {
            throw IllegalArgumentException("Página inválida.")
        }
        val margem = 0.0001f // tolerância para arredondamentos
        val dentroDaPagina = campo.x >= 0f && campo.y >= 0f &&
                campo.largura > 0f && campo.altura > 0f &&
                campo.x + campo.largura <= 1f + margem &&
                campo.y + campo.altura <= 1f + margem
        if (!dentroDaPagina) {
            throw IllegalArgumentException("O campo precisa ficar dentro dos limites da página.")
        }
    }

    override suspend fun addField(ownerId: Long, campo: SignatureFieldEntity): Result<Long> = try {
        val doc = garantirRascunho(campo.documentId, ownerId)
        validarCampo(campo, doc)
        // Regra: todo campo pertence a um participante DESTE documento
        val participante = signerDao.getById(campo.signerId)
        if (participante == null || participante.documentId != campo.documentId) {
            throw IllegalArgumentException("Selecione um signatário deste documento.")
        }
        Result.success(fieldDao.insert(campo))
    } catch (e: IllegalArgumentException) {
        Result.failure(e)
    } catch (e: IllegalStateException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(Exception("Não foi possível salvar o campo."))
    }

    override suspend fun updateField(ownerId: Long, campo: SignatureFieldEntity): Result<Unit> = executar {
        val doc = garantirRascunho(campo.documentId, ownerId)
        validarCampo(campo, doc)
        fieldDao.update(campo)
    }

    override suspend fun deleteField(ownerId: Long, campo: SignatureFieldEntity): Result<Unit> = executar {
        garantirRascunho(campo.documentId, ownerId)
        fieldDao.delete(campo)
    }

    override suspend fun renderPage(
        documentId: Long,
        userId: Long,
        pagina: Int,
        larguraPx: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val doc = getDocument(documentId, userId) ?: return@withContext null
        runCatching { storage.renderPage(File(doc.caminhoArquivo), pagina, larguraPx) }.getOrNull()
    }
}