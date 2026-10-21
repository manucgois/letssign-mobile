package br.com.fiap.letssign.ui.screens.document

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.SignerWithUser
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerRole
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.DocumentRepository
import br.com.fiap.letssign.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Qual lista de seleção está aberta: adicionar alguém novo ou substituir um participante. */
sealed interface FolhaSelecao {
    data object Adicionar : FolhaSelecao
    data class Substituir(val participanteId: Long, val nomeAtual: String) : FolhaSelecao
}

data class DocumentSignersUiState(
    val carregando: Boolean = true,
    val bloqueio: String? = null,
    val documentId: Long = -1,
    val titulo: String = "",
    val adminId: Long = -1,
    val participantes: List<SignerWithUser> = emptyList(),
    val disponiveis: List<UserEntity> = emptyList(),
    val folha: FolhaSelecao? = null,
    val erro: String? = null,
    val avancar: Boolean = false
)

@HiltViewModel
class DocumentSignersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val documentId: Long = savedStateHandle.get<Long>("documentId") ?: -1L

    private val _uiState = MutableStateFlow(DocumentSignersUiState(documentId = documentId))
    val uiState: StateFlow<DocumentSignersUiState> = _uiState.asStateFlow()

    private val adminId: Long get() = _uiState.value.adminId

    init {
        carregar()
    }

    private fun carregar() {
        viewModelScope.launch {
            val admin = authRepository.currentUser.value
            if (admin == null) {
                _uiState.update { it.copy(carregando = false, bloqueio = "Sessão expirada. Faça login novamente.") }
                return@launch
            }

            val doc = documentRepository.getDocument(documentId, admin.id)
            if (doc == null || doc.ownerId != admin.id) {
                _uiState.update { it.copy(carregando = false, bloqueio = "Documento não encontrado.") }
                return@launch
            }
            if (doc.status != DocumentStatus.RASCUNHO) {
                _uiState.update {
                    it.copy(carregando = false, bloqueio = "Este documento já foi enviado e não pode ser alterado.")
                }
                return@launch
            }

            _uiState.update { it.copy(titulo = doc.titulo, adminId = admin.id) }

            // Escuta "ao vivo" os participantes e as pessoas cadastradas
            combine(
                documentRepository.observeParticipants(documentId),
                userRepository.observeSignersForAdmin(admin.id)
            ) { participantes, pessoas ->
                val jaAdicionados = participantes.map { it.user.id }.toSet()
                participantes to pessoas.filter { it.id !in jaAdicionados }
            }.collect { (participantes, disponiveis) ->
                _uiState.update {
                    it.copy(carregando = false, participantes = participantes, disponiveis = disponiveis)
                }
            }
        }
    }

    // ---------- Lista de seleção ----------
    fun abrirAdicionar() = _uiState.update { it.copy(folha = FolhaSelecao.Adicionar, erro = null) }

    fun abrirSubstituir(p: SignerWithUser) = _uiState.update {
        it.copy(folha = FolhaSelecao.Substituir(p.signer.id, p.user.nomeCompleto), erro = null)
    }

    fun fecharFolha() = _uiState.update { it.copy(folha = null) }

    fun selecionarPessoa(pessoa: UserEntity) {
        val folha = _uiState.value.folha ?: return
        _uiState.update { it.copy(folha = null) }

        viewModelScope.launch {
            val resultado = when (folha) {
                is FolhaSelecao.Adicionar -> documentRepository.addParticipant(
                    documentId = documentId,
                    ownerId = adminId,
                    userId = pessoa.id,
                    papel = papelSugerido()
                )
                is FolhaSelecao.Substituir -> documentRepository.replaceParticipant(
                    ownerId = adminId,
                    participanteId = folha.participanteId,
                    novoUserId = pessoa.id
                )
            }
            resultado.onFailure { e -> _uiState.update { it.copy(erro = e.message) } }
        }
    }

    /** Sugestão: 1º contratante, 2º contratado, os demais testemunhas (pode ser trocado). */
    private fun papelSugerido(): SignerRole = when (_uiState.value.participantes.size) {
        0 -> SignerRole.CONTRATANTE
        1 -> SignerRole.CONTRATADO
        else -> SignerRole.TESTEMUNHA
    }

    // ---------- Ações em cada participante ----------
    fun alterarPapel(p: SignerWithUser, papel: SignerRole) = salvar(p.signer.copy(papel = papel))

    fun alterarObrigatorio(p: SignerWithUser, obrigatorio: Boolean) =
        salvar(p.signer.copy(obrigatorio = obrigatorio))

    private fun salvar(atualizado: br.com.fiap.letssign.data.local.entity.DocumentSignerEntity) {
        viewModelScope.launch {
            documentRepository.updateParticipant(adminId, atualizado)
                .onFailure { e -> _uiState.update { it.copy(erro = e.message) } }
        }
    }

    fun remover(p: SignerWithUser) {
        viewModelScope.launch {
            documentRepository.removeParticipant(adminId, p.signer)
                .onFailure { e -> _uiState.update { it.copy(erro = e.message) } }
        }
    }

    // ---------- Avançar ----------
    fun continuar() {
        val participantes = _uiState.value.participantes
        val erro = when {
            participantes.isEmpty() ->
                "Adicione pelo menos um signatário."
            participantes.none { it.signer.obrigatorio } ->
                "Marque pelo menos um signatário como obrigatório."
            else -> null
        }
        _uiState.update { if (erro != null) it.copy(erro = erro) else it.copy(erro = null, avancar = true) }
    }

    fun onAvancou() = _uiState.update { it.copy(avancar = false) }
}