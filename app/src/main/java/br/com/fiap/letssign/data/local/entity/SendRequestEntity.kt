package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fiap.letssign.domain.model.SendStatus

@Entity(
    tableName = "send_requests",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index("documentId")
    ]
)
data class SendRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val idempotencyKey: String,
    val status: SendStatus = SendStatus.PENDENTE,
    val tentativas: Int = 0,
    val ultimaTentativaEm: Long? = null,
    val erro: String? = null
)