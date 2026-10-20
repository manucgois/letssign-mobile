package br.com.fiap.letssign.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.ui.theme.RatificaTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import br.com.fiap.letssign.R
import br.com.fiap.letssign.ui.theme.AzulRatifica

/** Versão "com estado": conversa com o ViewModel. */
@Composable
fun LoginScreen(
    onLoginSuccess: (UserEntity) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Quando o login dá certo, navega uma única vez
    LaunchedEffect(state.usuarioLogado) {
        state.usuarioLogado?.let { user ->
            onLoginSuccess(user)
            viewModel.onNavigated()
        }
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSenhaChange = viewModel::onSenhaChange,
        onToggleSenha = viewModel::onToggleSenhaVisivel,
        onLoginClick = viewModel::login
    )
}

/** Versão "sem estado": só desenha a tela. Permite usar o Preview. */
@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onSenhaChange: (String) -> Unit,
    onToggleSenha: () -> Unit,
    onLoginClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(64.dp))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(AzulRatifica, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_ratifica_mark),
                    contentDescription = "Logo Ratifica",
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Ratifica",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Seu contrato ratificado na palma da mão.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                isError = state.emailErro != null,
                supportingText = if (state.emailErro != null) {
                    { Text(state.emailErro ?: "") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.senha,
                onValueChange = onSenhaChange,
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onToggleSenha) {
                        Icon(
                            imageVector = if (state.senhaVisivel) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = if (state.senhaVisivel) "Ocultar senha"
                            else "Mostrar senha"
                        )
                    }
                },
                visualTransformation = if (state.senhaVisivel) VisualTransformation.None
                else PasswordVisualTransformation(),
                isError = state.senhaErro != null,
                supportingText = if (state.senhaErro != null) {
                    { Text(state.senhaErro ?: "") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClick()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.erroGeral != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.erroGeral ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onLoginClick,
                enabled = !state.carregando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.carregando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Entrar")
                }
            }

            Spacer(Modifier.height(32.dp))

            // Dica de acesso para testes e para o vídeo (remover em produção)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Acesso de demonstração",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Admin: admin@letssign.com / Admin@123", style = MaterialTheme.typography.bodySmall)
                    Text("Signatária: maria@teste.com / Teste@123", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    RatificaTheme {
        LoginContent(
            state = LoginUiState(),
            onEmailChange = {},
            onSenhaChange = {},
            onToggleSenha = {},
            onLoginClick = {}
        )
    }
}