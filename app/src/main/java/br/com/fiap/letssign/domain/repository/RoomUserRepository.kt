package br.com.fiap.letssign.data.repository

import android.database.sqlite.SQLiteConstraintException
import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.domain.repository.UserRepository
import br.com.fiap.letssign.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomUserRepository @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override fun observeSignersForAdmin(adminId: Long): Flow<List<UserEntity>> =
        userDao.observeSignersForAdmin(adminId)

    override suspend fun getById(id: Long): UserEntity? = userDao.getById(id)

    override suspend fun createSigner(
        adminId: Long,
        nomeCompleto: String,
        email: String,
        telefone: String,
        senha: String,
        empresa: String
    ): Result<Long> = withContext(Dispatchers.Default) {
        try {
            val salt = PasswordHasher.generateSalt()
            val id = userDao.insert(
                UserEntity(
                    nomeCompleto = nomeCompleto.trim(),
                    email = email.trim().lowercase(),
                    telefone = telefone.trim(),
                    senhaHash = PasswordHasher.hash(senha, salt),
                    senhaSalt = salt,
                    perfil = UserRole.SIGNATARIO,
                    empresa = empresa.trim(),
                    criadoPorId = adminId
                )
            )
            Result.success(id)
        } catch (e: SQLiteConstraintException) {
            Result.failure(IllegalArgumentException("Já existe um usuário com este e-mail"))
        }
    }

    override suspend fun updateSigner(
        user: UserEntity,
        nomeCompleto: String,
        email: String,
        telefone: String,
        empresa: String,
        novaSenha: String?
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            var updated = user.copy(
                nomeCompleto = nomeCompleto.trim(),
                email = email.trim().lowercase(),
                telefone = telefone.trim(),
                empresa = empresa.trim()
            )
            if (!novaSenha.isNullOrBlank()) {
                val salt = PasswordHasher.generateSalt()
                updated = updated.copy(
                    senhaHash = PasswordHasher.hash(novaSenha, salt),
                    senhaSalt = salt
                )
            }
            userDao.update(updated)
            Result.success(Unit)
        } catch (e: SQLiteConstraintException) {
            Result.failure(IllegalArgumentException("Já existe um usuário com este e-mail"))
        }
    }
}