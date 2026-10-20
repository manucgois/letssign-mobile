package br.com.fiap.letssign.domain.repository

import android.graphics.Bitmap
import br.com.fiap.letssign.data.local.entity.SignatureProfileEntity
import kotlinx.coroutines.flow.Flow

interface SignatureProfileRepository {
    fun observeProfile(userId: Long): Flow<SignatureProfileEntity?>
    suspend fun getProfile(userId: Long): SignatureProfileEntity?

    /** desenho = imagem feita no quadro (null quando a assinatura é digitada). */
    suspend fun saveProfile(profile: SignatureProfileEntity, desenho: Bitmap?): Result<Unit>
}