package br.com.fiap.letssign.domain.repository

import android.graphics.Bitmap
import br.com.fiap.letssign.data.local.entity.SignatureProfileEntity
import kotlinx.coroutines.flow.Flow

interface SignatureProfileRepository {
    fun observeProfile(userId: Long): Flow<SignatureProfileEntity?>
    suspend fun getProfile(userId: Long): SignatureProfileEntity?


    /** desenho = assinatura desenhada; rubrica = rubrica desenhada (as duas podem ser null). */
    suspend fun saveProfile(
        profile: SignatureProfileEntity,
        desenho: Bitmap?,
        rubrica: Bitmap? = null
    ): Result<Unit>
}