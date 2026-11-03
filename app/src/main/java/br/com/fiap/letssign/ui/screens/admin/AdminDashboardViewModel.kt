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

/** Filtros da lista. statuses = null significa "todos". */
enum class FiltroDocumento(val label: String, val statuses: Set<DocumentStatus>?) {
    TODOS("Todos", null),
    RASCUNHO("Rascunhos", setOf(DocumentStatus.RASCUNHO)),
    ANDAMENTO("Em andamento", setOf(DocumentStatus.ENVIADO, DocumentStatus.PARCIALMENTE_ASSINADO)),
    CONCLUIDO("Concluídos", setOf(DocumentStatus.CONCLUIDO))
}

data class AdminDashboardUiState(
    val carregando: Boolean = true,
    val primeiroNome: String = "",
    val filtro: FiltroDocumento = FiltroDocumento.TODOS,
    val documentos: List<DocumentListItem> = emptyList(),
    val totalRascunhos: Int = 0,
    val totalAndamento: Int = 0,
    val totalConcluidos: Int = 0,
    val totalGeral: Int = 0
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    documentRepository: DocumentRepository
) : ViewModel() {

    private val admin = authRepository.currentUser.value
    private val filtro = MutableStateFlow(FiltroDocumento.TODOS)

    val uiState: StateFlow<AdminDashboardUiState> =
        combine(
            documentRepository.observeMyDocumentsWithProgress(admin?.id ?: -1L),
            filtro
        ) { lista, filtroAtual ->
            fun conta(f: FiltroDocumento) = lista.count { f.statuses?.contains(it.document.status) == true }
            AdminDashboardUiState(
                carregando = false,
                primeiroNome = admin?.nomeCompleto?.substringBefore(" ").orEmpty(),
                filtro = filtroAtual,
                documentos = filtroAtual.statuses?.let { s -> lista.filter { it.document.status in s } } ?: lista,
                totalRascunhos = conta(FiltroDocumento.RASCUNHO),
                totalAndamento = conta(FiltroDocumento.ANDAMENTO),
                totalConcluidos = conta(FiltroDocumento.CONCLUIDO),
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
}