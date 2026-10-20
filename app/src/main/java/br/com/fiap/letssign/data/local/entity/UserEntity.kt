package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fiap.letssign.domain.model.UserRole

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nomeCompleto: String,
    val email: String,
    val telefone: String,
    val senhaHash: String,
    val senhaSalt: String,
    val perfil: UserRole,
    val empresa: String,
    val assinaturaConfigurada: Boolean = false,
    val criadoPorId: Long? = null,
    val criadoEm: Long = System.currentTimeMillis()
)