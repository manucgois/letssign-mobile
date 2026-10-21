package br.com.fiap.letssign.ui.screens.document

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.file.PdfInfo
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewDocumentUiState(
    val titulo: String = "",
    val tituloErro: String? = null,
    val pdfUri: Uri? = null,
    val pdf: PdfInfo? = null,
    val analisando: Boolean = false,
    val arquivoErro: String? = null,
    val salvando: Boolean = false,
    val erroGeral: String? = null,
    val criadoId: Long? = null
)

@HiltViewModel
class NewDocumentViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewDocumentUiState())
    val uiState: StateFlow<NewDocumentUiState> = _uiState.asStateFlow()

    fun onTituloChange(v: String) = _uiState.update { it.copy(titulo = v, tituloErro = null) }

    /** Chamado quando o usuário escolhe um arquivo (null = cancelou). */
    fun onPdfSelecionado(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(analisando = true, arquivoErro = null, pdf = null, pdfUri = null) }

            documentRepository.inspectPdf(uri)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            analisando = false,
                            pdfUri = uri,
                            pdf = info,
                            // Sugere o nome do arquivo como título, se ainda estiver vazio
                            titulo = it.titulo.ifBlank { info.nomeArquivo.removeSuffix(".pdf") }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(analisando = false, arquivoErro = e.message) }
                }
        }
    }

    fun continuar() {
        val s = _uiState.value
        if (s.salvando) return
        val admin = authRepository.currentUser.value ?: return

        val tituloErro = when {
            s.titulo.isBlank() -> "Informe o título do documento"
            s.titulo.trim().length < 3 -> "Título muito curto"
            else -> null
        }
        val arquivoErro = if (s.pdfUri == null) "Selecione um arquivo PDF" else null

        if (tituloErro != null || arquivoErro != null) {
            _uiState.update { it.copy(tituloErro = tituloErro, arquivoErro = arquivoErro ?: it.arquivoErro) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(salvando = true, erroGeral = null) }
            documentRepository.createDraft(admin.id, s.titulo, s.pdfUri!!)
                .onSuccess { id -> _uiState.update { it.copy(salvando = false, criadoId = id) } }
                .onFailure { e -> _uiState.update { it.copy(salvando = false, erroGeral = e.message) } }
        }
    }
}