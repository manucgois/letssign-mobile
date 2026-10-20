package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import br.com.fiap.letssign.data.local.entity.AuditEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditEventDao {

    @Insert
    suspend fun insert(event: AuditEventEntity): Long

    /** Linha do tempo do documento, do mais antigo para o mais recente. */
    @Query("SELECT * FROM audit_events WHERE documentId = :documentId ORDER BY dataHora ASC")
    fun observeByDocument(documentId: Long): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events WHERE documentId = :documentId ORDER BY dataHora ASC")
    suspend fun getByDocument(documentId: Long): List<AuditEventEntity>
}