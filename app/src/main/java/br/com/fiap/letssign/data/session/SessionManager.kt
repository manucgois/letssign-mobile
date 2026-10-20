package br.com.fiap.letssign.data.session

import br.com.fiap.letssign.data.local.entity.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda quem está logado enquanto o app estiver aberto.
 * Decisão de segurança: ao fechar o app, a sessão é encerrada e o login é exigido de novo.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    fun start(user: UserEntity) {
        _currentUser.value = user
    }

    fun end() {
        _currentUser.value = null
    }

    /** Retorna o usuário logado ou lança erro se ninguém estiver logado. */
    fun requireUser(): UserEntity =
        _currentUser.value ?: throw IllegalStateException("Nenhum usuário logado")
}