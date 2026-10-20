package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.fiap.letssign.data.local.entity.SendRequestEntity

@Dao
interface SendRequestDao {

    /**
     * IGNORE: se a idempotencyKey já existir, não insere de novo e retorna -1.
     * É assim que evitamos duplicar a solicitação numa nova tentativa.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(request: SendRequestEntity): Long

    @Update
    suspend fun update(request: SendRequestEntity)

    @Query("SELECT * FROM send_requests WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByKey(key: String): SendRequestEntity?
}