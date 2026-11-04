package br.com.fiap.letssign.ui.screens.signature

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.SignatureProfileEntity
import br.com.fiap.letssign.domain.model.IdentificationType
import br.com.fiap.letssign.domain.model.SignatureStyle
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.SignatureProfileRepository
import br.com.fiap.letssign.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignatureSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: SignatureProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignatureSetupUiState())
    val uiState: StateFlow<SignatureSetupUiState> = _uiState.asStateFlow()

    private var perfilExistente: SignatureProfileEntity? = null

    init {
        carregar()
    }

    /** Preenche a tela: com o perfil salvo, ou com os dados do cadastro (1º acesso). */
    private fun carregar() {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            if (user == null) {
                _uiState.update {
                    it.copy(carregando = false, erroGeral = "Sessão expirada. Faça login novamente.")
                }
                return@launch
            }

            val p = profileRepository.getProfile(user.id)
            perfilExistente = p

            _uiState.update {
                it.copy(
                    carregando = false,
                    userId = user.id,
                    perfil = user.perfil,
                    nome = p?.nomeExibicao ?: user.nomeCompleto,
                    cargo = p?.cargo.orEmpty(),
                    tipoIdentificacao = p?.tipoIdentificacao ?: IdentificationType.CPF,
                    numero = p?.numeroIdentificacao.orEmpty(),
                    empresa = p?.empresa ?: user.empresa,
                    // "Rubrica" deixou de ser estilo: quem tinha escolhido passa para "Desenhar"
                    estilo = p?.estiloAssinatura
                        ?.takeIf { it != SignatureStyle.RUBRICA } ?: SignatureStyle.DESENHADA,
                    textoDigitado = p?.textoDigitado ?: user.nomeCompleto,
                    assinaturaPath = p?.assinaturaPath,
                    rubricaPath = p?.rubricaPath
                )
            }
        }
    }

    fun onNomeChange(v: String) = _uiState.update { it.copy(nome = v, nomeErro = null) }
    fun onCargoChange(v: String) = _uiState.update { it.copy(cargo = v) }
    fun onTipoChange(v: IdentificationType) =
        _uiState.update { it.copy(tipoIdentificacao = v, numeroErro = null) }
    fun onNumeroChange(v: String) = _uiState.update { it.copy(numero = v, numeroErro = null) }
    fun onEmpresaChange(v: String) = _uiState.update { it.copy(empresa = v, empresaErro = null) }
    fun onEstiloChange(v: SignatureStyle) = _uiState.update { it.copy(estilo = v, visualErro = null) }
    fun onTextoDigitadoChange(v: String) =
        _uiState.update { it.copy(textoDigitado = v, visualErro = null) }


    /** desenho = assinatura desenhada (null se vazio ou "Digitar"); rubrica = rubrica desenhada (opcional). */
    fun salvar(desenho: Bitmap?, rubrica: Bitmap?) {
        val s = _uiState.value
        if (s.salvando) return
        val userId = s.userId ?: return

        val nomeErro = if (s.nome.isBlank()) "Informe seu nome completo" else null
        val empresaErro = if (s.empresa.isBlank()) "Informe a empresa" else null
        val numeroErro = Validators.validateIdentification(s.tipoIdentificacao, s.numero)
        val visualErro = when {
            s.estilo == SignatureStyle.DIGITADA && s.textoDigitado.isBlank() ->
                "Digite como você quer assinar"
            s.estilo != SignatureStyle.DIGITADA && desenho == null && s.assinaturaPath == null ->
                "Desenhe sua assinatura no quadro"
            else -> null
        }

        if (listOf(nomeErro, empresaErro, numeroErro, visualErro).any { it != null }) {
            _uiState.update {
                it.copy(nomeErro = nomeErro, empresaErro = empresaErro, numeroErro = numeroErro, visualErro = visualErro)
            }
            return
        }

        val numeroSalvo = if (s.tipoIdentificacao == IdentificationType.CPF)
            Validators.onlyDigits(s.numero) else s.numero.trim()

        val profile = SignatureProfileEntity(
            userId = userId,
            nomeExibicao = s.nome.trim(),
            cargo = s.cargo.trim().ifBlank { null },
            tipoIdentificacao = s.tipoIdentificacao,
            numeroIdentificacao = numeroSalvo,
            empresa = s.empresa.trim(),
            estiloAssinatura = s.estilo,
            assinaturaPath = s.assinaturaPath,
            rubricaPath = s.rubricaPath,
            textoDigitado = s.textoDigitado.trim().ifBlank { null },
            criadoEm = perfilExistente?.criadoEm ?: System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(salvando = true, erroGeral = null) }
            profileRepository.saveProfile(profile, desenho, rubrica)
                .onSuccess { _uiState.update { it.copy(salvando = false, salvo = true) } }
                .onFailure { e -> _uiState.update { it.copy(salvando = false, erroGeral = e.message) } }
        }
    }
}