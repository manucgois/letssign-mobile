package br.com.fiap.letssign.ui.screens.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailErro = null, erroGeral = null) }
    }

    fun onSenhaChange(value: String) {
        _uiState.update { it.copy(senha = value, senhaErro = null, erroGeral = null) }
    }

    fun onToggleSenhaVisivel() {
        _uiState.update { it.copy(senhaVisivel = !it.senhaVisivel) }
    }

    fun login() {
        val state = _uiState.value
        if (state.carregando) return // evita clique duplo

        // 1. Validação dos dados obrigatórios antes de consultar o banco
        val emailErro = when {
            state.email.isBlank() -> "Informe o e-mail"
            !Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() -> "E-mail inválido"
            else -> null
        }
        val senhaErro = if (state.senha.isBlank()) "Informe a senha" else null

        if (emailErro != null || senhaErro != null) {
            _uiState.update { it.copy(emailErro = emailErro, senhaErro = senhaErro) }
            return
        }

        // 2. Login no repositório (em segundo plano)
        viewModelScope.launch {
            _uiState.update { it.copy(carregando = true, erroGeral = null) }

            authRepository.login(state.email, state.senha)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(carregando = false, senha = "", usuarioLogado = user)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(carregando = false, erroGeral = e.message ?: "Não foi possível entrar")
                    }
                }
        }
    }

    /** Chamado pela tela depois de navegar, para não navegar de novo. */
    fun onNavigated() {
        _uiState.update { it.copy(usuarioLogado = null) }
    }
}