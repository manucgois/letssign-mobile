package br.com.fiap.letssign.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    onOpenSignature: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    HomePlaceholder(
        titulo = "Área do Administrador",
        nome = user?.nomeCompleto.orEmpty(),
        descricao = "Aqui ficarão o dashboard, os documentos e o cadastro de signatários.",
        onOpenSignature = onOpenSignature,
        onLogout = {
            viewModel.logout()
            onLogout()
        }
    )
}

@Composable
fun SignerHomeScreen(
    onLogout: () -> Unit,
    onOpenSignature: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    HomePlaceholder(
        titulo = "Área do Signatário",
        nome = user?.nomeCompleto.orEmpty(),
        descricao = "Aqui ficarão os documentos pendentes e os já assinados.",
        onOpenSignature = onOpenSignature,
        onLogout = {
            viewModel.logout()
            onLogout()
        }
    )
}

@Composable
private fun HomePlaceholder(
    titulo: String,
    nome: String,
    descricao: String,
    onOpenSignature: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Olá, $nome!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                descricao,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onOpenSignature) {
                Icon(Icons.Outlined.Draw, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Minha assinatura")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sair")
            }
        }
    }
}