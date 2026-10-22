@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.document

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.ui.components.StepIndicator
import br.com.fiap.letssign.ui.components.corDoSignatario
import br.com.fiap.letssign.ui.theme.Tinta
import kotlin.math.roundToInt

@Composable
fun DocumentFieldsScreen(
    onBack: () -> Unit,
    onContinue: (Long) -> Unit,
    viewModel: DocumentFieldsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.avancar) {
        if (state.avancar) {
            viewModel.onAvancou()
            onContinue(state.documentId)
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
        },
        bottomBar = {
            if (state.bloqueio == null && !state.carregando) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    if (state.erro != null) {
                        Text(
                            state.erro ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = viewModel::continuar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) { Text("Continuar") }
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

            else -> Conteudo(state, viewModel, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun Conteudo(
    state: DocumentFieldsUiState,
    viewModel: DocumentFieldsViewModel,
    modifier: Modifier = Modifier
) {
    val nomeSelecionado = state.participantes
        .firstOrNull { it.signer.id == state.participanteSelecionadoId }
        ?.user?.nomeCompleto?.substringBefore(" ")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StepIndicator(etapaAtual = 3)

        Column {
            Text("Onde cada pessoa vai assinar?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (nomeSelecionado != null)
                        "Toque na página para posicionar o campo de $nomeSelecionado. Arraste para ajustar."
                    else "Escolha um signatário abaixo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------- Quem vai receber o próximo campo ----------
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.participantes, key = { _, p -> p.signer.id }) { indice, p ->
                val quantidade = state.campos.count { it.signerId == p.signer.id }
                FilterChip(
                    selected = state.participanteSelecionadoId == p.signer.id,
                    onClick = { viewModel.selecionarParticipante(p.signer.id) },
                    label = { Text("${p.user.nomeCompleto.substringBefore(" ")} · $quantidade") },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(corDoSignatario(indice), CircleShape)
                        )
                    }
                )
            }
        }

        // ---------- A página com os campos ----------
        PaginaComCampos(
            state = state,
            onTocar = viewModel::tocarNaPagina,
            onSelecionarCampo = viewModel::selecionarCampo,
            onMover = viewModel::moverCampo
        )

        // ---------- Troca de página ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = viewModel::paginaAnterior,
                enabled = state.paginaAtual > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Página anterior")
            }
            Text(
                "Página ${state.paginaAtual + 1} de ${state.totalPaginas}",
                style = MaterialTheme.typography.titleSmall
            )
            FilledTonalIconButton(
                onClick = viewModel::proximaPagina,
                enabled = state.paginaAtual < state.totalPaginas - 1
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próxima página")
            }
        }

        // ---------- Campo selecionado ----------
        val campoSelecionado = state.campos.firstOrNull { it.id == state.campoSelecionadoId }
        if (campoSelecionado != null) {
            val dono = state.participantes.firstOrNull { it.signer.id == campoSelecionado.signerId }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Campo de ${dono?.user?.nomeCompleto?.substringBefore(" ") ?: "signatário"}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Arraste para mover · página ${campoSelecionado.pagina + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = viewModel::excluirCampoSelecionado) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // ---------- Resumo ----------
        Text("Resumo", style = MaterialTheme.typography.titleMedium)
        state.participantes.forEachIndexed { indice, p ->
            val camposDele = state.campos.filter { it.signerId == p.signer.id }
            val paginas = camposDele.map { it.pagina + 1 }.distinct().sorted()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(corDoSignatario(indice), CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "${p.user.nomeCompleto} · ${p.signer.papel.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (camposDele.isEmpty()) "Nenhum campo ainda"
                        else "${plural(camposDele.size, "campo", "campos")} · " +
                                (if (paginas.size == 1) "página " else "páginas ") + paginas.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (camposDele.isEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PaginaComCampos(
    state: DocumentFieldsUiState,
    onTocar: (Float, Float) -> Unit,
    onSelecionarCampo: (Long) -> Unit,
    onMover: (SignatureFieldEntity, Float, Float) -> Unit
) {
    val bitmap = state.paginaBitmap
    // Proporção real da página (A4 em pé ≈ 1 : 1,414 enquanto carrega)
    val proporcao = bitmap?.let { it.width.toFloat() / it.height } ?: (1f / 1.414f)
    val imagem = remember(bitmap) { bitmap?.asImageBitmap() }
    var tamanho by remember { mutableStateOf(IntSize.Zero) }
    val onTocarAtual by rememberUpdatedState(onTocar)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(proporcao)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .onSizeChanged { tamanho = it }
            .pointerInput(state.paginaAtual, tamanho) {
                detectTapGestures { posicao ->
                    if (tamanho.width > 0 && tamanho.height > 0) {
                        onTocarAtual(posicao.x / tamanho.width, posicao.y / tamanho.height)
                    }
                }
            }
    ) {
        if (imagem != null) {
            Image(
                bitmap = imagem,
                contentDescription = "Página ${state.paginaAtual + 1} do documento",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        if (tamanho.width > 0) {
            state.campos
                .filter { it.pagina == state.paginaAtual }
                .forEach { campo ->
                    val indice = state.participantes.indexOfFirst { it.signer.id == campo.signerId }
                    val nome = state.participantes.getOrNull(indice)
                        ?.user?.nomeCompleto?.substringBefore(" ") ?: "?"
                    key(campo.id) {
                        CampoArrastavel(
                            campo = campo,
                            larguraPaginaPx = tamanho.width.toFloat(),
                            alturaPaginaPx = tamanho.height.toFloat(),
                            cor = corDoSignatario(indice),
                            rotulo = nome,
                            selecionado = state.campoSelecionadoId == campo.id,
                            onSelecionar = { onSelecionarCampo(campo.id) },
                            onSoltar = { x, y -> onMover(campo, x, y) }
                        )
                    }
                }
        }
    }
}

@Composable
private fun CampoArrastavel(
    campo: SignatureFieldEntity,
    larguraPaginaPx: Float,
    alturaPaginaPx: Float,
    cor: Color,
    rotulo: String,
    selecionado: Boolean,
    onSelecionar: () -> Unit,
    onSoltar: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val larguraPx = campo.largura * larguraPaginaPx
    val alturaPx = campo.altura * alturaPaginaPx

    // Posição em pixels durante o arraste; reinicia quando o banco devolve a posição salva
    var posicao by remember(campo.id, campo.x, campo.y, larguraPaginaPx, alturaPaginaPx) {
        mutableStateOf(Offset(campo.x * larguraPaginaPx, campo.y * alturaPaginaPx))
    }
    val onSelecionarAtual by rememberUpdatedState(onSelecionar)
    val onSoltarAtual by rememberUpdatedState(onSoltar)
    val formato = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .offset { IntOffset(posicao.x.roundToInt(), posicao.y.roundToInt()) }
            .size(
                width = with(density) { larguraPx.toDp() },
                height = with(density) { alturaPx.toDp() }
            )
            .background(cor.copy(alpha = if (selecionado) 0.30f else 0.18f), formato)
            .border(if (selecionado) 2.5.dp else 1.5.dp, cor, formato)
            .pointerInput(campo.id, larguraPaginaPx, alturaPaginaPx) {
                detectTapGestures { onSelecionarAtual() }
            }
            .pointerInput(campo.id, larguraPaginaPx, alturaPaginaPx) {
                detectDragGestures(
                    onDragStart = { onSelecionarAtual() },
                    onDrag = { change, arraste ->
                        change.consume()
                        // coerceIn = o campo nunca sai da página
                        posicao = Offset(
                            x = (posicao.x + arraste.x).coerceIn(0f, larguraPaginaPx - larguraPx),
                            y = (posicao.y + arraste.y).coerceIn(0f, alturaPaginaPx - alturaPx)
                        )
                    },
                    onDragEnd = {
                        // Converte de pixels para proporção da página (0 a 1) e salva
                        onSoltarAtual(posicao.x / larguraPaginaPx, posicao.y / alturaPaginaPx)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Draw,
                contentDescription = null,
                tint = Tinta,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                rotulo,
                style = MaterialTheme.typography.labelSmall,
                color = Tinta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun plural(n: Int, singular: String, plural: String): String =
    "$n ${if (n == 1) singular else plural}"