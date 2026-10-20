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

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: RoomAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: RoomUserRepository): UserRepository
}