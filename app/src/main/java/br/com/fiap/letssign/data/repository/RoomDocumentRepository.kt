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
import br.com.fiap.letssign.data.local.dao.SendRequestDao
import br.com.fiap.letssign.data.local.entity.SendRequestEntity
import br.com.fiap.letssign.domain.model.EnvioResultado
import br.com.fiap.letssign.domain.model.SendStatus
import java.io.IOException

class RoomDocumentRepository @Inject constructor(
    private val database: AppDatabase,
    private val documentDao: DocumentDao,
    private val auditEventDao: AuditEventDao,
    private val signerDao: DocumentSignerDao,
    private val fieldDao: SignatureFieldDao,
    private val sendRequestDao: SendRequestDao,
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
        larguraPx: Int,
        versaoFinal: Boolean
    ): Bitmap? = withContext(Dispatchers.IO) {
        val doc = getDocument(documentId, userId) ?: return@withContext null
        val final = doc.caminhoArquivoFinal
        val caminho = if (versaoFinal && final != null) final else doc.caminhoArquivo
        runCatching { storage.renderPage(File(caminho), pagina, larguraPx) }.getOrNull()
    }

    // =================== Etapa 4: envio ===================

    /** Regras de negócio conferidas ANTES de enviar (a tela também confere, mas aqui é a garantia). */
    private fun validarEnvio(
        doc: DocumentEntity,
        participantes: List<DocumentSignerEntity>,
        campos: List<SignatureFieldEntity>
    ) {
        if (doc.titulo.isBlank()) {
            throw IllegalArgumentException("Informe o título do documento.")
        }
        if (participantes.isEmpty()) {
            throw IllegalArgumentException("Adicione pelo menos um signatário.")
        }
        if (participantes.none { it.obrigatorio }) {
            throw IllegalArgumentException("Marque pelo menos um signatário como obrigatório.")
        }
        if (participantes.any { p -> campos.none { it.signerId == p.id } }) {
            throw IllegalArgumentException("Há signatários sem campo de assinatura.")
        }
        campos.forEach { validarCampo(it, doc) }

        // Integridade: o PDF precisa ser exatamente o mesmo do upload
        val arquivo = File(doc.caminhoArquivo)
        if (!arquivo.exists() || FileHasher.sha256(arquivo) != doc.hashOriginal) {
            throw IllegalStateException(
                "O arquivo do documento foi alterado ou está corrompido. Crie o documento novamente."
            )
        }
    }

    override suspend fun sendDocument(
        ownerId: Long,
        documentId: Long,
        mensagem: String?,
        idempotencyKey: String,
        simularFalha: Boolean
    ): Result<EnvioResultado> = withContext(Dispatchers.IO) {

        // 1. Esta solicitação já foi concluída antes? Então não faz nada de novo.
        val anterior = sendRequestDao.getByKey(idempotencyKey)
        if (anterior != null && anterior.status == SendStatus.SUCESSO) {
            return@withContext Result.success(
                EnvioResultado(jaEnviadoAntes = true, totalSignatarios = signerDao.getByDocument(documentId).size)
            )
        }

        // 2. Registra a solicitação (só na primeira tentativa) e conta a tentativa
        if (anterior == null) {
            sendRequestDao.insert(SendRequestEntity(documentId = documentId, idempotencyKey = idempotencyKey))
        }
        val solicitacao = sendRequestDao.getByKey(idempotencyKey)
            ?: return@withContext Result.failure(Exception("Não foi possível registrar o envio."))
        val tentativa = solicitacao.copy(
            tentativas = solicitacao.tentativas + 1,
            ultimaTentativaEm = System.currentTimeMillis()
        )

        try {
            // 3. Valida tudo
            val doc = garantirRascunho(documentId, ownerId)
            val participantes = signerDao.getByDocument(documentId)
            val campos = fieldDao.getByDocument(documentId)
            validarEnvio(doc, participantes, campos)

            if (simularFalha) throw IOException("Falha simulada de conexão")

            // 4. Envia: status + auditoria + solicitação, tudo na MESMA transação
            val agora = System.currentTimeMillis()
            database.withTransaction {
                documentDao.update(
                    doc.copy(
                        status = DocumentStatus.ENVIADO,
                        enviadoEm = agora,
                        mensagemConvite = mensagem?.trim()?.ifBlank { null }
                    )
                )
                auditEventDao.insert(
                    AuditEventEntity(
                        documentId = documentId,
                        userId = ownerId,
                        tipo = AuditEventType.ENVIADO,
                        dataHora = agora,
                        detalhe = "Enviado para ${participantes.size} signatário(s)"
                    )
                )
                sendRequestDao.update(tentativa.copy(status = SendStatus.SUCESSO, erro = null))
            }
            Result.success(EnvioResultado(jaEnviadoAntes = false, totalSignatarios = participantes.size))

        } catch (e: Exception) {
            val mensagemErro = when (e) {
                is IllegalArgumentException, is IllegalStateException -> e.message
                is IOException -> "Não foi possível concluir o envio. Verifique sua conexão e tente novamente."
                else -> "Ocorreu um erro inesperado ao enviar. Tente novamente."
            } ?: "Não foi possível enviar."

            // A falha também fica registrada (útil para auditoria e suporte)
            runCatching { sendRequestDao.update(tentativa.copy(status = SendStatus.FALHA, erro = mensagemErro)) }
            Result.failure(Exception(mensagemErro))
        }

    }

    override fun observeAuditEvents(documentId: Long): Flow<List<AuditEventEntity>> =
        auditEventDao.observeByDocument(documentId)

    // =================== Etapa 1: editar ou descartar rascunho ===================

    override suspend fun getDraftPreview(documentId: Long, ownerId: Long): PdfInfo? =
        withContext(Dispatchers.IO) {
            val doc = documentDao.getById(documentId) ?: return@withContext null
            if (doc.ownerId != ownerId || doc.status != DocumentStatus.RASCUNHO) return@withContext null
            val arquivo = File(doc.caminhoArquivo)
            if (!arquivo.exists()) return@withContext null
            runCatching {
                PdfInfo(
                    nomeArquivo = doc.nomeArquivo,
                    tamanhoBytes = arquivo.length(),
                    paginas = doc.quantidadePaginas,
                    capa = storage.renderPage(arquivo, 0, 600)
                )
            }.getOrNull()
        }

    override suspend fun updateDraft(
        ownerId: Long,
        documentId: Long,
        titulo: String,
        novoArquivo: Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var arquivoCopiado: File? = null
        try {
            val doc = garantirRascunho(documentId, ownerId)

            // Só o título mudou
            if (novoArquivo == null) {
                documentDao.update(doc.copy(titulo = titulo.trim()))
                return@withContext Result.success(Unit)
            }

            // Troca de arquivo: valida, copia e calcula o novo hash
            val info = storage.inspect(novoArquivo)
            val copia = storage.importPdf(novoArquivo)
            arquivoCopiado = copia
            val hash = FileHasher.sha256(copia)

            database.withTransaction {
                fieldDao.deleteByDocument(documentId) // as páginas podem ser outras
                documentDao.update(
                    doc.copy(
                        titulo = titulo.trim(),
                        nomeArquivo = info.nomeArquivo,
                        caminhoArquivo = copia.absolutePath,
                        quantidadePaginas = info.paginas,
                        hashOriginal = hash
                    )
                )
                auditEventDao.insert(
                    AuditEventEntity(
                        documentId = documentId,
                        userId = ownerId,
                        tipo = AuditEventType.NOVA_VERSAO,
                        detalhe = "Arquivo do rascunho substituído por ${info.nomeArquivo}"
                    )
                )
            }
            runCatching { File(doc.caminhoArquivo).delete() } // o arquivo antigo não é mais usado
            Result.success(Unit)

        } catch (e: PdfInvalidoException) {
            arquivoCopiado?.delete()
            Result.failure(e)
        } catch (e: IllegalStateException) {
            arquivoCopiado?.delete()
            Result.failure(e)
        } catch (e: Exception) {
            arquivoCopiado?.delete()
            Result.failure(Exception("Não foi possível salvar as alterações. Tente novamente."))
        }
    }

    override suspend fun deleteDraft(ownerId: Long, documentId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val doc = garantirRascunho(documentId, ownerId) // só rascunho pode ser apagado
                documentDao.delete(doc)
                runCatching { File(doc.caminhoArquivo).delete() }
                Result.success(Unit)
            } catch (e: IllegalStateException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(Exception("Não foi possível descartar o rascunho."))
            }
        }
}