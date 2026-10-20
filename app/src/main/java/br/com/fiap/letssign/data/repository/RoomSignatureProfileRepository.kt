package br.com.fiap.letssign.data.repository

import android.graphics.Bitmap
import br.com.fiap.letssign.data.file.SignatureFileStorage
import br.com.fiap.letssign.data.local.dao.SignatureProfileDao
import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.SignatureProfileEntity
import br.com.fiap.letssign.data.session.SessionManager
import br.com.fiap.letssign.domain.model.SignatureStyle
import br.com.fiap.letssign.domain.repository.SignatureProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomSignatureProfileRepository @Inject constructor(
    private val profileDao: SignatureProfileDao,
    private val userDao: UserDao,
    private val storage: SignatureFileStorage,
    private val session: SessionManager
) : SignatureProfileRepository {

    override fun observeProfile(userId: Long): Flow<SignatureProfileEntity?> =
        profileDao.observeByUserId(userId)

    override suspend fun getProfile(userId: Long): SignatureProfileEntity? =
        profileDao.getByUserId(userId)

    override suspend fun saveProfile(
        profile: SignatureProfileEntity,
        desenho: Bitmap?
    ): Result<Unit> = try {
        var toSave = profile.copy(atualizadoEm = System.currentTimeMillis())

        // 1. Se houve desenho, salva a imagem e guarda o caminho no campo certo
        if (desenho != null) {
            when (profile.estiloAssinatura) {
                SignatureStyle.RUBRICA ->
                    toSave = toSave.copy(rubricaPath = storage.save(profile.userId, "rubrica", desenho))
                else ->
                    toSave = toSave.copy(assinaturaPath = storage.save(profile.userId, "assinatura", desenho))
            }
        }

        // 2. Salva o perfil e marca o usuário como "assinatura configurada"
        profileDao.upsert(toSave)
        userDao.setAssinaturaConfigurada(profile.userId, true)

        // 3. Atualiza a sessão para o app "saber" que o perfil foi criado
        userDao.getById(profile.userId)?.let { session.start(it) }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Não foi possível salvar a assinatura. Tente novamente."))
    }
}