package br.com.fiap.letssign.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.fiap.letssign.data.local.entity.AuditEventEntity
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.DocumentSignerEntity
import br.com.fiap.letssign.data.local.entity.SendRequestEntity
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.entity.SignatureProfileEntity
import br.com.fiap.letssign.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SignatureProfileEntity::class,
        DocumentEntity::class,
        DocumentSignerEntity::class,
        SignatureFieldEntity::class,
        AuditEventEntity::class,
        SendRequestEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

}