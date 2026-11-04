@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.signer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.ui.components.CampoVisual
import br.com.fiap.letssign.ui.components.PdfPageView
import br.com.fiap.letssign.ui.components.StatusBadge
import br.com.fiap.letssign.ui.theme.VerdeValidado
import br.com.fiap.letssign.util.DateFormatter

@Composable
fun SignDocumentScreen(
    onBack: () -> Unit,
    onOpenSignature: () -> Unit,
    viewModel: SignDocumentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Ao voltar da tela "Minha assinatura", recarrega para reconhecer o perfil novo
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.atualizar() }

    val resultado = state.resultado
    if (resultado != null) {
        TelaAssinado(
            concluido = resultado.documentoConcluido,
            titulo = state.titulo,
            onVerDocumento = viewModel::verDocumentoAssinado,
            onVoltar = onBack
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            if (!state.carregando && state.bloqueio == null && state.podeAssinar) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    if (state.temPerfil) {
                        Button(
                            onClick = viewModel::abrirConfirmacao,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Outlined.Draw, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Assinar documento")
                        }
                    } else {
                        Text(
                            "Para assinar, crie primeiro a sua assinatura eletrônica.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = onOpenSignature,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) { Text("Criar minha assinatura") }
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
                OutlinedButton(onClick = onBack) { Text("Voltar") }
            }

            else -> ConteudoDocumento(state, viewModel, Modifier.padding(innerPadding))
        }
    }

    if (state.confirmando) DialogoConfirmacao(state, viewModel)
}

@Composable
private fun ConteudoDocumento(
    state: SignDocumentUiState,
    viewModel: SignDocumentViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---------- Cabeçalho ----------
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(state.titulo, style = MaterialTheme.typography.headlineSmall)
            val enviadoEm = state.enviadoEm
            Text(
                "Enviado por ${state.remetente}" +
                        (if (enviadoEm != null) " em ${DateFormatter.dataHora(enviadoEm)}" else ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Você assina como: ${state.meuPapel}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // ---------- Mensagem do remetente ----------
        val mensagem = state.mensagem
        if (!mensagem.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(mensagem, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ---------- Situação ----------
        val assinadoEm = state.meuAssinadoEm
        if (!state.podeAssinar && assinadoEm != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Você assinou em ${DateFormatter.dataHora(assinadoEm)}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        StatusBadge(state.documentoStatus)
                    }
                    val codigo = state.codigoHashFinal
                    if (codigo != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Documento final: $codigo · o manifesto de assinaturas está na última página.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        } else if (state.podeAssinar) {
            val paginas = state.meusCampos.map { it.pagina + 1 }.distinct().sorted()
            Text(
                "Sua assinatura será aplicada em ${state.meusCampos.size} campo(s) · " +
                        (if (paginas.size == 1) "página " else "páginas ") + paginas.joinToString(", ") +
                        ". Os campos em verde mostram onde.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.paginasParaRubricar.isNotEmpty()) {
                Text(
                    "Sua rubrica também será aplicada no rodapé " +
                            (if (state.paginasParaRubricar.size == 1) "da página " else "das páginas ") +
                            state.paginasParaRubricar.joinToString(", ") + ".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ---------- Páginas ----------
        val camposVisiveis = if (state.podeAssinar) {
            state.meusCampos
                .filter { it.pagina == state.paginaAtual }
                .map { CampoVisual(it.id, it.x, it.y, it.largura, it.altura, VerdeValidado, "Assine aqui") }
        } else emptyList() // depois de assinado, o próprio PDF já mostra o carimbo

        PdfPageView(bitmap = state.paginaBitmap, campos = camposVisiveis)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = viewModel::paginaAnterior, enabled = state.paginaAtual > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Página anterior")
            }
            Text("Página ${state.paginaAtual + 1} de ${state.totalPaginas}" + if (state.ehManifesto) " · Manifesto" else "", style = MaterialTheme.typography.titleSmall)
            FilledTonalIconButton(
                onClick = viewModel::proximaPagina,
                enabled = state.paginaAtual < state.totalPaginas - 1
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próxima página")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DialogoConfirmacao(state: SignDocumentUiState, viewModel: SignDocumentViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::fecharConfirmacao,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text("Confirmar assinatura") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Seu carimbo:", style = MaterialTheme.typography.labelLarge)
                Text(
                    state.resumoCarimbo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.aceite, onCheckedChange = viewModel::onAceite)
                    Text(
                        "Li o documento e concordo com o seu conteúdo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedTextField(
                    value = state.senha,
                    onValueChange = viewModel::onSenha,
                    label = { Text("Sua senha") },
                    singleLine = true,
                    visualTransformation = if (state.senhaVisivel) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = viewModel::alternarSenha) {
                            Icon(
                                if (state.senhaVisivel) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (state.senhaVisivel) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                    },
                    isError = state.erroConfirmacao != null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.erroConfirmacao != null) {
                    Text(
                        state.erroConfirmacao ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = viewModel::assinar, enabled = !state.assinando) {
                if (state.assinando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Assinar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::fecharConfirmacao, enabled = !state.assinando) { Text("Cancelar") }
        }
    )
}

@Composable
private fun TelaAssinado(
    concluido: Boolean,
    titulo: String,
    onVerDocumento: () -> Unit,
    onVoltar: () -> Unit
) {
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
                Modifier
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
            Text("Documento assinado!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                if (concluido)
                    "Todas as assinaturas obrigatórias de \"$titulo\" foram concluídas. " +
                            "O documento final, com o manifesto de assinaturas, já está disponível."
                else
                    "Sua assinatura em \"$titulo\" foi registrada. Agora é só aguardar os demais signatários.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onVerDocumento,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Ver documento assinado") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onVoltar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Voltar aos documentos") }
        }
    }
}