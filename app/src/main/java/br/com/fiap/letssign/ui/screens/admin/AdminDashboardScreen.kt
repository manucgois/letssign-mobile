@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.local.entity.DocumentListItem
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.ui.components.CardResumoCompacto
import br.com.fiap.letssign.ui.components.DocumentoResumoCard
import br.com.fiap.letssign.ui.components.HeroHeader
import br.com.fiap.letssign.ui.components.corDoStatus
import br.com.fiap.letssign.ui.components.rotuloDoStatus
import br.com.fiap.letssign.util.DateFormatter

@Composable
fun AdminDashboardScreen(
    onNewDocument: () -> Unit,
    onContinueDraft: (Long) -> Unit,
    onOpenDocument: (Long) -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // O documento em andamento, quando só existe um (o atalho do cabeçalho abre direto)
    val unicoEmAndamento = state.documentos
        .takeIf { state.filtro == FiltroDocumento.TODOS }
        ?.singleOrNull {
            it.document.status == DocumentStatus.ENVIADO ||
                    it.document.status == DocumentStatus.PARCIALMENTE_ASSINADO
        }

    Scaffold(
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewDocument,
                icon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null) },
                text = { Text("Novo documento") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---------- Cabeçalho ----------
            item {
                val emAndamento = state.totalAndamento
                HeroHeader(
                    saudacao = "Olá, ${state.primeiroNome}",
                    titulo = "Seus documentos\nsob controle.",
                    acaoTitulo = when (emAndamento) {
                        0 -> "Tudo em dia"
                        1 -> "1 documento"
                        else -> "$emAndamento documentos"
                    },
                    acaoSubtitulo = when {
                        emAndamento == 0 -> "Toque para enviar um novo documento"
                        emAndamento == 1 -> "aguardando assinaturas · toque para acompanhar"
                        else -> "aguardando assinaturas"
                    },
                    acaoIcone = if (emAndamento == 0) Icons.AutoMirrored.Outlined.NoteAdd else Icons.Outlined.Draw,
                    onAcao = when {
                        emAndamento == 0 -> onNewDocument
                        emAndamento == 1 && unicoEmAndamento != null ->
                        { { onOpenDocument(unicoEmAndamento.document.id) } }
                        else -> { { viewModel.onFiltro(FiltroDocumento.ANDAMENTO) } }
                    }
                )
            }

            // ---------- Resumo ----------
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardResumoCompacto(
                        valor = state.totalRascunhos,
                        rotulo = "Rascunhos",
                        icone = Icons.Outlined.EditNote,
                        cor = corDoStatus(DocumentStatus.RASCUNHO),
                        selecionado = state.filtro == FiltroDocumento.RASCUNHO,
                        onClick = { viewModel.onFiltro(FiltroDocumento.RASCUNHO) },
                        modifier = Modifier.weight(1f)
                    )
                    CardResumoCompacto(
                        valor = state.totalAndamento,
                        rotulo = "Em andamento",
                        icone = Icons.Outlined.HourglassTop,
                        cor = corDoStatus(DocumentStatus.ENVIADO),
                        selecionado = state.filtro == FiltroDocumento.ANDAMENTO,
                        onClick = { viewModel.onFiltro(FiltroDocumento.ANDAMENTO) },
                        modifier = Modifier.weight(1f)
                    )
                    CardResumoCompacto(
                        valor = state.totalConcluidos,
                        rotulo = "Concluídos",
                        icone = Icons.Outlined.CheckCircle,
                        cor = corDoStatus(DocumentStatus.CONCLUIDO),
                        selecionado = state.filtro == FiltroDocumento.CONCLUIDO,
                        onClick = { viewModel.onFiltro(FiltroDocumento.CONCLUIDO) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ---------- Título da lista + filtros ----------
            item {
                Column {
                    Text("Documentos recentes", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Acompanhe o progresso de cada um",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FiltroDocumento.entries) { f ->
                            FilterChip(
                                selected = state.filtro == f,
                                onClick = { viewModel.onFiltro(f) },
                                label = { Text(f.label) }
                            )
                        }
                    }
                }
            }

            // ---------- Lista ----------
            when {
                state.carregando -> Unit

                state.totalGeral == 0 -> item {
                    EstadoVazio(
                        "Nenhum documento ainda",
                        "Toque em \"Novo documento\" para enviar o primeiro PDF para assinatura."
                    )
                }

                state.documentos.isEmpty() -> item {
                    EstadoVazio("Nada por aqui", "Nenhum documento em \"${state.filtro.label}\".")
                }

                else -> items(state.documentos, key = { it.document.id }) { item ->
                    CardDoDocumento(
                        item = item,
                        onClick = {
                            if (item.document.status == DocumentStatus.RASCUNHO) onContinueDraft(item.document.id)
                            else onOpenDocument(item.document.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardDoDocumento(item: DocumentListItem, onClick: () -> Unit) {
    val doc = item.document
    val rascunho = doc.status == DocumentStatus.RASCUNHO
    val concluidoEm = doc.concluidoEm
    val enviadoEm = doc.enviadoEm

    DocumentoResumoCard(
        titulo = doc.titulo,
        subtitulo = when {
            concluidoEm != null -> "Concluído ${DateFormatter.relativo(concluidoEm)}"
            enviadoEm != null -> "Enviado ${DateFormatter.relativo(enviadoEm)}"
            else -> "Criado ${DateFormatter.relativo(doc.criadoEm)}"
        },
        cor = corDoStatus(doc.status),
        rotuloStatus = if (rascunho) "Rascunho · toque para continuar" else rotuloDoStatus(doc.status),
        textoProgresso = when {
            item.totalSignatarios == 0 -> null
            rascunho -> "${item.totalSignatarios} signatário(s)"
            else -> "${item.assinados} de ${item.totalSignatarios} assinaturas"
        },
        progresso = if (!rascunho && item.totalSignatarios > 0)
            item.assinados.toFloat() / item.totalSignatarios else null,
        nomes = item.nomesSignatarios?.split("|")?.filter { it.isNotBlank() }.orEmpty(),
        onClick = onClick
    )
}

@Composable
private fun EstadoVazio(titulo: String, texto: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}