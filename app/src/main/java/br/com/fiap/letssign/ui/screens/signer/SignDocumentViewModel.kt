package br.com.fiap.letssign.ui.screens.signer

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.domain.model.AssinaturaResultado
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.SigningRepository
import br.com.fiap.letssign.util.FileHasher
import br.com.fiap.letssign.util.StampFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import br.com.fiap.letssign.domain.repository.DocumentRepository

data class SignDocumentUiState(
    val carregando: Boolean = true,
    val bloqueio: String? = null,
    val documentId: Long = -1,
    val titulo: String = "",
    val remetente: String = "",
    val enviadoEm: Long? = null,
    val mensagem: String? = null,
    val meuPapel: String = "",
    val meuStatus: SignerStatus = SignerStatus.PENDENTE,
    val meuAssinadoEm: Long? = null,
    val documentoStatus: DocumentStatus = DocumentStatus.ENVIADO,
    val codigoHashFinal: String? = null,
    val meusCampos: List<SignatureFieldEntity> = emptyList(),
    val paginasParaRubricar: List<Int> = emptyList(),
    val totalPaginas: Int = 0,
    val paginasOriginais: Int = 0,
    val paginaAtual: Int = 0,
    val paginaBitmap: Bitmap? = null,
    val temPerfil: Boolean = false,
    val resumoCarimbo: String = "",
    // Confirmação
    val confirmando: Boolean = false,
    val aceite: Boolean = false,
    val senha: String = "",
    val senhaVisivel: Boolean = false,
    val erroConfirmacao: String? = null,
    val assinando: Boolean = false,
    val resultado: AssinaturaResultado? = null
) {
    val podeAssinar: Boolean
        get() = meuStatus == SignerStatus.PENDENTE &&
                (documentoStatus == DocumentStatus.ENVIADO || documentoStatus == DocumentStatus.PARCIALMENTE_ASSINADO)

    /** Páginas além das do documento original = manifesto de assinaturas. */
    val ehManifesto: Boolean
        get() = paginasOriginais > 0 && paginaAtual >= paginasOriginais

}

@HiltViewModel
class SignDocumentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val signingRepository: SigningRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val documentId: Long = savedStateHandle.get<Long>("documentId") ?: -1L
    private val userId: Long get() = authRepository.currentUser.value?.id ?: -1L
    private val cachePaginas = mutableMapOf<Int, Bitmap>()

    private val _uiState = MutableStateFlow(SignDocumentUiState(documentId = documentId))
    val uiState: StateFlow<SignDocumentUiState> = _uiState.asStateFlow()

    init {
        carregar(primeiraVez = true)
    }

    private fun carregar(primeiraVez: Boolean) {
        viewModelScope.launch {
            val ctx = signingRepository.getSigningContext(documentId, userId)
            if (ctx == null) {
                _uiState.update { it.copy(carregando = false, bloqueio = "Documento não encontrado.") }
                return@launch
            }
            if (primeiraVez) signingRepository.registerView(documentId, userId)

            val doc = ctx.document
            val perfil = ctx.perfil
            val paginaInicial = if (ctx.eu.status == SignerStatus.PENDENTE)
                ctx.meusCampos.minOfOrNull { it.pagina } ?: 0 else 0
            val hashFinal = doc.hashFinal
            val total = documentRepository.countPages(documentId, userId, versaoFinal = true)

            _uiState.update {
                it.copy(
                    carregando = false,
                    titulo = doc.titulo,
                    remetente = ctx.remetente,
                    enviadoEm = doc.enviadoEm,
                    mensagem = doc.mensagemConvite,
                    meuPapel = ctx.eu.papel.label,
                    meuStatus = ctx.eu.status,
                    meuAssinadoEm = ctx.eu.assinadoEm,
                    documentoStatus = doc.status,
                    codigoHashFinal = hashFinal?.let { h -> FileHasher.shortCode(h) },
                    meusCampos = ctx.meusCampos,
                    totalPaginas = total,
                    paginasOriginais = doc.quantidadePaginas,
                    temPerfil = perfil != null,
                    resumoCarimbo = perfil?.let { p ->
                        StampFormatter.linhaPrincipal(p.nomeExibicao, p.cargo, p.tipoIdentificacao, p.numeroIdentificacao)
                    }.orEmpty(),
                    paginasParaRubricar = if (doc.exigirRubrica)
                        (0 until doc.quantidadePaginas)
                            .filter { p -> ctx.meusCampos.none { it.pagina == p } }
                            .map { it + 1 }
                    else emptyList(),
                )
            }
            cachePaginas.clear()
            carregarPagina(paginaInicial)
        }
    }

    /** Chamado quando a tela volta a aparecer (ex.: depois de criar a assinatura). */
    fun atualizar() {
        if (_uiState.value.resultado == null && !_uiState.value.carregando) carregar(primeiraVez = false)
    }

    // ---------- Páginas ----------
    private fun carregarPagina(pagina: Int) {
        _uiState.update { it.copy(paginaAtual = pagina) }
        cachePaginas[pagina]?.let { bmp ->
            _uiState.update { it.copy(paginaBitmap = bmp) }
            return
        }
        _uiState.update { it.copy(paginaBitmap = null) }
        viewModelScope.launch {
            // versaoAssinada = true: mostra as assinaturas já feitas (se existirem)
            val bmp = signingRepository.renderPage(documentId, userId, pagina, 1080, versaoAssinada = true)
            if (bmp != null) cachePaginas[pagina] = bmp
            if (_uiState.value.paginaAtual == pagina) _uiState.update { it.copy(paginaBitmap = bmp) }
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

    // ---------- Confirmação ----------
    fun abrirConfirmacao() = _uiState.update {
        it.copy(confirmando = true, aceite = false, senha = "", erroConfirmacao = null)
    }

    fun fecharConfirmacao() {
        if (!_uiState.value.assinando) _uiState.update { it.copy(confirmando = false) }
    }

    fun onAceite(valor: Boolean) = _uiState.update { it.copy(aceite = valor, erroConfirmacao = null) }
    fun onSenha(valor: String) = _uiState.update { it.copy(senha = valor, erroConfirmacao = null) }
    fun alternarSenha() = _uiState.update { it.copy(senhaVisivel = !it.senhaVisivel) }

    fun assinar() {
        val s = _uiState.value
        if (s.assinando) return
        val erro = when {
            !s.aceite -> "Confirme que leu o documento e concorda com o conteúdo."
            s.senha.isBlank() -> "Informe sua senha para confirmar."
            else -> null
        }
        if (erro != null) {
            _uiState.update { it.copy(erroConfirmacao = erro) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(assinando = true, erroConfirmacao = null) }
            signingRepository.sign(documentId, userId, s.senha)
                .onSuccess { resultado ->
                    _uiState.update {
                        it.copy(assinando = false, confirmando = false, senha = "", resultado = resultado)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(assinando = false, erroConfirmacao = e.message) }
                }
        }
    }

    /** Sai da tela de sucesso e mostra o documento já com a assinatura. */
    fun verDocumentoAssinado() {
        _uiState.update { it.copy(resultado = null, carregando = true) }
        carregar(primeiraVez = false)
    }
}