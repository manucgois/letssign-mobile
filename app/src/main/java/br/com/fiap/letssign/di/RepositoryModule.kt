package br.com.fiap.letssign.di

import br.com.fiap.letssign.data.repository.RoomAuthRepository
import br.com.fiap.letssign.data.repository.RoomUserRepository
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import br.com.fiap.letssign.data.repository.RoomSignatureProfileRepository
import br.com.fiap.letssign.domain.repository.SignatureProfileRepository
import br.com.fiap.letssign.data.repository.RoomDocumentRepository
import br.com.fiap.letssign.domain.repository.DocumentRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: RoomAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: RoomUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindSignatureProfileRepository(
        impl: RoomSignatureProfileRepository
    ): SignatureProfileRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(impl: RoomDocumentRepository): DocumentRepository
}