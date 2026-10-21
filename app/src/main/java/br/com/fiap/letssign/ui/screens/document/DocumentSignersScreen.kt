@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.document

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.local.entity.SignerWithUser
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.model.SignerRole
import br.com.fiap.letssign.ui.components.InitialsAvatar
import br.com.fiap.letssign.ui.components.StepIndicator

@Composable
fun DocumentSignersScreen(
    onBack: () -> Unit,
    onContinue: (Long) -> Unit,
    onCadastrarNovo: () -> Unit,
    viewModel: DocumentSignersViewModel = hiltViewModel()
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

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { StepIndicator(etapaAtual = 2) }

                item {
                    Column {
                        Text("Quem vai assinar?", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.titulo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (state.participantes.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                "Nenhum signatário adicionado. Adicione pelo menos uma pessoa " +
                                        "e defina o papel dela no documento.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(state.participantes, key = { it.signer.id }) { p ->
                    ParticipanteCard(
                        participante = p,
                        ehVoce = p.user.id == state.adminId,
                        onPapel = { papel -> viewModel.alterarPapel(p, papel) },
                        onObrigatorio = { valor -> viewModel.alterarObrigatorio(p, valor) },
                        onSubstituir = { viewModel.abrirSubstituir(p) },
                        onRemover = { viewModel.remover(p) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = viewModel::abrirAdicionar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar signatário")
                    }
                }
            }
        }
    }

    // Lista de seleção (abre de baixo para cima)
    state.folha?.let { folha ->
        SelecaoPessoaSheet(
            titulo = when (folha) {
                is FolhaSelecao.Adicionar -> "Adicionar signatário"
                is FolhaSelecao.Substituir -> "Substituir ${folha.nomeAtual}"
            },
            pessoas = state.disponiveis,
            adminId = state.adminId,
            onSelecionar = viewModel::selecionarPessoa,
            onCadastrarNovo = {
                viewModel.fecharFolha()
                onCadastrarNovo()
            },
            onFechar = viewModel::fecharFolha
        )
    }
}

@Composable
private fun ParticipanteCard(
    participante: SignerWithUser,
    ehVoce: Boolean,
    onPapel: (SignerRole) -> Unit,
    onObrigatorio: (Boolean) -> Unit,
    onSubstituir: () -> Unit,
    onRemover: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(participante.user.nomeCompleto)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        participante.user.nomeCompleto + if (ehVoce) " (você)" else "",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        participante.user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text("Papel no documento", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SignerRole.entries.forEach { papel ->
                    val selecionado = participante.signer.papel == papel
                    FilterChip(
                        selected = selecionado,
                        onClick = { onPapel(papel) },
                        label = { Text(papel.label) },
                        leadingIcon = if (selecionado) {
                            { Icon(Icons.Outlined.Check, contentDescription = null) }
                        } else null
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Assinatura obrigatória", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "O documento só é concluído quando todos os obrigatórios assinarem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = participante.signer.obrigatorio,
                    onCheckedChange = onObrigatorio
                )
            }

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onSubstituir) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Substituir")
                }
                TextButton(onClick = onRemover) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Remover", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SelecaoPessoaSheet(
    titulo: String,
    pessoas: List<UserEntity>,
    adminId: Long,
    onSelecionar: (UserEntity) -> Unit,
    onCadastrarNovo: () -> Unit,
    onFechar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var busca by remember { mutableStateOf("") }
    val filtradas = pessoas.filter {
        busca.isBlank() ||
                it.nomeCompleto.contains(busca, ignoreCase = true) ||
                it.email.contains(busca, ignoreCase = true)
    }

    ModalBottomSheet(onDismissRequest = onFechar, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                placeholder = { Text("Buscar por nome ou e-mail") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            if (filtradas.isEmpty()) {
                Text(
                    if (pessoas.isEmpty()) "Todas as pessoas cadastradas já estão no documento."
                    else "Ninguém encontrado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            LazyColumn(Modifier.fillMaxWidth()) {
                items(filtradas, key = { it.id }) { pessoa ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelecionar(pessoa) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InitialsAvatar(pessoa.nomeCompleto, tamanho = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                pessoa.nomeCompleto + if (pessoa.id == adminId) " (você)" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                pessoa.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    TextButton(onClick = onCadastrarNovo, modifier = Modifier.padding(vertical = 8.dp)) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cadastrar novo signatário")
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}