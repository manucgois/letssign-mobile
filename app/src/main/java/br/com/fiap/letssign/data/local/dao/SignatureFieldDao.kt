package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SignatureFieldDao {

    @Insert
    suspend fun insert(field: SignatureFieldEntity): Long

    @Update
    suspend fun update(field: SignatureFieldEntity)

    @Delete
    suspend fun delete(field: SignatureFieldEntity)

    @Query("SELECT * FROM signature_fields WHERE documentId = :documentId ORDER BY pagina")
    fun observeByDocument(documentId: Long): Flow<List<SignatureFieldEntity>>

    @Query("SELECT * FROM signature_fields WHERE documentId = :documentId ORDER BY pagina")
    suspend fun getByDocument(documentId: Long): List<SignatureFieldEntity>

    @Query("SELECT * FROM signature_fields WHERE signerId = :signerId ORDER BY pagina")
    suspend fun getBySigner(signerId: Long): List<SignatureFieldEntity>
}