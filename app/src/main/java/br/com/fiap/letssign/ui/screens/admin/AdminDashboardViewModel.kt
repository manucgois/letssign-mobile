package br.com.fiap.letssign.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.DocumentListItem
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Filtros da lista. status = null significa "todos". */
enum class FiltroDocumento(val label: String, val status: DocumentStatus?) {
    TODOS("Todos", null),
    RASCUNHO("Rascunhos", DocumentStatus.RASCUNHO),
    AGUARDANDO("Aguardando", DocumentStatus.ENVIADO),
    PARCIAL("Parciais", DocumentStatus.PARCIALMENTE_ASSINADO),
    CONCLUIDO("Concluídos", DocumentStatus.CONCLUIDO),
    RECUSADO("Recusados", DocumentStatus.RECUSADO)
}

data class AdminDashboardUiState(
    val carregando: Boolean = true,
    val primeiroNome: String = "",
    val filtro: FiltroDocumento = FiltroDocumento.TODOS,
    val documentos: List<DocumentListItem> = emptyList(),
    val contagem: Map<DocumentStatus, Int> = emptyMap(),
    val totalGeral: Int = 0
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    documentRepository: DocumentRepository
) : ViewModel() {

    private val admin = authRepository.currentUser.value
    private val filtro = MutableStateFlow(FiltroDocumento.TODOS)

    val uiState: StateFlow<AdminDashboardUiState> =
        combine(
            documentRepository.observeMyDocumentsWithProgress(admin?.id ?: -1L),
            filtro
        ) { lista, filtroAtual ->
            AdminDashboardUiState(
                carregando = false,
                primeiroNome = admin?.nomeCompleto?.substringBefore(" ").orEmpty(),
                filtro = filtroAtual,
                documentos = if (filtroAtual.status == null) lista
                else lista.filter { it.document.status == filtroAtual.status },
                // Conta quantos documentos existem em cada status (para os cards)
                contagem = lista.groupingBy { it.document.status }.eachCount(),
                totalGeral = lista.size
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminDashboardUiState()
        )

    /** Tocar no filtro já selecionado volta para "Todos". */
    fun onFiltro(novo: FiltroDocumento) {
        filtro.value = if (filtro.value == novo) FiltroDocumento.TODOS else novo
    }

    fun logout() = authRepository.logout()
}