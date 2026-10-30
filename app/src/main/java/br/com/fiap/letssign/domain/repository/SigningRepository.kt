package br.com.fiap.letssign.domain.repository

import android.graphics.Bitmap
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.DocumentSignerEntity
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.entity.SignatureProfileEntity
import br.com.fiap.letssign.data.local.entity.SignerDocumentItem
import br.com.fiap.letssign.domain.model.AssinaturaResultado
import kotlinx.coroutines.flow.Flow

/** Tudo o que a tela de assinatura precisa saber. */
data class SigningContext(
    val document: DocumentEntity,
    val eu: DocumentSignerEntity,
    val meusCampos: List<SignatureFieldEntity>,
    val remetente: String,
    val perfil: SignatureProfileEntity?
)

interface SigningRepository {
    fun observeMyDocuments(userId: Long): Flow<List<SignerDocumentItem>>
    suspend fun getSigningContext(documentId: Long, userId: Long): SigningContext?
    suspend fun registerView(documentId: Long, userId: Long)
    suspend fun sign(documentId: Long, userId: Long, senha: String): Result<AssinaturaResultado>

    /** versaoAssinada = true mostra o PDF com as assinaturas (quando já existe). */
    suspend fun renderPage(
        documentId: Long, userId: Long, pagina: Int, larguraPx: Int, versaoAssinada: Boolean
    ): Bitmap?
}