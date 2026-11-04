package br.com.fiap.letssign.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.room.withTransaction
import br.com.fiap.letssign.data.file.DocumentFileStorage
import br.com.fiap.letssign.data.file.LinhaManifesto
import br.com.fiap.letssign.data.file.Manifesto
import br.com.fiap.letssign.data.file.PdfStamper
import br.com.fiap.letssign.data.local.AppDatabase
import br.com.fiap.letssign.data.local.dao.AuditEventDao
import br.com.fiap.letssign.data.local.dao.DocumentDao
import br.com.fiap.letssign.data.local.dao.DocumentSignerDao
import br.com.fiap.letssign.data.local.dao.SignatureFieldDao
import br.com.fiap.letssign.data.local.dao.SignatureProfileDao
import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.AuditEventEntity
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.DocumentSignerEntity
import br.com.fiap.letssign.data.local.entity.SignatureProfileEntity
import br.com.fiap.letssign.data.local.entity.SignerDocumentItem
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.model.AssinaturaResultado
import br.com.fiap.letssign.domain.model.AuditEventType
import br.com.fiap.letssign.domain.model.CarimboSnapshot
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignatureStyle
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.repository.SigningContext
import br.com.fiap.letssign.domain.repository.SigningRepository
import br.com.fiap.letssign.util.DateFormatter
import br.com.fiap.letssign.util.FileHasher
import br.com.fiap.letssign.util.PasswordHasher
import br.com.fiap.letssign.util.StampFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class RoomSigningRepository @Inject constructor(
    private val database: AppDatabase,
    private val documentDao: DocumentDao,
    private val signerDao: DocumentSignerDao,
    private val fieldDao: SignatureFieldDao,
    private val userDao: UserDao,
    private val profileDao: SignatureProfileDao,
    private val auditEventDao: AuditEventDao,
    private val stamper: PdfStamper,
    private val storage: DocumentFileStorage,
    @ApplicationContext private val context: Context
) : SigningRepository {

    override fun observeMyDocuments(userId: Long): Flow<List<SignerDocumentItem>> =
        documentDao.observeForSigner(userId)

    override suspend fun getSigningContext(documentId: Long, userId: Long): SigningContext? {
        if (documentDao.countAccess(documentId, userId) == 0) return null // segurança
        val doc = documentDao.getById(documentId) ?: return null
        val eu = signerDao.getByDocumentAndUser(documentId, userId) ?: return null
        return SigningContext(
            document = doc,
            eu = eu,
            meusCampos = fieldDao.getBySigner(eu.id),
            remetente = userDao.getById(doc.ownerId)?.nomeCompleto ?: "Administrador",
            perfil = profileDao.getByUserId(userId)
        )
    }

    /** Registra a 1ª visualização de cada signatário (vai para o histórico e o manifesto). */
    override suspend fun registerView(documentId: Long, userId: Long) {
        if (signerDao.getByDocumentAndUser(documentId, userId) == null) return
        if (auditEventDao.count(documentId, userId, AuditEventType.VISUALIZADO.name) > 0) return
        val nome = userDao.getById(userId)?.nomeCompleto ?: ""
        auditEventDao.insert(
            AuditEventEntity(
                documentId = documentId,
                userId = userId,
                tipo = AuditEventType.VISUALIZADO,
                detalhe = "Visualizado por $nome"
            )
        )
    }

    override suspend fun renderPage(
        documentId: Long,
        userId: Long,
        pagina: Int,
        larguraPx: Int,
        versaoAssinada: Boolean
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (documentDao.countAccess(documentId, userId) == 0) return@withContext null
        val doc = documentDao.getById(documentId) ?: return@withContext null
        val final = doc.caminhoArquivoFinal
        val caminho = if (versaoAssinada && final != null) final else doc.caminhoArquivo
        runCatching { storage.renderPage(File(caminho), pagina, larguraPx) }.getOrNull()
    }

    override suspend fun sign(
        documentId: Long,
        userId: Long,
        senha: String
    ): Result<AssinaturaResultado> = withContext(Dispatchers.IO) {
        try {
            // ---------- 1. Verificações ----------
            if (documentDao.countAccess(documentId, userId) == 0) {
                throw IllegalStateException("Documento não encontrado.")
            }
            val doc = documentDao.getById(documentId)
                ?: throw IllegalStateException("Documento não encontrado.")
            val eu = signerDao.getByDocumentAndUser(documentId, userId)
                ?: throw IllegalStateException("Você não é signatário deste documento.")

            if (eu.status == SignerStatus.ASSINADO) {
                return@withContext Result.success(
                    AssinaturaResultado(doc.status == DocumentStatus.CONCLUIDO, jaAssinadoAntes = true)
                )
            }
            if (doc.status != DocumentStatus.ENVIADO && doc.status != DocumentStatus.PARCIALMENTE_ASSINADO) {
                throw IllegalStateException("Este documento não está disponível para assinatura.")
            }

            val usuario = userDao.getById(userId) ?: throw IllegalStateException("Usuário não encontrado.")
            if (!PasswordHasher.verify(senha, usuario.senhaSalt, usuario.senhaHash)) {
                throw IllegalArgumentException("Senha incorreta.")
            }
            val perfil = profileDao.getByUserId(userId)
                ?: throw IllegalArgumentException("Crie sua assinatura antes de assinar.")

            // ---------- 2. Congela o carimbo ----------
            val agora = System.currentTimeMillis()
            val carimbo = criarCarimbo(doc, eu, usuario, perfil, agora)
            val euAssinado = eu.copy(
                status = SignerStatus.ASSINADO,
                assinadoEm = agora,
                carimboSnapshot = carimbo.toJson()
            )

            // ---------- 3. Situação depois desta assinatura ----------
            val participantes = signerDao.getByDocument(documentId).map { if (it.id == eu.id) euAssinado else it }
            val concluido = participantes.none { it.obrigatorio && it.status != SignerStatus.ASSINADO }
            val assinaturas = participantes
                .filter { it.status == SignerStatus.ASSINADO && it.carimboSnapshot != null }
                .associate { it.id to CarimboSnapshot.fromJson(it.carimboSnapshot!!) }

            val eventoAssinatura = AuditEventEntity(
                documentId = documentId,
                userId = userId,
                tipo = AuditEventType.ASSINADO,
                dataHora = agora,
                detalhe = "Assinado por ${perfil.nomeExibicao} (${eu.papel.label})"
            )
            val eventoConclusao = if (concluido) AuditEventEntity(
                documentId = documentId,
                userId = null,
                tipo = AuditEventType.CONCLUIDO,
                dataHora = agora,
                detalhe = "Todos os signatários obrigatórios assinaram"
            ) else null

            val manifesto = if (concluido) {
                val eventos = auditEventDao.getByDocument(documentId) + listOfNotNull(eventoAssinatura, eventoConclusao)
                Manifesto(
                    tituloDocumento = doc.titulo,
                    nomeArquivo = doc.nomeArquivo,
                    paginas = doc.quantidadePaginas,
                    hashOriginal = doc.hashOriginal,
                    assinaturas = assinaturas.values.sortedBy { it.assinadoEm },
                    eventos = eventos.sortedBy { it.dataHora }.map {
                        LinhaManifesto(
                            dataHora = DateFormatter.dataHora(it.dataHora),
                            descricao = listOfNotNull(it.tipo.label, it.detalhe).joinToString(" · ")
                        )
                    },
                    rubricaNasPaginas = doc.exigirRubrica
                )

            } else null

            // ---------- 4. Gera o PDF ANTES de gravar (se falhar, nada muda no banco) ----------
            val pdfGerado = stamper.gerar(
                documentId = documentId,
                original = File(doc.caminhoArquivo),
                campos = fieldDao.getByDocument(documentId),
                assinaturas = assinaturas,
                manifesto = manifesto,
                rubricaNasPaginas = doc.exigirRubrica
            )

            val hashFinal = if (concluido) FileHasher.sha256(pdfGerado) else null

            val versaoAnterior = doc.caminhoArquivoFinal

            // ---------- 5. Grava tudo de uma vez ----------
            database.withTransaction {
                signerDao.update(euAssinado)
                auditEventDao.insert(eventoAssinatura)
                if (eventoConclusao != null) auditEventDao.insert(eventoConclusao)
                documentDao.update(
                    doc.copy(
                        status = if (concluido) DocumentStatus.CONCLUIDO else DocumentStatus.PARCIALMENTE_ASSINADO,
                        caminhoArquivoFinal = pdfGerado.absolutePath,
                        hashFinal = hashFinal,
                        concluidoEm = if (concluido) agora else null
                    )
                )
            }

            // A versão parcial anterior não é mais necessária
            if (versaoAnterior != null && versaoAnterior != pdfGerado.absolutePath) {
                runCatching { File(versaoAnterior).delete() }
            }

            Result.success(AssinaturaResultado(documentoConcluido = concluido, jaAssinadoAntes = false))

        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível concluir a assinatura. Tente novamente."))
        }
    }

    /** Copia as imagens (assinatura e rubrica) para a pasta do documento: elas ficam "congeladas" ali. */
    private fun criarCarimbo(
        doc: DocumentEntity,
        eu: DocumentSignerEntity,
        usuario: UserEntity,
        perfil: SignatureProfileEntity,
        agora: Long
    ): CarimboSnapshot {
        val pasta = File(context.filesDir, "documents/carimbos").apply { mkdirs() }

        fun congelar(caminho: String?, tipo: String): String? {
            val arquivo = caminho?.let { File(it) } ?: return null
            if (!arquivo.exists()) return null
            val destino = File(pasta, "doc${doc.id}_signer${eu.id}_${tipo}_$agora.png")
            arquivo.copyTo(destino, overwrite = true)
            return destino.absolutePath
        }

        val imagemAssinatura = if (perfil.estiloAssinatura == SignatureStyle.DIGITADA) null
        else congelar(perfil.assinaturaPath, "assinatura")

        val iniciais = perfil.nomeExibicao.trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

        return CarimboSnapshot(
            nome = perfil.nomeExibicao,
            cargo = perfil.cargo,
            identificacao = StampFormatter.identificacao(perfil.tipoIdentificacao, perfil.numeroIdentificacao),
            empresa = perfil.empresa,
            papel = eu.papel.label,
            email = usuario.email,
            estilo = perfil.estiloAssinatura,
            imagemPath = imagemAssinatura,
            textoDigitado = perfil.textoDigitado ?: perfil.nomeExibicao,
            assinadoEm = agora,
            dispositivo = "${Build.MANUFACTURER} ${Build.MODEL}",
            codigo = FileHasher.shortCode(FileHasher.sha256("${doc.hashOriginal}|${usuario.id}|$agora")),
            rubricaPath = congelar(perfil.rubricaPath, "rubrica"),
            iniciais = iniciais
        )
    }
}