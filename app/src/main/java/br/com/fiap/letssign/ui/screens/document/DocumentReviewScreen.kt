@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.document

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.ui.components.CampoVisual
import br.com.fiap.letssign.ui.components.InitialsAvatar
import br.com.fiap.letssign.ui.components.PdfPageView
import br.com.fiap.letssign.ui.components.StepIndicator
import br.com.fiap.letssign.ui.components.corDoSignatario
import br.com.fiap.letssign.ui.components.FecharCadastroButton

@Composable
fun DocumentReviewScreen(
    onBack: () -> Unit,
    onExit: () -> Unit,
    onEditSigners: () -> Unit,
    onEditFields: () -> Unit,
    onFinish: () -> Unit,
    viewModel: DocumentReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Depois de enviado, o "voltar" leva ao início (não volta para as etapas)
    BackHandler(enabled = state.resultado != null) { onFinish() }

    if (state.resultado != null) {
        TelaSucesso(state, onFinish)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo documento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (state.documentId != -1L) {
                        FecharCadastroButton(
                            documentId = state.documentId,
                            onSairParaPainel = onExit
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (state.bloqueio == null && !state.carregando) {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Button(
                        onClick = viewModel::pedirConfirmacao,
                        enabled = !state.enviando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (state.enviando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Enviando…")
                        } else {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.erroEnvio != null) "Tentar novamente" else "Enviar para assinatura")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            state.carregando -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.bloqueio != null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.bloqueio ?: "", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onFinish) { Text("Voltar ao início") }
            }

            else -> ConteudoRevisao(
                state = state,
                viewModel = viewModel,
                onEditSigners = onEditSigners,
                onEditFields = onEditFields,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (state.confirmando) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarConfirmacao,
            icon = { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null) },
            title = { Text("Enviar documento?") },
            text = {
                Text(
                    "\"${state.titulo}\" será enviado para ${state.participantes.size} " +
                            "signatário(s). Depois do envio, o documento não poderá ser alterado."
                )
            },
            confirmButton = { Button(onClick = viewModel::enviar) { Text("Enviar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarConfirmacao) { Text("Revisar") } }
        )
    }
}

@Composable
private fun ConteudoRevisao(
    state: DocumentReviewUiState,
    viewModel: DocumentReviewViewModel,
    onEditSigners: () -> Unit,
    onEditFields: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StepIndicator(etapaAtual = 4)

        Column {
            Text("Revise antes de enviar", style = MaterialTheme.typography.titleMedium)
            Text(
                "Confira o documento, quem vai assinar e onde. Está tudo certo? É só enviar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---------- Documento ----------
        CabecalhoSecao("Documento final", onEditar = onEditFields, textoEditar = "Editar campos")
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${state.nomeArquivo} · ${state.totalPaginas} página(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Impressão digital: ${state.codigoHash}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        val camposDaPagina = state.campos
            .filter { it.pagina == state.paginaAtual }
            .map { campo ->
                val indice = state.participantes.indexOfFirst { it.signer.id == campo.signerId }
                CampoVisual(
                    id = campo.id,
                    x = campo.x,
                    y = campo.y,
                    largura = campo.largura,
                    altura = campo.altura,
                    cor = corDoSignatario(indice),
                    rotulo = state.participantes.getOrNull(indice)
                        ?.user?.nomeCompleto?.substringBefore(" ") ?: "?"
                )
            }
        PdfPageView(bitmap = state.paginaBitmap, campos = camposDaPagina)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = viewModel::paginaAnterior, enabled = state.paginaAtual > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Página anterior")
            }
            Text("Página ${state.paginaAtual + 1} de ${state.totalPaginas}", style = MaterialTheme.typography.titleSmall)
            FilledTonalIconButton(
                onClick = viewModel::proximaPagina,
                enabled = state.paginaAtual < state.totalPaginas - 1
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próxima página")
            }
        }

        // ---------- Signatários ----------
        CabecalhoSecao("Signatários (${state.participantes.size})", onEditar = onEditSigners, textoEditar = "Editar")
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                state.participantes.forEachIndexed { indice, p ->
                    val camposDele = state.campos.filter { it.signerId == p.signer.id }
                    val paginas = camposDele.map { it.pagina + 1 }.distinct().sorted().joinToString(", ")
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            InitialsAvatar(p.user.nomeCompleto, tamanho = 40.dp)
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(corDoSignatario(indice), CircleShape)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.user.nomeCompleto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                p.user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${p.signer.papel.label} · " +
                                        (if (p.signer.obrigatorio) "Obrigatório" else "Opcional") +
                                        " · ${camposDele.size} campo(s) · pág. $paginas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ---------- Mensagem ----------
        Text("Mensagem do convite", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.mensagem,
            onValueChange = viewModel::onMensagemChange,
            placeholder = { Text("Escreva uma mensagem para os signatários") },
            minLines = 3,
            maxLines = 6,
            supportingText = {
                Text("${state.mensagem.length}/${DocumentReviewViewModel.LIMITE_MENSAGEM}")
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // ---------- Aviso de preservação ----------
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Depois do envio, o documento e suas configurações ficam protegidos. " +
                            "Qualquer alteração vai gerar uma nova versão.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ---------- Erro de envio ----------
        if (state.erroEnvio != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Não foi possível enviar",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            state.erroEnvio ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Seu rascunho está salvo. Toque em \"Tentar novamente\": " +
                                    "nenhuma solicitação duplicada será criada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // ---------- Modo demonstração ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Simular falha de conexão", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Modo demonstração: testa o aviso de erro e a nova tentativa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = state.simularFalha, onCheckedChange = viewModel::alternarSimulacao)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CabecalhoSecao(titulo: String, onEditar: () -> Unit, textoEditar: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onEditar) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(textoEditar)
        }
    }
}

@Composable
private fun TelaSucesso(state: DocumentReviewUiState, onFinish: () -> Unit) {
    val resultado = state.resultado ?: return
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("Documento enviado!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "${resultado.totalSignatarios} signatário(s) convidado(s) para assinar \"${state.titulo}\".",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (resultado.jaEnviadoAntes) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Este envio já estava registrado. Nenhuma solicitação duplicada foi criada.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Você pode acompanhar as assinaturas pelo painel.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Voltar ao início") }
        }
    }
}