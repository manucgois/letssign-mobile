package br.com.fiap.letssign.data.local

import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.util.PasswordHasher
import javax.inject.Inject

/**
 * Cria usuários FICTÍCIOS na primeira vez que o app abre, para facilitar testes e o vídeo.
 *  Admin:        admin@letssign.com  / Admin@123
 *  Signatários:  maria@teste.com, joao@teste.com, ana@teste.com  / Teste@123
 */
class DatabaseSeeder @Inject constructor(
    private val userDao: UserDao
) {

    suspend fun seedIfEmpty() {
        if (userDao.count() > 0) return

        val adminId = userDao.insert(
            newUser("Administrador Demo", "admin@letssign.com", "(31) 99999-0000", "Admin@123", UserRole.ADMIN, null)
        )

        userDao.insert(newUser("Maria Contratante", "maria@teste.com", "(31) 99999-0001", "Teste@123", UserRole.SIGNATARIO, adminId))
        userDao.insert(newUser("João Contratado", "joao@teste.com", "(31) 99999-0002", "Teste@123", UserRole.SIGNATARIO, adminId))
        userDao.insert(newUser("Ana Testemunha", "ana@teste.com", "(31) 99999-0003", "Teste@123", UserRole.SIGNATARIO, adminId))
    }

    private fun newUser(
        nome: String,
        email: String,
        telefone: String,
        senha: String,
        perfil: UserRole,
        criadoPorId: Long?
    ): UserEntity {
        val salt = PasswordHasher.generateSalt()
        return UserEntity(
            nomeCompleto = nome,
            email = email,
            telefone = telefone,
            senhaHash = PasswordHasher.hash(senha, salt),
            senhaSalt = salt,
            perfil = perfil,
            empresa = "Empresa Demo Ltda.",
            criadoPorId = criadoPorId
        )
    }
}