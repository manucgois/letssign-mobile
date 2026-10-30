@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.signer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.local.entity.SignerDocumentItem
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.ui.components.StatusBadge
import br.com.fiap.letssign.util.DateFormatter

@Composable
fun SignerDashboardScreen(
    onOpenDocument: (Long) -> Unit,
    onOpenSignature: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SignerDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lista = if (state.aba == AbaSignatario.PENDENTES) state.pendentes else state.assinados

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ratifica", fontWeight = FontWeight.ExtraBold) },
                actions = {
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text("Olá, ${state.primeiroNome}!", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        when (state.pendentes.size) {
                            0 -> "Nenhum documento aguardando sua assinatura."
                            1 -> "Você tem 1 documento aguardando sua assinatura."
                            else -> "Você tem ${state.pendentes.size} documentos aguardando sua assinatura."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Aviso: ainda não criou a assinatura
            if (!state.temAssinatura) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Crie sua assinatura eletrônica", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Configure uma única vez e use em todos os documentos.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(onClick = onOpenSignature) { Text("Criar agora") }
                        }
                    }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.aba == AbaSignatario.PENDENTES,
                        onClick = { viewModel.onAba(AbaSignatario.PENDENTES) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Pendentes (${state.pendentes.size})") }
                    SegmentedButton(
                        selected = state.aba == AbaSignatario.ASSINADOS,
                        onClick = { viewModel.onAba(AbaSignatario.ASSINADOS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Assinados (${state.assinados.size})") }
                }
            }

            if (!state.carregando && lista.isEmpty()) {
                item {
                    EstadoVazio(
                        if (state.aba == AbaSignatario.PENDENTES) "Tudo em dia!"
                        else "Nenhum documento assinado ainda",
                        if (state.aba == AbaSignatario.PENDENTES)
                            "Quando alguém enviar um documento para você, ele aparece aqui."
                        else "Os documentos que você assinar ficam guardados aqui."
                    )
                }
            }

            items(lista, key = { it.document.id }) { item ->
                DocumentoSignatarioCard(item, onClick = { onOpenDocument(item.document.id) })
            }
        }
    }
}

private val AmbarFundo = Color(0xFFFFF1D6)
private val AmbarTexto = Color(0xFF5C3B00)

@Composable
private fun DocumentoSignatarioCard(item: SignerDocumentItem, onClick: () -> Unit) {
    val doc = item.document
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
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
            }
            val enviadoEm = doc.enviadoEm
            Text(
                "Enviado por ${item.remetente}" +
                        (if (enviadoEm != null) " · ${DateFormatter.data(enviadoEm)}" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val assinadoEm = item.meuAssinadoEm
                if (item.meuStatus == SignerStatus.ASSINADO && assinadoEm != null) {
                    Text(
                        "Você assinou em ${DateFormatter.data(assinadoEm)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(doc.status)
                } else {
                    Text(
                        "Aguardando sua assinatura",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AmbarTexto,
                        modifier = Modifier
                            .background(AmbarFundo, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoVazio(titulo: String, texto: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.TaskAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}