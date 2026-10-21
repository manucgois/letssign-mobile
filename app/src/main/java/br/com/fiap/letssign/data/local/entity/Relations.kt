package br.com.fiap.letssign.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus

/** Participante de um documento junto com os dados do usuário (nome, e-mail...). */
data class SignerWithUser(
    @Embedded val signer: DocumentSignerEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val user: UserEntity
)

/** Documento visto pelo signatário: o documento + a situação DELE naquele documento. */
data class SignerDocumentItem(
    @Embedded val document: DocumentEntity,
    val meuSignerId: Long,
    val meuStatus: SignerStatus
)

/** Contagem de documentos por status (para os cards do dashboard do admin). */
data class StatusCount(
    val status: DocumentStatus,
    val total: Int
)

/** Documento na lista do admin + quantas pessoas assinam e quantas já assinaram. */
data class DocumentListItem(
    @Embedded val document: DocumentEntity,
    val totalSignatarios: Int,
    val assinados: Int
)