package br.com.fiap.letssign.ui.screens.signers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SignersListUiState(
    val carregando: Boolean = true,
    val signatarios: List<UserEntity> = emptyList(),
    val totalCadastrados: Int = 0
)

@HiltViewModel
class SignersListViewModel @Inject constructor(
    authRepository: AuthRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val adminId: Long = authRepository.currentUser.value?.id ?: -1L

    private val _busca = MutableStateFlow("")
    val busca: StateFlow<String> = _busca.asStateFlow()

    /**
     * Junta duas fontes "ao vivo": a lista do banco + o texto da busca.
     * Sempre que uma das duas muda, a lista filtrada é recalculada.
     */
    val uiState: StateFlow<SignersListUiState> =
        combine(userRepository.observeSignersForAdmin(adminId), _busca) { lista, termo ->
            val semOAdmin = lista.filter { it.id != adminId }
            val filtrada = if (termo.isBlank()) semOAdmin else semOAdmin.filter {
                it.nomeCompleto.contains(termo, ignoreCase = true) ||
                        it.email.contains(termo, ignoreCase = true)
            }
            SignersListUiState(
                carregando = false,
                signatarios = filtrada,
                totalCadastrados = semOAdmin.size
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SignersListUiState()
        )

    fun onBuscaChange(valor: String) {
        _busca.value = valor
    }
}