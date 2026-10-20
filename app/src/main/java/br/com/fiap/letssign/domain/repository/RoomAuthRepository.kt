package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.data.session.SessionManager
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomAuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val session: SessionManager
) : AuthRepository {

    override val currentUser: StateFlow<UserEntity?> = session.currentUser

    override suspend fun login(email: String, senha: String): Result<UserEntity> =
        withContext(Dispatchers.Default) {
            val user = userDao.getByEmail(email.trim().lowercase())
            val valid = user != null &&
                    PasswordHasher.verify(senha, user.senhaSalt, user.senhaHash)

            if (user != null && valid) {
                session.start(user)
                Result.success(user)
            } else {
                // Mensagem genérica de propósito: não revela se o e-mail existe.
                Result.failure(IllegalArgumentException("E-mail ou senha inválidos"))
            }
        }

    override fun logout() = session.end()
}