package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fiap.letssign.domain.model.AuditEventType

@Entity(
    tableName = "audit_events",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val userId: Long?,
    val tipo: AuditEventType,
    val dataHora: Long = System.currentTimeMillis(),
    val detalhe: String? = null
)