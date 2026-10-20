package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "signature_fields",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DocumentSignerEntity::class,
            parentColumns = ["id"],
            childColumns = ["signerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId"), Index("signerId")]
)
data class SignatureFieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val signerId: Long,
    val pagina: Int,
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float
)