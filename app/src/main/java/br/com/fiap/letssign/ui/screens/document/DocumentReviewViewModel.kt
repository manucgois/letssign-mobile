package br.com.fiap.letssign.ui.screens.document

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.entity.SignerWithUser
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.EnvioResultado
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.DocumentRepository
import br.com.fiap.letssign.util.FileHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DocumentReviewUiState(
    val carregando: Boolean = true,
    val bloqueio: String? = null,
    val documentId: Long = -1,
    val titulo: String = "",
    val nomeArquivo: String = "",
    val totalPaginas: Int = 0,
    val codigoHash: String = "",
    val paginaAtual: Int = 0,
    val paginaBitmap: Bitmap? = null,
    val participantes: List<SignerWithUser> = emptyList(),
    val campos: List<SignatureFieldEntity> = emptyList(),
    val mensagem: String = "",
    val confirmando: Boolean = false,
    val enviando: Boolean = false,
    val erroEnvio: String? = null,
    val tentativasComFalha: Int = 0,
    val simularFalha: Boolean = false,
    val exigirRubrica: Boolean = false,
    val resultado: EnvioResultado? = null
)

@HiltViewModel
class DocumentReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    companion object {
        const val LIMITE_MENSAGEM = 500
        private const val CHAVE_ENVIO = "idempotencyKey"
        private const val LARGURA_RENDER_PX = 1080
    }

    private val documentId: Long = savedStateHandle.get<Long>("documentId") ?: -1L

    /**
     * Chave única DESTE envio, usada em todas as tentativas.
     * Fica no SavedStateHandle: sobrevive até à rotação da tela.
     */
    private val idempotencyKey: String =
        savedStateHandle.get<String>(CHAVE_ENVIO)
            ?: UUID.randomUUID().toString().also { savedStateHandle[CHAVE_ENVIO] = it }

    private var userId: Long = -1L
    private val cachePaginas = mutableMapOf<Int, Bitmap>()

    private val _uiState = MutableStateFlow(DocumentReviewUiState(documentId = documentId))
    val uiState: StateFlow<DocumentReviewUiState> = _uiState.asStateFlow()

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
                _uiState.update { it.copy(carregando = false, bloqueio = "Este documento já foi enviado.") }
                return@launch
            }

            _uiState.update {
                it.copy(
                    titulo = doc.titulo,
                    nomeArquivo = doc.nomeArquivo,
                    totalPaginas = doc.quantidadePaginas,
                    codigoHash = FileHasher.shortCode(doc.hashOriginal),
                    mensagem = doc.mensagemConvite
                        ?: "Olá! Você recebeu o documento \"${doc.titulo}\" para assinatura no Ratifica.",
                    exigirRubrica = doc.exigirRubrica
                )
            }
            carregarPagina(0)

            combine(
                documentRepository.observeParticipants(documentId),
                documentRepository.observeFields(documentId)
            ) { participantes, campos -> participantes to campos }
                .collect { (participantes, campos) ->
                    _uiState.update {
                        it.copy(carregando = false, participantes = participantes, campos = campos)
                    }
                }
        }
    }

    // ---------- Páginas ----------
    private fun carregarPagina(pagina: Int) {
        _uiState.update { it.copy(paginaAtual = pagina) }
        cachePaginas[pagina]?.let { bitmap ->
            _uiState.update { it.copy(paginaBitmap = bitmap) }
            return
        }
        _uiState.update { it.copy(paginaBitmap = null) }
        viewModelScope.launch {
            val bitmap = documentRepository.renderPage(documentId, userId, pagina, LARGURA_RENDER_PX)
            if (bitmap != null) cachePaginas[pagina] = bitmap
            if (_uiState.value.paginaAtual == pagina) {
                _uiState.update { it.copy(paginaBitmap = bitmap) }
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

    // ---------- Mensagem ----------
    fun onMensagemChange(valor: String) {
        _uiState.update { it.copy(mensagem = valor.take(LIMITE_MENSAGEM)) }
    }

    fun alternarSimulacao(ativar: Boolean) = _uiState.update { it.copy(simularFalha = ativar) }

    fun alternarRubrica(ativar: Boolean) = _uiState.update { it.copy(exigirRubrica = ativar) }

    // ---------- Envio ----------
    fun pedirConfirmacao() {
        if (_uiState.value.enviando) return
        _uiState.update { it.copy(confirmando = true) }
    }

    fun cancelarConfirmacao() = _uiState.update { it.copy(confirmando = false) }

    fun enviar() {
        val s = _uiState.value
        if (s.enviando) return
        _uiState.update { it.copy(confirmando = false, enviando = true, erroEnvio = null) }

        viewModelScope.launch {
            documentRepository.sendDocument(
                ownerId = userId,
                documentId = documentId,
                mensagem = s.mensagem,
                idempotencyKey = idempotencyKey,
                exigirRubrica = s.exigirRubrica,
                simularFalha = s.simularFalha
            )
                .onSuccess { resultado ->
                    _uiState.update { it.copy(enviando = false, resultado = resultado) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            enviando = false,
                            erroEnvio = e.message,
                            tentativasComFalha = it.tentativasComFalha + 1
                        )
                    }
                }
        }
    }
}