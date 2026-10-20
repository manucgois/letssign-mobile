package br.com.fiap.letssign.di

import android.content.Context
import androidx.room.Room
import br.com.fiap.letssign.data.local.AppDatabase
import br.com.fiap.letssign.data.local.dao.AuditEventDao
import br.com.fiap.letssign.data.local.dao.DocumentDao
import br.com.fiap.letssign.data.local.dao.DocumentSignerDao
import br.com.fiap.letssign.data.local.dao.SendRequestDao
import br.com.fiap.letssign.data.local.dao.SignatureFieldDao
import br.com.fiap.letssign.data.local.dao.SignatureProfileDao
import br.com.fiap.letssign.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "letssign.db").build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideSignatureProfileDao(db: AppDatabase): SignatureProfileDao = db.signatureProfileDao()
    @Provides fun provideDocumentDao(db: AppDatabase): DocumentDao = db.documentDao()
    @Provides fun provideDocumentSignerDao(db: AppDatabase): DocumentSignerDao = db.documentSignerDao()
    @Provides fun provideSignatureFieldDao(db: AppDatabase): SignatureFieldDao = db.signatureFieldDao()
    @Provides fun provideAuditEventDao(db: AppDatabase): AuditEventDao = db.auditEventDao()
    @Provides fun provideSendRequestDao(db: AppDatabase): SendRequestDao = db.sendRequestDao()
}