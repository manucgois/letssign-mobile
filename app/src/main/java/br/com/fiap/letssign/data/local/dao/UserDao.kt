package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.fiap.letssign.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    /** Retorna o id gerado. Se o e-mail já existir, lança erro (índice único). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    /**
     * Signatários disponíveis para um admin: os que ELE cadastrou + ele mesmo
     * (o admin também pode assinar os próprios documentos).
     */
    @Query(
        """
        SELECT * FROM users
        WHERE criadoPorId = :adminId OR id = :adminId
        ORDER BY nomeCompleto
        """
    )
    fun observeSignersForAdmin(adminId: Long): Flow<List<UserEntity>>

    @Query("UPDATE users SET assinaturaConfigurada = :configurada WHERE id = :userId")
    suspend fun setAssinaturaConfigurada(userId: Long, configurada: Boolean)

    /** Usado para saber se o banco está vazio e criar os dados de demonstração. */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}