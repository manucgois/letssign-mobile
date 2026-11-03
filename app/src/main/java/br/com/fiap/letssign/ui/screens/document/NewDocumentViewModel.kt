package br.com.fiap.letssign.ui.screens.document

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.file.PdfInfo
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewDocumentUiState(
    val carregando: Boolean = false,
    val documentId: Long? = null,      // null = ainda não salvo; com valor = editando um rascunho
    val titulo: String = "",
    val tituloErro: String? = null,
    val pdfUri: Uri? = null,           // arquivo NOVO escolhido nesta tela (ainda não salvo)
    val pdf: PdfInfo? = null,          // o que aparece na prévia
    val totalCampos: Int = 0,
    val analisando: Boolean = false,
    val arquivoErro: String? = null,
    val salvando: Boolean = false,
    val erroGeral: String? = null,
    val confirmandoSaida: Boolean = false,
    val avancarPara: Long? = null,     // evento: ir para a Etapa 2
    val sair: Boolean = false          // evento: fechar a tela
) {
    val modoEdicao: Boolean get() = documentId != null
}

@HiltViewModel
class NewDocumentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val documentIdDaRota: Long = savedStateHandle.get<Long>("documentId") ?: -1L
    private var tituloSalvo: String = ""

    private val _uiState = MutableStateFlow(NewDocumentUiState())
    val uiState: StateFlow<NewDocumentUiState> = _uiState.asStateFlow()

    private val adminId: Long get() = authRepository.currentUser.value?.id ?: -1L

    init {
        if (documentIdDaRota != -1L) carregarRascunho(documentIdDaRota)
    }

    /** Abre um rascunho existente: título, prévia e quantos campos ele já tem. */
    private fun carregarRascunho(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(carregando = true) }
            val doc = documentRepository.getDocument(id, adminId)
            if (doc == null || doc.ownerId != adminId || doc.status != DocumentStatus.RASCUNHO) {
                _uiState.update {
                    it.copy(carregando = false, erroGeral = "Este documento não pode mais ser editado.")
                }
                return@launch
            }
            tituloSalvo = doc.titulo
            val previa = documentRepository.getDraftPreview(id, adminId)
            val campos = documentRepository.observeFields(id).first().size
            _uiState.update {
                it.copy(carregando = false, documentId = id, titulo = doc.titulo, pdf = previa, totalCampos = campos)
            }
        }
    }

    fun onTituloChange(v: String) = _uiState.update { it.copy(titulo = v, tituloErro = null) }

    /** Chamado quando o usuário escolhe um arquivo (null = desistiu no seletor). */
    fun onPdfSelecionado(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(analisando = true, arquivoErro = null) }
            documentRepository.inspectPdf(uri)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            analisando = false,
                            pdfUri = uri,
                            pdf = info,
                            titulo = it.titulo.ifBlank { info.nomeArquivo.removeSuffix(".pdf") }
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(analisando = false, arquivoErro = e.message) } }
        }
    }

    fun continuar() {
        val s = _uiState.value
        if (s.salvando) return

        val tituloErro = when {
            s.titulo.isBlank() -> "Informe o título do documento"
            s.titulo.trim().length < 3 -> "Título muito curto"
            else -> null
        }
        val arquivoErro = if (s.pdf == null) "Selecione um arquivo PDF" else null
        if (tituloErro != null || arquivoErro != null) {
            _uiState.update { it.copy(tituloErro = tituloErro, arquivoErro = arquivoErro ?: it.arquivoErro) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(salvando = true, erroGeral = null) }
            val id = s.documentId

            if (id == null) {
                // 1ª vez: cria o rascunho
                documentRepository.createDraft(adminId, s.titulo, s.pdfUri!!)
                    .onSuccess { novoId ->
                        tituloSalvo = s.titulo.trim()
                        _uiState.update {
                            it.copy(salvando = false, documentId = novoId, pdfUri = null, avancarPara = novoId)
                        }
                    }
                    .onFailure { e -> _uiState.update { it.copy(salvando = false, erroGeral = e.message) } }
            } else {
                // Rascunho existente: só salva se algo mudou
                val mudouTitulo = s.titulo.trim() != tituloSalvo
                if (!mudouTitulo && s.pdfUri == null) {
                    _uiState.update { it.copy(salvando = false, avancarPara = id) }
                    return@launch
                }
                documentRepository.updateDraft(adminId, id, s.titulo, s.pdfUri)
                    .onSuccess {
                        tituloSalvo = s.titulo.trim()
                        _uiState.update {
                            it.copy(
                                salvando = false,
                                pdfUri = null,
                                totalCampos = if (s.pdfUri != null) 0 else it.totalCampos,
                                avancarPara = id
                            )
                        }
                    }
                    .onFailure { e -> _uiState.update { it.copy(salvando = false, erroGeral = e.message) } }
            }
        }
    }

    fun onAvancou() = _uiState.update { it.copy(avancarPara = null) }

    // ---------- Saída ----------
    /** Botão voltar: sem rascunho salvo, só sai; com rascunho, pergunta o que fazer. */
    fun pedirSaida() {
        if (_uiState.value.modoEdicao) _uiState.update { it.copy(confirmandoSaida = true) }
        else _uiState.update { it.copy(sair = true) }
    }

    fun continuarEditando() = _uiState.update { it.copy(confirmandoSaida = false) }

    fun salvarESair() = _uiState.update { it.copy(confirmandoSaida = false, sair = true) }

    fun descartarRascunho() {
        val id = _uiState.value.documentId ?: return
        viewModelScope.launch {
            documentRepository.deleteDraft(adminId, id)
                .onSuccess { _uiState.update { it.copy(confirmandoSaida = false, sair = true) } }
                .onFailure { e -> _uiState.update { it.copy(confirmandoSaida = false, erroGeral = e.message) } }
        }
    }
}