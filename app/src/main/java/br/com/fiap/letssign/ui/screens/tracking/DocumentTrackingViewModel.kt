package br.com.fiap.letssign.ui.screens.tracking

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.AuditEventEntity
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.SignerWithUser
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocumentTrackingUiState(
    val carregando: Boolean = true,
    val bloqueio: String? = null,
    val documentId: Long = -1,
    val adminId: Long = -1,
    val documento: DocumentEntity? = null,
    val participantes: List<SignerWithUser> = emptyList(),
    val eventos: List<AuditEventEntity> = emptyList(),
    val adminPodeAssinar: Boolean = false,
    val paginaAtual: Int = 0,
    val paginaBitmap: Bitmap? = null,
    val totalPaginas: Int = 0,
    // Cancelamento
    val mostrandoCancelamento: Boolean = false,
    val motivoCancelamento: String = "",
    val cancelando: Boolean = false,
    val erroCancelamento: String? = null
) {
    /** Páginas além das do documento original = manifesto de assinaturas. */
    val ehManifesto: Boolean
        get() {
            val originais = documento?.quantidadePaginas ?: return false
            return paginaAtual >= originais
        }

    /** Só documentos aguardando assinatura podem ser cancelados. */
    val podeCancelar: Boolean
        get() = documento?.status == DocumentStatus.ENVIADO ||
                documento?.status == DocumentStatus.PARCIALMENTE_ASSINADO
}

@HiltViewModel
class DocumentTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val documentId: Long = savedStateHandle.get<Long>("documentId") ?: -1L
    private val cachePaginas = mutableMapOf<Int, Bitmap>()

    private val _uiState = MutableStateFlow(DocumentTrackingUiState(documentId = documentId))
    val uiState: StateFlow<DocumentTrackingUiState> = _uiState.asStateFlow()

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
            // Segurança: só o dono do documento acompanha
            val doc = documentRepository.getDocument(documentId, admin.id)
            if (doc == null || doc.ownerId != admin.id) {
                _uiState.update { it.copy(carregando = false, bloqueio = "Documento não encontrado.") }
                return@launch
            }
            _uiState.update { it.copy(adminId = admin.id) }

            // Tudo "ao vivo": se alguém assinar, a tela se atualiza sozinha
            combine(
                documentRepository.observeDocument(documentId),
                documentRepository.observeParticipants(documentId),
                documentRepository.observeAuditEvents(documentId)
            ) { d, p, e -> Triple(d, p, e) }
                .collect { (d, participantes, eventos) ->
                    if (d == null) return@collect
                    val arquivoMudou =
                        d.caminhoArquivoFinal != _uiState.value.documento?.caminhoArquivoFinal

                    _uiState.update {
                        it.copy(
                            carregando = false,
                            documento = d,
                            participantes = participantes,
                            eventos = eventos,
                            adminPodeAssinar = participantes.any { s ->
                                s.user.id == admin.id && s.signer.status == SignerStatus.PENDENTE
                            } && (d.status == DocumentStatus.ENVIADO ||
                                    d.status == DocumentStatus.PARCIALMENTE_ASSINADO)
                        )
                    }

                    // Nova versão assinada → reconta e redesenha as páginas
                    if (arquivoMudou || _uiState.value.totalPaginas == 0) {
                        val total = documentRepository.countPages(documentId, admin.id, versaoFinal = true)
                        _uiState.update {
                            it.copy(
                                totalPaginas = total,
                                paginaAtual = it.paginaAtual.coerceAtMost((total - 1).coerceAtLeast(0))
                            )
                        }
                    }
                    if (arquivoMudou || _uiState.value.paginaBitmap == null) {
                        cachePaginas.clear()
                        carregarPagina(_uiState.value.paginaAtual)
                    }
                }
        }
    }

    private fun carregarPagina(pagina: Int) {
        _uiState.update { it.copy(paginaAtual = pagina) }
        cachePaginas[pagina]?.let { bmp ->
            _uiState.update { it.copy(paginaBitmap = bmp) }
            return
        }
        viewModelScope.launch {
            val bmp = documentRepository.renderPage(
                documentId, _uiState.value.adminId, pagina, 1080, versaoFinal = true
            )
            if (bmp != null) cachePaginas[pagina] = bmp
            if (_uiState.value.paginaAtual == pagina) _uiState.update { it.copy(paginaBitmap = bmp) }
        }
    }

    /** Total de páginas do arquivo exibido (o PDF final tem as páginas do manifesto a mais). */
    private fun totalPaginasExibidas(): Int = _uiState.value.totalPaginas

    fun paginaAnterior() {
        val atual = _uiState.value.paginaAtual
        if (atual > 0) carregarPagina(atual - 1)
    }

    fun proximaPagina() {
        val atual = _uiState.value.paginaAtual
        if (atual < totalPaginasExibidas() - 1) carregarPagina(atual + 1)
    }

    // ---------- Cancelamento ----------
    fun abrirCancelamento() = _uiState.update {
        it.copy(mostrandoCancelamento = true, motivoCancelamento = "", erroCancelamento = null)
    }

    fun fecharCancelamento() {
        if (!_uiState.value.cancelando) _uiState.update { it.copy(mostrandoCancelamento = false) }
    }

    fun onMotivoCancelamento(valor: String) =
        _uiState.update { it.copy(motivoCancelamento = valor, erroCancelamento = null) }

    fun confirmarCancelamento() {
        val s = _uiState.value
        if (s.cancelando) return
        if (s.motivoCancelamento.isBlank()) {
            _uiState.update { it.copy(erroCancelamento = "Informe o motivo do cancelamento.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cancelando = true, erroCancelamento = null) }
            documentRepository.cancelDocument(s.adminId, documentId, s.motivoCancelamento)
                .onSuccess {
                    // A tela se atualiza sozinha (o documento é observado "ao vivo")
                    _uiState.update { it.copy(cancelando = false, mostrandoCancelamento = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(cancelando = false, erroCancelamento = e.message) }
                }
        }
    }
}