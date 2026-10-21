@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.local.entity.DocumentListItem
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.ui.components.StatusBadge
import br.com.fiap.letssign.util.DateFormatter

@Composable
fun AdminDashboardScreen(
    onNewDocument: () -> Unit,
    onOpenSigners: () -> Unit,
    onOpenSignature: () -> Unit,
    onContinueDraft: (Long) -> Unit,
    onOpenDocument: (Long) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ratifica", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = onOpenSigners) {
                        Icon(Icons.Outlined.Group, contentDescription = "Signatários")
                    }
                    IconButton(onClick = onOpenSignature) {
                        Icon(Icons.Outlined.Draw, contentDescription = "Minha assinatura")
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sair")
                    }
                }
            )
        },
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(
                        "Olá, ${state.primeiroNome}!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Acompanhe o ciclo de vida dos seus documentos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------- Cards de resumo (2 x 2) ----------
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CardResumo(
                            titulo = "Rascunhos",
                            quantidade = state.contagem[DocumentStatus.RASCUNHO] ?: 0,
                            icone = Icons.Outlined.EditNote,
                            selecionado = state.filtro == FiltroDocumento.RASCUNHO,
                            onClick = { viewModel.onFiltro(FiltroDocumento.RASCUNHO) },
                            modifier = Modifier.weight(1f)
                        )
                        CardResumo(
                            titulo = "Aguardando",
                            quantidade = state.contagem[DocumentStatus.ENVIADO] ?: 0,
                            icone = Icons.Outlined.HourglassTop,
                            selecionado = state.filtro == FiltroDocumento.AGUARDANDO,
                            onClick = { viewModel.onFiltro(FiltroDocumento.AGUARDANDO) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CardResumo(
                            titulo = "Parciais",
                            quantidade = state.contagem[DocumentStatus.PARCIALMENTE_ASSINADO] ?: 0,
                            icone = Icons.Outlined.Pending,
                            selecionado = state.filtro == FiltroDocumento.PARCIAL,
                            onClick = { viewModel.onFiltro(FiltroDocumento.PARCIAL) },
                            modifier = Modifier.weight(1f)
                        )
                        CardResumo(
                            titulo = "Concluídos",
                            quantidade = state.contagem[DocumentStatus.CONCLUIDO] ?: 0,
                            icone = Icons.Outlined.CheckCircle,
                            selecionado = state.filtro == FiltroDocumento.CONCLUIDO,
                            onClick = { viewModel.onFiltro(FiltroDocumento.CONCLUIDO) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ---------- Filtros ----------
            item {
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

            // ---------- Lista ----------
            when {
                state.carregando -> Unit

                state.totalGeral == 0 -> item {
                    EstadoVazio(
                        titulo = "Nenhum documento ainda",
                        texto = "Toque em \"Novo documento\" para enviar o primeiro PDF para assinatura."
                    )
                }

                state.documentos.isEmpty() -> item {
                    EstadoVazio(
                        titulo = "Nada por aqui",
                        texto = "Nenhum documento com o filtro \"${state.filtro.label}\"."
                    )
                }

                else -> items(state.documentos, key = { it.document.id }) { item ->
                    DocumentoCard(
                        item = item,
                        onClick = {
                            if (item.document.status == DocumentStatus.RASCUNHO) {
                                onContinueDraft(item.document.id)
                            } else {
                                onOpenDocument(item.document.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardResumo(
    titulo: String,
    quantidade: Int,
    icone: ImageVector,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        border = if (selecionado) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selecionado) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                quantidade.toString(),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                titulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DocumentoCard(item: DocumentListItem, onClick: () -> Unit) {
    val doc = item.document
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    doc.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(doc.status)
            }

            val progresso = when {
                item.totalSignatarios == 0 -> "Sem signatários"
                doc.status == DocumentStatus.RASCUNHO ->
                    plural(item.totalSignatarios, "signatário", "signatários")
                else ->
                    "${item.assinados} de ${plural(item.totalSignatarios, "assinatura", "assinaturas")}"
            }
            Text(
                "${plural(doc.quantidadePaginas, "página", "páginas")} · $progresso",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val concluidoEm = doc.concluidoEm
            val enviadoEm = doc.enviadoEm
            val dataTexto = when {
                concluidoEm != null -> "Concluído em ${DateFormatter.dataHora(concluidoEm)}"
                enviadoEm != null -> "Enviado em ${DateFormatter.dataHora(enviadoEm)}"
                else -> "Criado em ${DateFormatter.dataHora(doc.criadoEm)}"
            }
            Text(
                dataTexto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (doc.status == DocumentStatus.RASCUNHO) {
                Text(
                    "Toque para continuar o cadastro",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
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

/** Ex.: plural(1, "página", "páginas") = "1 página"; plural(3, ...) = "3 páginas" */
private fun plural(n: Int, singular: String, plural: String): String =
    "$n ${if (n == 1) singular else plural}"