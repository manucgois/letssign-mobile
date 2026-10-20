package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.fiap.letssign.data.local.entity.DocumentSignerEntity
import br.com.fiap.letssign.data.local.entity.SignerWithUser
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentSignerDao {

    @Insert
    suspend fun insert(signer: DocumentSignerEntity): Long

    @Update
    suspend fun update(signer: DocumentSignerEntity)

    @Delete
    suspend fun delete(signer: DocumentSignerEntity)

    @Query("SELECT * FROM document_signers WHERE id = :id")
    suspend fun getById(id: Long): DocumentSignerEntity?

    @Query("SELECT * FROM document_signers WHERE documentId = :documentId ORDER BY ordem")
    suspend fun getByDocument(documentId: Long): List<DocumentSignerEntity>

    /** Signatários do documento com nome/e-mail (para revisão e acompanhamento). */
    @Transaction
    @Query("SELECT * FROM document_signers WHERE documentId = :documentId ORDER BY ordem")
    fun observeWithUserByDocument(documentId: Long): Flow<List<SignerWithUser>>

    @Query("SELECT * FROM document_signers WHERE documentId = :documentId AND userId = :userId LIMIT 1")
    suspend fun getByDocumentAndUser(documentId: Long, userId: Long): DocumentSignerEntity?

    /**
     * Quantos signatários OBRIGATÓRIOS ainda não assinaram.
     * Quando der 0, o documento pode ser marcado como CONCLUÍDO.
     */
    @Query(
        """
        SELECT COUNT(*) FROM document_signers
        WHERE documentId = :documentId AND obrigatorio = 1 AND status != 'ASSINADO'
        """
    )
    suspend fun countPendingRequired(documentId: Long): Int
}