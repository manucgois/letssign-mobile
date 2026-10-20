package br.com.fiap.letssign.domain.repository

import br.com.fiap.letssign.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeSignersForAdmin(adminId: Long): Flow<List<UserEntity>>
    suspend fun getById(id: Long): UserEntity?

    suspend fun createSigner(
        adminId: Long,
        nomeCompleto: String,
        email: String,
        telefone: String,
        senha: String,
        empresa: String
    ): Result<Long>

    /** novaSenha = null mantém a senha atual. */
    suspend fun updateSigner(
        user: UserEntity,
        nomeCompleto: String,
        email: String,
        telefone: String,
        empresa: String,
        novaSenha: String?
    ): Result<Unit>
}