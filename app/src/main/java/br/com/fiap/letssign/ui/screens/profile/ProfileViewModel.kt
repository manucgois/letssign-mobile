package br.com.fiap.letssign.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.DocumentRepository
import br.com.fiap.letssign.domain.repository.SigningRepository
import br.com.fiap.letssign.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class Estatistica(val rotulo: String, val valor: Int)

data class ProfileUiState(
    val nome: String = "",
    val email: String = "",
    val empresa: String = "",
    val perfil: UserRole? = null,
    val temAssinatura: Boolean = false,
    val estatisticas: List<Estatistica> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val signingRepository: SigningRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> =
        combine(
            authRepository.currentUser,
            estatisticasDe(authRepository.currentUser.value)
        ) { usuario, estatisticas ->
            ProfileUiState(
                nome = usuario?.nomeCompleto.orEmpty(),
                email = usuario?.email.orEmpty(),
                empresa = usuario?.empresa.orEmpty(),
                perfil = usuario?.perfil,
                temAssinatura = usuario?.assinaturaConfigurada ?: false,
                estatisticas = estatisticas
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState()
        )

    /** Números do perfil: o admin vê os documentos que enviou; o signatário, os que recebeu. */
    private fun estatisticasDe(usuario: UserEntity?): Flow<List<Estatistica>> = when {
        usuario == null -> flowOf(emptyList())

        usuario.perfil == UserRole.ADMIN -> combine(
            documentRepository.observeMyDocumentsWithProgress(usuario.id),
            userRepository.observeSignersForAdmin(usuario.id)
        ) { documentos, pessoas ->
            listOf(
                Estatistica("Documentos", documentos.size),
                Estatistica("Concluídos", documentos.count { it.document.status == DocumentStatus.CONCLUIDO }),
                Estatistica("Signatários", pessoas.count { it.id != usuario.id })
            )
        }

        else -> signingRepository.observeMyDocuments(usuario.id).map { lista ->
            listOf(
                Estatistica("Recebidos", lista.size),
                Estatistica("Pendentes", lista.count { it.meuStatus == SignerStatus.PENDENTE }),
                Estatistica("Assinados", lista.count { it.meuStatus == SignerStatus.ASSINADO })
            )
        }
    }

    fun logout() = authRepository.logout()
}