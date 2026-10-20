package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import br.com.fiap.letssign.domain.model.IdentificationType
import br.com.fiap.letssign.domain.model.SignatureStyle

@Entity(
    tableName = "signature_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SignatureProfileEntity(
    @PrimaryKey val userId: Long,
    val nomeExibicao: String,
    val cargo: String? = null,
    val tipoIdentificacao: IdentificationType,
    val numeroIdentificacao: String,
    val empresa: String,
    val estiloAssinatura: SignatureStyle,
    val assinaturaPath: String? = null,
    val rubricaPath: String? = null,
    val textoDigitado: String? = null,
    val criadoEm: Long = System.currentTimeMillis(),
    val atualizadoEm: Long = System.currentTimeMillis()
)