package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fiap.letssign.domain.model.DocumentStatus

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ownerId"), Index("documentoPaiId")]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val ownerId: Long,
    val status: DocumentStatus = DocumentStatus.RASCUNHO,
    val versao: Int = 1,
    val documentoPaiId: Long? = null,
    val nomeArquivo: String,
    val caminhoArquivo: String,
    val quantidadePaginas: Int,
    val hashOriginal: String,
    val caminhoArquivoFinal: String? = null,
    val hashFinal: String? = null,
    val mensagemConvite: String? = null,
    val criadoEm: Long = System.currentTimeMillis(),
    val enviadoEm: Long? = null,
    val concluidoEm: Long? = null,
    val expiraEm: Long? = null
)