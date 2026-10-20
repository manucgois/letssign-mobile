package br.com.fiap.letssign.domain.repository

import br.com.fiap.letssign.data.local.entity.UserEntity
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserEntity?>
    suspend fun login(email: String, senha: String): Result<UserEntity>
    fun logout()
}