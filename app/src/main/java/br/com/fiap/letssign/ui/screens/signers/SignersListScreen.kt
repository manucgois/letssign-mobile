@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.signers

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.ui.theme.RatificaTheme

@Composable
fun SignersListScreen(
    onBack: () -> Unit,
    onNovo: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: SignersListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busca by viewModel.busca.collectAsStateWithLifecycle()

    SignersListContent(
        state = state,
        busca = busca,
        onBuscaChange = viewModel::onBuscaChange,
        onBack = onBack,
        onNovo = onNovo,
        onEditar = onEditar
    )
}

@Composable
fun SignersListContent(
    state: SignersListUiState,
    busca: String,
    onBuscaChange: (String) -> Unit,
    onBack: () -> Unit,
    onNovo: () -> Unit,
    onEditar: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signatários") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovo,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Novo signatário") }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = busca,
                onValueChange = onBuscaChange,
                placeholder = { Text("Buscar por nome ou e-mail") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                state.carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.totalCadastrados == 0 -> EstadoVazio(
                    titulo = "Nenhum signatário cadastrado",
                    texto = "Cadastre as pessoas que vão assinar os seus documentos."
                )

                state.signatarios.isEmpty() -> EstadoVazio(
                    titulo = "Nenhum resultado",
                    texto = "Ninguém encontrado para \"$busca\"."
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "${state.signatarios.size} de ${state.totalCadastrados} signatário(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.signatarios, key = { it.id }) { signatario ->
                        SignatarioCard(signatario, onClick = { onEditar(signatario.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatarioCard(signatario: UserEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(signatario.nomeCompleto)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    signatario.nomeCompleto,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    signatario.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${signatario.telefone} · ${signatario.empresa}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Editar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Círculo com as iniciais do nome, ex.: "Maria Contratante" → "MC". */
@Composable
private fun Avatar(nome: String) {
    val iniciais = nome.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            iniciais,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun EstadoVazio(titulo: String, texto: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.GroupAdd,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
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

@Preview(showBackground = true)
@Composable
private fun SignersListPreview() {
    val exemplo = listOf(
        UserEntity(
            id = 2, nomeCompleto = "Maria Contratante", email = "maria@teste.com",
            telefone = "(31) 99999-0001", senhaHash = "", senhaSalt = "",
            perfil = UserRole.SIGNATARIO, empresa = "Empresa Demo Ltda."
        ),
        UserEntity(
            id = 3, nomeCompleto = "João Contratado", email = "joao@teste.com",
            telefone = "(31) 99999-0002", senhaHash = "", senhaSalt = "",
            perfil = UserRole.SIGNATARIO, empresa = "Empresa Demo Ltda."
        )
    )
    RatificaTheme {
        SignersListContent(
            state = SignersListUiState(carregando = false, signatarios = exemplo, totalCadastrados = 2),
            busca = "",
            onBuscaChange = {},
            onBack = {},
            onNovo = {},
            onEditar = {}
        )
    }
}