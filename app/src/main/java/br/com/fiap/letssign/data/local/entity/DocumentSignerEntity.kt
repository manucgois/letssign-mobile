package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fiap.letssign.domain.model.SignerRole
import br.com.fiap.letssign.domain.model.SignerStatus

@Entity(
    tableName = "document_signers",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId", "userId"], unique = true),
        Index("userId")
    ]
)
data class DocumentSignerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val userId: Long,
    val papel: SignerRole,
    val obrigatorio: Boolean = true,
    val ordem: Int = 0,
    val status: SignerStatus = SignerStatus.PENDENTE,
    val assinadoEm: Long? = null,
    val motivoRecusa: String? = null,
    val carimboSnapshot: String? = null
)