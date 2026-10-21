@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.fiap.letssign.ui.screens.signers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SignerFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignerFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.salvo) {
        if (state.salvo) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.modoEdicao) "Editar signatário" else "Novo signatário") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->

        when {
            state.carregando -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.naoEncontrado -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Signatário não encontrado", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onBack) { Text("Voltar") }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Campo(
                    valor = state.nome, onChange = viewModel::onNomeChange,
                    rotulo = "Nome completo", icone = Icons.Outlined.Badge, erro = state.nomeErro
                )
                Campo(
                    valor = state.email, onChange = viewModel::onEmailChange,
                    rotulo = "E-mail (usado no login)", icone = Icons.Outlined.Email,
                    erro = state.emailErro, teclado = KeyboardType.Email
                )
                Campo(
                    valor = state.telefone, onChange = viewModel::onTelefoneChange,
                    rotulo = "Telefone com DDD", icone = Icons.Outlined.Phone,
                    erro = state.telefoneErro, teclado = KeyboardType.Phone
                )
                Campo(
                    valor = state.empresa, onChange = viewModel::onEmpresaChange,
                    rotulo = "Empresa", icone = Icons.Outlined.Business, erro = state.empresaErro
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    if (state.modoEdicao) "Alterar senha (deixe em branco para manter a atual)"
                    else "Senha de acesso",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Mínimo de 8 caracteres, com letras e números. " +
                            "Informe a senha ao signatário por um canal seguro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CampoSenha(
                    valor = state.senha, onChange = viewModel::onSenhaChange,
                    rotulo = "Senha", erro = state.senhaErro,
                    visivel = state.senhaVisivel, onToggle = viewModel::onToggleSenha
                )
                CampoSenha(
                    valor = state.confirmarSenha, onChange = viewModel::onConfirmarChange,
                    rotulo = "Confirmar senha", erro = state.confirmarErro,
                    visivel = state.senhaVisivel, onToggle = viewModel::onToggleSenha
                )

                if (state.erroGeral != null) {
                    Text(state.erroGeral ?: "", color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::salvar,
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
                        Text(if (state.modoEdicao) "Salvar alterações" else "Cadastrar signatário")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Campo(
    valor: String,
    onChange: (String) -> Unit,
    rotulo: String,
    icone: ImageVector,
    erro: String?,
    teclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(rotulo) },
        leadingIcon = { Icon(icone, contentDescription = null) },
        isError = erro != null,
        supportingText = if (erro != null) {
            { Text(erro) }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CampoSenha(
    valor: String,
    onChange: (String) -> Unit,
    rotulo: String,
    erro: String?,
    visivel: Boolean,
    onToggle: () -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(rotulo) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    if (visivel) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visivel) "Ocultar senha" else "Mostrar senha"
                )
            }
        },
        visualTransformation = if (visivel) VisualTransformation.None else PasswordVisualTransformation(),
        isError = erro != null,
        supportingText = if (erro != null) {
            { Text(erro) }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}