package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.SignerDocumentItem
import br.com.fiap.letssign.data.local.entity.StatusCount
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Update
    suspend fun update(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: Long): Flow<DocumentEntity?>

    /** Documentos do ADMIN: apenas os que ele criou. */
    @Query("SELECT * FROM documents WHERE ownerId = :ownerId ORDER BY criadoEm DESC")
    fun observeByOwner(ownerId: Long): Flow<List<DocumentEntity>>

    /**
     * Documentos do SIGNATÁRIO: apenas os que foram ENVIADOS para ele
     * (rascunhos nunca aparecem), junto com a situação dele em cada um.
     */
    @Query(
        """
        SELECT d.*, s.id AS meuSignerId, s.status AS meuStatus
        FROM documents d
        INNER JOIN document_signers s ON s.documentId = d.id
        WHERE s.userId = :userId AND d.status != 'RASCUNHO'
        ORDER BY d.enviadoEm DESC
        """
    )
    fun observeForSigner(userId: Long): Flow<List<SignerDocumentItem>>

    /**
     * Verificação de segurança: o usuário pode abrir este documento?
     * Pode se for o dono OU um signatário de um documento já enviado.
     */
    @Query(
        """
        SELECT COUNT(*) FROM documents d
        WHERE d.id = :documentId AND (
            d.ownerId = :userId OR EXISTS (
                SELECT 1 FROM document_signers s
                WHERE s.documentId = d.id
                  AND s.userId = :userId
                  AND d.status != 'RASCUNHO'
            )
        )
        """
    )
    suspend fun countAccess(documentId: Long, userId: Long): Int

    /** Totais por status para os cards do dashboard. */
    @Query(
        """
        SELECT status, COUNT(*) AS total FROM documents
        WHERE ownerId = :ownerId
        GROUP BY status
        """
    )
    fun observeStatusCounts(ownerId: Long): Flow<List<StatusCount>>
}