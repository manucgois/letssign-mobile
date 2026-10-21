package br.com.fiap.letssign.ui.screens.signers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.UserRepository
import br.com.fiap.letssign.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignerFormUiState(
    val carregando: Boolean = true,
    val modoEdicao: Boolean = false,
    val nome: String = "",
    val email: String = "",
    val telefone: String = "",
    val empresa: String = "",
    val senha: String = "",
    val confirmarSenha: String = "",
    val senhaVisivel: Boolean = false,
    val nomeErro: String? = null,
    val emailErro: String? = null,
    val telefoneErro: String? = null,
    val empresaErro: String? = null,
    val senhaErro: String? = null,
    val confirmarErro: String? = null,
    val erroGeral: String? = null,
    val naoEncontrado: Boolean = false,
    val salvando: Boolean = false,
    val salvo: Boolean = false
)

@HiltViewModel
class SignerFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    /** Vem da rota: -1 = novo signatário; outro valor = editar esse id. */
    private val signerId: Long = savedStateHandle.get<Long>("signerId") ?: -1L
    private var original: UserEntity? = null

    private val _uiState = MutableStateFlow(SignerFormUiState())
    val uiState: StateFlow<SignerFormUiState> = _uiState.asStateFlow()

    init {
        carregar()
    }

    private fun carregar() {
        viewModelScope.launch {
            val admin = authRepository.currentUser.value
            if (admin == null) {
                _uiState.update { it.copy(carregando = false, naoEncontrado = true) }
                return@launch
            }

            if (signerId == -1L) {
                // Novo: empresa pré-preenchida com a do admin
                _uiState.update { it.copy(carregando = false, empresa = admin.empresa) }
                return@launch
            }

            val usuario = userRepository.getById(signerId)
            // Segurança: o admin só edita signatários que ELE cadastrou
            if (usuario == null || usuario.criadoPorId != admin.id) {
                _uiState.update { it.copy(carregando = false, naoEncontrado = true) }
                return@launch
            }

            original = usuario
            _uiState.update {
                it.copy(
                    carregando = false,
                    modoEdicao = true,
                    nome = usuario.nomeCompleto,
                    email = usuario.email,
                    telefone = usuario.telefone,
                    empresa = usuario.empresa
                )
            }
        }
    }

    fun onNomeChange(v: String) = _uiState.update { it.copy(nome = v, nomeErro = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailErro = null, erroGeral = null) }
    fun onTelefoneChange(v: String) = _uiState.update { it.copy(telefone = v, telefoneErro = null) }
    fun onEmpresaChange(v: String) = _uiState.update { it.copy(empresa = v, empresaErro = null) }
    fun onSenhaChange(v: String) = _uiState.update { it.copy(senha = v, senhaErro = null) }
    fun onConfirmarChange(v: String) = _uiState.update { it.copy(confirmarSenha = v, confirmarErro = null) }
    fun onToggleSenha() = _uiState.update { it.copy(senhaVisivel = !it.senhaVisivel) }

    fun salvar() {
        val s = _uiState.value
        if (s.salvando) return
        val admin = authRepository.currentUser.value ?: return

        // 1. Validações
        val nomeErro = Validators.validateFullName(s.nome)
        val emailErro = if (Validators.isValidEmail(s.email)) null else "E-mail inválido"
        val telefoneErro = Validators.validatePhone(s.telefone)
        val empresaErro = if (s.empresa.isBlank()) "Informe a empresa" else null

        // Senha: obrigatória ao criar; opcional ao editar (em branco = mantém a atual)
        val precisaValidarSenha = !s.modoEdicao || s.senha.isNotEmpty()
        val senhaErro = if (precisaValidarSenha) Validators.validatePassword(s.senha) else null
        val confirmarErro = if (precisaValidarSenha && s.senha != s.confirmarSenha)
            "As senhas não conferem" else null

        if (listOf(nomeErro, emailErro, telefoneErro, empresaErro, senhaErro, confirmarErro).any { it != null }) {
            _uiState.update {
                it.copy(
                    nomeErro = nomeErro, emailErro = emailErro, telefoneErro = telefoneErro,
                    empresaErro = empresaErro, senhaErro = senhaErro, confirmarErro = confirmarErro
                )
            }
            return
        }

        val telefoneFormatado = Validators.formatPhone(s.telefone)

        // 2. Salva (criar ou editar)
        viewModelScope.launch {
            _uiState.update { it.copy(salvando = true, erroGeral = null) }

            val resultado: Result<Unit> = if (s.modoEdicao) {
                userRepository.updateSigner(
                    user = original!!,
                    nomeCompleto = s.nome,
                    email = s.email,
                    telefone = telefoneFormatado,
                    empresa = s.empresa,
                    novaSenha = s.senha.ifBlank { null }
                )
            } else {
                userRepository.createSigner(
                    adminId = admin.id,
                    nomeCompleto = s.nome,
                    email = s.email,
                    telefone = telefoneFormatado,
                    senha = s.senha,
                    empresa = s.empresa
                ).map { }
            }

            resultado
                .onSuccess { _uiState.update { it.copy(salvando = false, salvo = true) } }
                .onFailure { e ->
                    // O erro mais comum aqui é o e-mail repetido: mostramos no próprio campo
                    _uiState.update { it.copy(salvando = false, emailErro = e.message) }
                }
        }
    }
}