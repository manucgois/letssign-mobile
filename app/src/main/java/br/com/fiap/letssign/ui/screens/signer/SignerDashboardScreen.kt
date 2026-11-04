@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.signer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.local.entity.SignerDocumentItem
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.ui.components.DocumentoResumoCard
import br.com.fiap.letssign.ui.components.HeroHeader
import br.com.fiap.letssign.ui.components.corDoStatus
import br.com.fiap.letssign.ui.components.rotuloDoStatus
import br.com.fiap.letssign.util.DateFormatter

@Composable
fun SignerDashboardScreen(
    onOpenDocument: (Long) -> Unit,
    onOpenSignature: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: SignerDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lista = if (state.aba == AbaSignatario.PENDENTES) state.pendentes else state.assinados
    val pendentes = state.pendentes.size

    Scaffold(bottomBar = bottomBar) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeroHeader(
                    saudacao = "Olá, ${state.primeiroNome}",
                    titulo = "Seus documentos\npara assinar.",
                    acaoTitulo = when (pendentes) {
                        0 -> "Tudo em dia"
                        1 -> "1 documento"
                        else -> "$pendentes documentos"
                    },
                    acaoSubtitulo = when (pendentes) {
                        0 -> "Nenhuma assinatura pendente"
                        1 -> "Toque para abrir e assinar"
                        else -> "aguardando a sua assinatura"
                    },
                    acaoIcone = if (pendentes == 0) Icons.Outlined.TaskAlt else Icons.Outlined.Draw,
                    onAcao = when (pendentes) {
                        0 -> null
                        1 -> { { onOpenDocument(state.pendentes.first().document.id) } }
                        else -> { { viewModel.onAba(AbaSignatario.PENDENTES) } }
                    }
                )
            }

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
                        if (state.aba == AbaSignatario.PENDENTES) "Tudo em dia!" else "Nenhum documento assinado ainda",
                        if (state.aba == AbaSignatario.PENDENTES)
                            "Quando alguém enviar um documento para você, ele aparece aqui."
                        else "Os documentos que você assinar ficam guardados aqui."
                    )
                }
            }

            items(lista, key = { it.document.id }) { item ->
                CardDoSignatario(item, onClick = { onOpenDocument(item.document.id) })
            }
        }
    }
}

@Composable
private fun CardDoSignatario(item: SignerDocumentItem, onClick: () -> Unit) {
    val doc = item.document
    val assinei = item.meuStatus == SignerStatus.ASSINADO
    val meuAssinadoEm = item.meuAssinadoEm
    val enviadoEm = doc.enviadoEm

    DocumentoResumoCard(
        titulo = doc.titulo,
        subtitulo = "De ${item.remetente}" + (enviadoEm?.let { " · ${DateFormatter.relativo(it)}" } ?: ""),
        // Pendente para mim = âmbar; já assinei = cor da situação geral do documento
        cor = if (assinei) corDoStatus(doc.status) else corDoStatus(DocumentStatus.ENVIADO),
        rotuloStatus = if (assinei) "Você assinou · ${rotuloDoStatus(doc.status)}"
        else "Aguardando sua assinatura",
        textoProgresso = if (assinei && meuAssinadoEm != null) DateFormatter.relativo(meuAssinadoEm) else null,
        progresso = null,
        nomes = emptyList(),
        onClick = onClick
    )
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