package br.com.fiap.letssign.ui.screens.document

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.entity.SignerWithUser
import br.com.fiap.letssign.domain.model.DocumentStatus
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

data class DocumentFieldsUiState(
    val carregando: Boolean = true,
    val bloqueio: String? = null,
    val documentId: Long = -1,
    val titulo: String = "",
    val totalPaginas: Int = 0,
    val paginaAtual: Int = 0,          // começa em 0 (no banco); na tela mostramos +1
    val paginaBitmap: Bitmap? = null,
    val renderizando: Boolean = false,
    val participantes: List<SignerWithUser> = emptyList(),
    val participanteSelecionadoId: Long? = null,
    val campos: List<SignatureFieldEntity> = emptyList(),
    val campoSelecionadoId: Long? = null,
    val erro: String? = null,
    val avancar: Boolean = false
)

@HiltViewModel
class DocumentFieldsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    companion object {
        /** Tamanho padrão do campo, em proporção da página (30% da largura, 7% da altura). */
        const val LARGURA_PADRAO = 0.30f
        const val ALTURA_PADRAO = 0.07f
        private const val LARGURA_RENDER_PX = 1080
    }

    private val documentId: Long = savedStateHandle.get<Long>("documentId") ?: -1L
    private var userId: Long = -1L

    /** Páginas já desenhadas ficam guardadas: voltar a uma página é instantâneo. */
    private val cachePaginas = mutableMapOf<Int, Bitmap>()

    private val _uiState = MutableStateFlow(DocumentFieldsUiState(documentId = documentId))
    val uiState: StateFlow<DocumentFieldsUiState> = _uiState.asStateFlow()

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
            userId = admin.id

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

            _uiState.update { it.copy(titulo = doc.titulo, totalPaginas = doc.quantidadePaginas) }
            carregarPagina(0)

            // Participantes e campos "ao vivo"
            combine(
                documentRepository.observeParticipants(documentId),
                documentRepository.observeFields(documentId)
            ) { participantes, campos -> participantes to campos }
                .collect { (participantes, campos) ->
                    _uiState.update { s ->
                        s.copy(
                            carregando = false,
                            participantes = participantes,
                            campos = campos,
                            // Mantém a seleção se ainda existir; senão, seleciona o primeiro
                            participanteSelecionadoId = s.participanteSelecionadoId
                                ?.takeIf { id -> participantes.any { it.signer.id == id } }
                                ?: participantes.firstOrNull()?.signer?.id,
                            campoSelecionadoId = s.campoSelecionadoId
                                ?.takeIf { id -> campos.any { it.id == id } }
                        )
                    }
                }
        }
    }

    // ---------- Páginas ----------
    private fun carregarPagina(pagina: Int) {
        _uiState.update { it.copy(paginaAtual = pagina, campoSelecionadoId = null) }

        cachePaginas[pagina]?.let { bitmap ->
            _uiState.update { it.copy(paginaBitmap = bitmap, renderizando = false) }
            return
        }

        _uiState.update { it.copy(paginaBitmap = null, renderizando = true) }
        viewModelScope.launch {
            val bitmap = documentRepository.renderPage(documentId, userId, pagina, LARGURA_RENDER_PX)
            if (bitmap != null) cachePaginas[pagina] = bitmap
            // Só aplica se o usuário ainda estiver nesta página
            if (_uiState.value.paginaAtual == pagina) {
                _uiState.update {
                    it.copy(
                        paginaBitmap = bitmap,
                        renderizando = false,
                        erro = if (bitmap == null) "Não foi possível exibir esta página." else it.erro
                    )
                }
            }
        }
    }

    fun paginaAnterior() {
        val atual = _uiState.value.paginaAtual
        if (atual > 0) carregarPagina(atual - 1)
    }

    fun proximaPagina() {
        val s = _uiState.value
        if (s.paginaAtual < s.totalPaginas - 1) carregarPagina(s.paginaAtual + 1)
    }

    // ---------- Seleção ----------
    fun selecionarParticipante(signerId: Long) =
        _uiState.update { it.copy(participanteSelecionadoId = signerId, campoSelecionadoId = null, erro = null) }

    fun selecionarCampo(campoId: Long) =
        _uiState.update { it.copy(campoSelecionadoId = campoId, erro = null) }

    /**
     * Toque numa área vazia da página:
     * - se havia um campo selecionado, apenas tira a seleção;
     * - senão, cria um campo para o signatário escolhido, centralizado no toque.
     */
    fun tocarNaPagina(xRelativo: Float, yRelativo: Float) {
        if (_uiState.value.campoSelecionadoId != null) {
            _uiState.update { it.copy(campoSelecionadoId = null) }
            return
        }
        adicionarCampo(xRelativo, yRelativo)
    }

    private fun adicionarCampo(xRelativo: Float, yRelativo: Float) {
        val s = _uiState.value
        val signerId = s.participanteSelecionadoId
        if (signerId == null) {
            _uiState.update { it.copy(erro = "Volte à etapa anterior e adicione os signatários.") }
            return
        }

        // Centraliza o campo no toque, sem deixar sair da página
        val x = (xRelativo - LARGURA_PADRAO / 2).coerceIn(0f, 1f - LARGURA_PADRAO)
        val y = (yRelativo - ALTURA_PADRAO / 2).coerceIn(0f, 1f - ALTURA_PADRAO)

        viewModelScope.launch {
            documentRepository.addField(
                userId,
                SignatureFieldEntity(
                    documentId = documentId,
                    signerId = signerId,
                    pagina = s.paginaAtual,
                    x = x,
                    y = y,
                    largura = LARGURA_PADRAO,
                    altura = ALTURA_PADRAO
                )
            )
                .onSuccess { id -> _uiState.update { it.copy(campoSelecionadoId = id, erro = null) } }
                .onFailure { e -> _uiState.update { it.copy(erro = e.message) } }
        }
    }

    // ---------- Mover e excluir ----------
    fun moverCampo(campo: SignatureFieldEntity, xRelativo: Float, yRelativo: Float) {
        val atualizado = campo.copy(
            x = xRelativo.coerceIn(0f, 1f - campo.largura),
            y = yRelativo.coerceIn(0f, 1f - campo.altura)
        )
        viewModelScope.launch {
            documentRepository.updateField(userId, atualizado)
                .onFailure { e -> _uiState.update { it.copy(erro = e.message) } }
        }
    }

    fun excluirCampoSelecionado() {
        val s = _uiState.value
        val campo = s.campos.firstOrNull { it.id == s.campoSelecionadoId } ?: return
        viewModelScope.launch {
            documentRepository.deleteField(userId, campo)
                .onSuccess { _uiState.update { it.copy(campoSelecionadoId = null) } }
                .onFailure { e -> _uiState.update { it.copy(erro = e.message) } }
        }
    }

    // ---------- Avançar ----------
    fun continuar() {
        val s = _uiState.value
        val semCampo = s.participantes.filter { p -> s.campos.none { it.signerId == p.signer.id } }

        val erro = when {
            s.participantes.isEmpty() ->
                "Volte à etapa anterior e adicione os signatários."
            semCampo.isNotEmpty() ->
                "Adicione pelo menos um campo para: " +
                        semCampo.joinToString(", ") { it.user.nomeCompleto.substringBefore(" ") }
            else -> null
        }
        _uiState.update { if (erro != null) it.copy(erro = erro) else it.copy(erro = null, avancar = true) }
    }

    fun onAvancou() = _uiState.update { it.copy(avancar = false) }
}