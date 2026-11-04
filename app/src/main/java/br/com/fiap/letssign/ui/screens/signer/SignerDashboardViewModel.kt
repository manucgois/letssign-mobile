package br.com.fiap.letssign.ui.screens.signer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.SignerDocumentItem
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.SigningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class AbaSignatario { PENDENTES, ASSINADOS }

data class SignerDashboardUiState(
    val carregando: Boolean = true,
    val primeiroNome: String = "",
    val temAssinatura: Boolean = true,
    val aba: AbaSignatario = AbaSignatario.PENDENTES,
    val pendentes: List<SignerDocumentItem> = emptyList(),
    val assinados: List<SignerDocumentItem> = emptyList()
)

@HiltViewModel
class SignerDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    signingRepository: SigningRepository
) : ViewModel() {

    private val userId = authRepository.currentUser.value?.id ?: -1L
    private val aba = MutableStateFlow(AbaSignatario.PENDENTES)

    val uiState: StateFlow<SignerDashboardUiState> =
        combine(
            signingRepository.observeMyDocuments(userId),
            aba,
            authRepository.currentUser
        ) { lista, abaAtual, usuario ->
            SignerDashboardUiState(
                carregando = false,
                primeiroNome = usuario?.nomeCompleto?.substringBefore(" ").orEmpty(),
                temAssinatura = usuario?.assinaturaConfigurada ?: false,
                aba = abaAtual,
                pendentes = lista.filter {
                    it.meuStatus == SignerStatus.PENDENTE &&
                            (it.document.status == DocumentStatus.ENVIADO ||
                                    it.document.status == DocumentStatus.PARCIALMENTE_ASSINADO)
                },
                assinados = lista.filter { it.meuStatus == SignerStatus.ASSINADO }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SignerDashboardUiState()
        )

    fun onAba(nova: AbaSignatario) {
        aba.value = nova
    }

    fun logout() = authRepository.logout()
}