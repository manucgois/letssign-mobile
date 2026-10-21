@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.document

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.file.PdfInfo
import br.com.fiap.letssign.ui.components.StepIndicator
import java.util.Locale

@Composable
fun NewDocumentScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: NewDocumentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Abre o seletor de arquivos do Android, filtrando só PDFs
    val seletorPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> viewModel.onPdfSelecionado(uri) }

    LaunchedEffect(state.criadoId) {
        state.criadoId?.let { id ->
            Toast.makeText(context, "Rascunho criado!", Toast.LENGTH_SHORT).show()
            onCreated(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo documento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepIndicator(etapaAtual = 1)

            OutlinedTextField(
                value = state.titulo,
                onValueChange = viewModel::onTituloChange,
                label = { Text("Título do documento") },
                placeholder = { Text("Ex.: Contrato de prestação de serviços") },
                isError = state.tituloErro != null,
                supportingText = if (state.tituloErro != null) {
                    { Text(state.tituloErro ?: "") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Arquivo", style = MaterialTheme.typography.titleMedium)

            when {
                state.analisando -> CartaoAnalisando()
                state.pdf != null -> CartaoPdfSelecionado(
                    pdf = state.pdf!!,
                    onTrocar = { seletorPdf.launch(arrayOf("application/pdf")) }
                )
                else -> CartaoSelecionar(onClick = { seletorPdf.launch(arrayOf("application/pdf")) })
            }

            if (state.arquivoErro != null) {
                CartaoErro(state.arquivoErro ?: "")
            }

            Text(
                "Aceito: PDF de até 20 MB, sem senha.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.erroGeral != null) {
                Text(state.erroGeral ?: "", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::continuar,
                enabled = !state.salvando && !state.analisando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.salvando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continuar")
                }
            }
        }
    }
}

@Composable
private fun CartaoSelecionar(onClick: () -> Unit) {
    val formato = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outline, formato)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.UploadFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text("Selecione o PDF que será assinado", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onClick) { Text("Selecionar PDF do celular") }
    }
}

@Composable
private fun CartaoAnalisando() {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(16.dp))
            Text("Verificando o arquivo…")
        }
    }
}

@Composable
private fun CartaoPdfSelecionado(pdf: PdfInfo, onTrocar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Image(
                bitmap = pdf.capa.asImageBitmap(),
                contentDescription = "Primeira página do PDF",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(96.dp)
                    .height(132.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        pdf.nomeArquivo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "${pdf.paginas} página(s) · ${formatarTamanho(pdf.tamanhoBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "✓ Arquivo válido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onTrocar) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Trocar arquivo")
                }
            }
        }
    }
}

@Composable
private fun CartaoErro(mensagem: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(12.dp))
            Text(mensagem, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

private fun formatarTamanho(bytes: Long): String {
    val ptBr = Locale.forLanguageTag("pt-BR")
    return if (bytes < 1024 * 1024) String.format(ptBr, "%.0f KB", bytes / 1024.0)
    else String.format(ptBr, "%.1f MB", bytes / (1024.0 * 1024.0))
}