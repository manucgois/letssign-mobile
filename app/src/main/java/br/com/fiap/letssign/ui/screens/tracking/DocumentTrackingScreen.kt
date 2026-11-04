@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.tracking

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.local.entity.AuditEventEntity
import br.com.fiap.letssign.domain.model.AuditEventType
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.ui.components.InitialsAvatar
import br.com.fiap.letssign.ui.components.PdfPageView
import br.com.fiap.letssign.ui.components.StatusBadge
import br.com.fiap.letssign.ui.components.corDoSignatario
import br.com.fiap.letssign.util.DateFormatter
import br.com.fiap.letssign.util.FileHasher
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton

@Composable
fun DocumentTrackingScreen(
    onBack: () -> Unit,
    onSign: (Long) -> Unit,
    viewModel: DocumentTrackingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acompanhamento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        val doc = state.documento
        when {
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

            state.carregando || doc == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            else -> Conteudo(state, viewModel, onSign, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun Conteudo(
    state: DocumentTrackingUiState,
    viewModel: DocumentTrackingViewModel,
    onSign: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val doc = state.documento ?: return
    val context = LocalContext.current
    val total = state.participantes.size
    val assinados = state.participantes.count { it.signer.status == SignerStatus.ASSINADO }
    val concluido = doc.status == DocumentStatus.CONCLUIDO
    val totalPaginas = if (concluido) doc.quantidadePaginas + 1 else doc.quantidadePaginas

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---------- Situação geral ----------
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(doc.titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    StatusBadge(doc.status)
                }
                Text(
                    "$assinados de $total assinatura(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BarraProgresso(if (total == 0) 0f else assinados.toFloat() / total)

                LinhaData("Criado em", doc.criadoEm)
                doc.enviadoEm?.let { LinhaData("Enviado em", it) }
                doc.concluidoEm?.let { LinhaData("Concluído em", it) }
            }
        }

        // ---------- O admin também precisa assinar ----------
        if (state.adminPodeAssinar) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Você também é signatário deste documento", style = MaterialTheme.typography.titleSmall)
                    Button(onClick = { onSign(doc.id) }) {
                        Icon(Icons.Outlined.Draw, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Assinar agora")
                    }
                }
            }
        }

        // ---------- Situação individual ----------
        Text("Signatários", style = MaterialTheme.typography.titleMedium)
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                state.participantes.forEachIndexed { indice, p ->
                    val assinadoEm = p.signer.assinadoEm
                    val visualizou = state.eventos.any {
                        it.tipo == AuditEventType.VISUALIZADO && it.userId == p.user.id
                    }
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                            Text(
                                p.user.nomeCompleto + if (p.user.id == state.adminId) " (você)" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                p.signer.papel.label + if (p.signer.obrigatorio) "" else " · opcional",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                when {
                                    assinadoEm != null -> "Assinou em ${DateFormatter.dataHora(assinadoEm)}"
                                    visualizou -> "Visualizou · aguardando assinatura"
                                    else -> "Aguardando assinatura"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (assinadoEm != null) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ---------- Documento ----------
        Text(if (concluido) "Documento final" else "Documento", style = MaterialTheme.typography.titleMedium)

        val hashFinal = doc.hashFinal
        val arquivoFinal = doc.caminhoArquivoFinal
        if (concluido && hashFinal != null && arquivoFinal != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Impressão digital final: ${FileHasher.shortCode(hashFinal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "O manifesto de assinaturas está na última página.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = {
                        runCatching {
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", File(arquivoFinal)
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, doc.titulo)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartilhar documento assinado"))
                        }.onFailure {
                            Toast.makeText(context, "Não foi possível compartilhar o arquivo.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Compartilhar PDF assinado")
                    }
                }
            }
        }

        // ---------- Prévia do documento (recolhida por padrão) ----------
        var mostrarDocumento by remember { mutableStateOf(false) }

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${doc.quantidadePaginas} página(s)" + if (concluido) " + manifesto" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        doc.nomeArquivo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                TextButton(onClick = { mostrarDocumento = !mostrarDocumento }) {
                    Text(if (mostrarDocumento) "Ocultar" else "Ver documento")
                    Icon(
                        if (mostrarDocumento) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = mostrarDocumento,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PdfPageView(bitmap = state.paginaBitmap, campos = emptyList())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(onClick = viewModel::paginaAnterior, enabled = state.paginaAtual > 0) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Página anterior")
                    }
                    Text(
                        if (concluido && state.paginaAtual == totalPaginas - 1) "Manifesto"
                        else "Página ${state.paginaAtual + 1} de ${doc.quantidadePaginas}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    FilledTonalIconButton(
                        onClick = viewModel::proximaPagina,
                        enabled = state.paginaAtual < totalPaginas - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próxima página")
                    }
                }
            }
        }

        // ---------- Histórico ----------
        Text("Histórico", style = MaterialTheme.typography.titleMedium)
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                val eventos = state.eventos.sortedBy { it.dataHora }
                eventos.forEachIndexed { i, evento ->
                    ItemLinhaDoTempo(evento, ultimo = i == eventos.lastIndex)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BarraProgresso(progresso: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progresso.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun LinhaData(rotulo: String, millis: Long) {
    Row {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(DateFormatter.dataHora(millis), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ItemLinhaDoTempo(evento: AuditEventEntity, ultimo: Boolean) {
    val icone: ImageVector = when (evento.tipo) {
        AuditEventType.CRIADO -> Icons.AutoMirrored.Outlined.NoteAdd
        AuditEventType.ENVIADO -> Icons.AutoMirrored.Outlined.Send
        AuditEventType.VISUALIZADO -> Icons.Outlined.Visibility
        AuditEventType.ASSINADO -> Icons.Outlined.Draw
        AuditEventType.CONCLUIDO -> Icons.Outlined.TaskAlt
        AuditEventType.RECUSADO -> Icons.Outlined.Close
        AuditEventType.NOVA_VERSAO -> Icons.Outlined.ContentCopy
    }
    Row {
        // Coluna da esquerda: ícone + linha vertical ligando ao próximo evento
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!ultimo) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(top = 4.dp, bottom = if (ultimo) 0.dp else 12.dp)) {
            Text(evento.tipo.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            evento.detalhe?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                DateFormatter.dataHora(evento.dataHora),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}