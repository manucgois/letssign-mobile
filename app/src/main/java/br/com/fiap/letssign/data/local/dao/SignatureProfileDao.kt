package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import br.com.fiap.letssign.data.local.entity.SignatureProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SignatureProfileDao {

    /** Cria o perfil se não existir; atualiza se já existir. */
    @Upsert
    suspend fun upsert(profile: SignatureProfileEntity)

    @Query("SELECT * FROM signature_profiles WHERE userId = :userId")
    suspend fun getByUserId(userId: Long): SignatureProfileEntity?

    @Query("SELECT * FROM signature_profiles WHERE userId = :userId")
    fun observeByUserId(userId: Long): Flow<SignatureProfileEntity?>
}