@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.signature

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.domain.model.IdentificationType
import br.com.fiap.letssign.domain.model.SignatureStyle
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.ui.components.SignaturePad
import br.com.fiap.letssign.ui.components.SignaturePadState
import br.com.fiap.letssign.ui.components.SignaturePreview
import br.com.fiap.letssign.ui.components.rememberSignaturePadState
import br.com.fiap.letssign.ui.theme.Cursiva
import br.com.fiap.letssign.ui.theme.LightOutline
import br.com.fiap.letssign.ui.theme.Tinta
import br.com.fiap.letssign.util.StampFormatter

@Composable
fun SignatureSetupScreen(
    primeiroAcesso: Boolean,
    onFinish: (UserRole?) -> Unit,
    onBack: () -> Unit,
    viewModel: SignatureSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val assinaturaPad = rememberSignaturePadState()
    val rubricaPad = rememberSignaturePadState()

    LaunchedEffect(state.salvo) {
        if (state.salvo) onFinish(state.perfil)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (primeiroAcesso) "Criar minha assinatura" else "Minha assinatura") },
                navigationIcon = {
                    if (!primeiroAcesso) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        if (state.carregando) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (primeiroAcesso) {
                Text(
                    "Configure uma única vez e use em todos os documentos. " +
                            "A cada assinatura, o app pede a sua confirmação.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---------- Dados ----------
            SectionTitle("Seus dados")

            OutlinedTextField(
                value = state.nome,
                onValueChange = viewModel::onNomeChange,
                label = { Text("Nome completo") },
                isError = state.nomeErro != null,
                supportingText = if (state.nomeErro != null) {
                    { Text(state.nomeErro ?: "") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.cargo,
                onValueChange = viewModel::onCargoChange,
                label = { Text("Cargo (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            TipoIdentificacaoDropdown(
                selecionado = state.tipoIdentificacao,
                onSelect = viewModel::onTipoChange
            )

            OutlinedTextField(
                value = state.numero,
                onValueChange = viewModel::onNumeroChange,
                label = {
                    Text(
                        when (state.tipoIdentificacao) {
                            IdentificationType.CPF -> "Número do CPF"
                            IdentificationType.OAB -> "OAB (UF + número)"
                            else -> "Número"
                        }
                    )
                },
                isError = state.numeroErro != null,
                supportingText = if (state.numeroErro != null) {
                    { Text(state.numeroErro ?: "") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (state.tipoIdentificacao == IdentificationType.CPF)
                        KeyboardType.Number else KeyboardType.Text
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.empresa,
                onValueChange = viewModel::onEmpresaChange,
                label = { Text("Empresa") },
                isError = state.empresaErro != null,
                supportingText = {
                    Text(state.empresaErro ?: "Altere se você representa outra empresa.")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Estilo ----------
            SectionTitle("Como você quer assinar")

            val estilos = SignatureStyle.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                estilos.forEachIndexed { index, estilo ->
                    SegmentedButton(
                        selected = state.estilo == estilo,
                        onClick = { viewModel.onEstiloChange(estilo) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = estilos.size)
                    ) {
                        Text(
                            when (estilo) {
                                SignatureStyle.DESENHADA -> "Desenhar"
                                SignatureStyle.DIGITADA -> "Digitar"
                                SignatureStyle.RUBRICA -> "Rubrica"
                            }
                        )
                    }
                }
            }

            when (state.estilo) {
                SignatureStyle.DESENHADA ->
                    AreaDeDesenho(assinaturaPad, state.assinaturaPath, "Assine aqui com o dedo")
                SignatureStyle.RUBRICA ->
                    AreaDeDesenho(rubricaPad, state.rubricaPath, "Faça sua rubrica aqui")
                SignatureStyle.DIGITADA ->
                    OutlinedTextField(
                        value = state.textoDigitado,
                        onValueChange = viewModel::onTextoDigitadoChange,
                        label = { Text("Digite seu nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
            }

            if (state.visualErro != null) {
                Text(
                    state.visualErro ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ---------- Prévia ----------
            SectionTitle("Prévia do carimbo")
            CarimboPreview(
                state = state,
                padAtual = when (state.estilo) {
                    SignatureStyle.DESENHADA -> assinaturaPad
                    SignatureStyle.RUBRICA -> rubricaPad
                    SignatureStyle.DIGITADA -> null
                }
            )

            if (state.erroGeral != null) {
                Text(state.erroGeral ?: "", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val desenho = when (state.estilo) {
                        SignatureStyle.DESENHADA -> assinaturaPad.toBitmap()
                        SignatureStyle.RUBRICA -> rubricaPad.toBitmap()
                        SignatureStyle.DIGITADA -> null
                    }
                    viewModel.salvar(desenho)
                },
                enabled = !state.salvando,
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
                    Text("Salvar assinatura")
                }
            }

            if (primeiroAcesso) {
                TextButton(
                    onClick = { onFinish(state.perfil) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Pular por enquanto") }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun TipoIdentificacaoDropdown(
    selecionado: IdentificationType,
    onSelect: (IdentificationType) -> Unit
) {
    var aberto by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
        OutlinedTextField(
            value = selecionado.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo de identificação") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            IdentificationType.entries.forEach { tipo ->
                DropdownMenuItem(
                    text = { Text(tipo.label) },
                    onClick = {
                        onSelect(tipo)
                        aberto = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AreaDeDesenho(padState: SignaturePadState, caminhoSalvo: String?, dica: String) {
    val imagemSalva = remember(caminhoSalvo) {
        caminhoSalvo?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    val formato = RoundedCornerShape(16.dp)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        if (imagemSalva != null && padState.isEmpty) {
            Text("Atual (desenhe abaixo para substituir):", style = MaterialTheme.typography.labelMedium)
            Image(
                bitmap = imagemSalva,
                contentDescription = "Assinatura atual",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(formato)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, formato)
        ) {
            if (padState.isEmpty) {
                Text(
                    dica,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            // Linha de base, como numa folha de papel
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
            SignaturePad(state = padState, modifier = Modifier.fillMaxSize())
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { padState.undo() }, enabled = !padState.isEmpty) {
                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Desfazer")
            }
            OutlinedButton(onClick = { padState.clear() }, enabled = !padState.isEmpty) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Limpar")
            }
        }
    }
}

/** Prévia do carimbo: sempre em fundo branco, como ficará no PDF. */
@Composable
private fun CarimboPreview(state: SignatureSetupUiState, padAtual: SignaturePadState?) {
    val caminhoSalvo = when (state.estilo) {
        SignatureStyle.DESENHADA -> state.assinaturaPath
        SignatureStyle.RUBRICA -> state.rubricaPath
        SignatureStyle.DIGITADA -> null
    }
    val imagemSalva = remember(caminhoSalvo) {
        caminhoSalvo?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    val cinza = Color(0xFF4A5568)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LightOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                when {
                    state.estilo == SignatureStyle.DIGITADA ->
                        Text(
                            state.textoDigitado.ifBlank { "Seu nome" },
                            fontFamily = Cursiva,
                            fontSize = 34.sp,
                            color = Tinta,
                            maxLines = 1
                        )
                    padAtual != null && !padAtual.isEmpty ->
                        SignaturePreview(
                            state = padAtual,
                            color = Tinta,
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.7f)
                        )
                    imagemSalva != null ->
                        Image(imagemSalva, contentDescription = null, modifier = Modifier.fillMaxHeight())
                    else ->
                        Text("[sua assinatura]", color = cinza)
                }
            }
            HorizontalDivider(color = Tinta.copy(alpha = 0.3f))
            Spacer(Modifier.height(6.dp))
            Text(
                StampFormatter.linhaPrincipal(state.nome, state.cargo, state.tipoIdentificacao, state.numero),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Tinta
            )
            if (state.empresa.isNotBlank()) {
                Text(state.empresa, style = MaterialTheme.typography.bodySmall, color = Tinta)
            }
            Text(
                "Assinado em [data e hora] · cód. [gerado ao assinar]",
                style = MaterialTheme.typography.labelSmall,
                color = cinza
            )
        }
    }
}