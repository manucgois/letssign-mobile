package br.com.fiap.letssign.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.ui.theme.AzulRatifica

private enum class DialogoPerfil { SEGURANCA, SOBRE, SAIR }

@Composable
fun ProfileScreen(
    onOpenSignature: () -> Unit,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialogo by remember { mutableStateOf<DialogoPerfil?>(null) }
    val context = LocalContext.current
    val versao = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.0"
    }

    Scaffold(bottomBar = bottomBar) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Sua conta", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Perfil", style = MaterialTheme.typography.headlineMedium)

            // ---------- Cartão de identificação ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(AzulRatifica)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val iniciais = state.nome.trim().split(Regex("\\s+"))
                    .filter { it.isNotBlank() }.take(2)
                    .joinToString("") { it.first().uppercase() }
                Box(
                    Modifier
                        .size(80.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iniciais, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text(state.nome, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(state.email, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.height(10.dp))
                Text(
                    (if (state.perfil == UserRole.ADMIN) "Administrador" else "Signatário") + " · " + state.empresa,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // ---------- Números ----------
            if (state.estatisticas.isNotEmpty()) {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(vertical = 16.dp)) {
                        state.estatisticas.forEach { e ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    e.valor.toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    e.rotulo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ---------- Opções ----------
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    ItemMenu(
                        icone = Icons.Outlined.Draw,
                        titulo = "Minha assinatura",
                        subtitulo = if (state.temAssinatura) "Configurada · toque para editar" else "Ainda não criada",
                        onClick = onOpenSignature
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ItemMenu(
                        icone = Icons.Outlined.Shield,
                        titulo = "Segurança e privacidade",
                        subtitulo = "Como protegemos seus documentos",
                        onClick = { dialogo = DialogoPerfil.SEGURANCA }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ItemMenu(
                        icone = Icons.Outlined.Info,
                        titulo = "Sobre o Ratifica",
                        subtitulo = "Versão $versao",
                        onClick = { dialogo = DialogoPerfil.SOBRE }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ItemMenu(
                        icone = Icons.AutoMirrored.Outlined.Logout,
                        titulo = "Sair da conta",
                        subtitulo = null,
                        destrutivo = true,
                        onClick = { dialogo = DialogoPerfil.SAIR }
                    )
                }
            }

            Text(
                "Ratifica para Android · versão $versao",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    when (dialogo) {
        DialogoPerfil.SEGURANCA -> AlertDialog(
            onDismissRequest = { dialogo = null },
            icon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
            title = { Text("Segurança e privacidade") },
            text = {
                Text(
                    "• Senhas guardadas com criptografia (PBKDF2), nunca em texto.\n" +
                            "• Cada documento tem uma impressão digital (SHA-256) que revela qualquer alteração.\n" +
                            "• Toda assinatura exige confirmação de senha e aceite do conteúdo.\n" +
                            "• Histórico completo: criação, envio, visualização e assinatura.\n" +
                            "• Arquivos na área privada do app: cada pessoa só vê o que é dela ou foi enviado a ela."
                )
            },
            confirmButton = { TextButton(onClick = { dialogo = null }) { Text("Entendi") } }
        )

        DialogoPerfil.SOBRE -> AlertDialog(
            onDismissRequest = { dialogo = null },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text("Ratifica") },
            text = {
                Text(
                    "Seu contrato ratificado na palma da mão.\n\n" +
                            "Versão $versao\n" +
                            "Solução desenvolvida para o Challenge FIAP + LetsSign (Sprint 1)."
                )
            },
            confirmButton = { TextButton(onClick = { dialogo = null }) { Text("Fechar") } }
        )

        DialogoPerfil.SAIR -> AlertDialog(
            onDismissRequest = { dialogo = null },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
            title = { Text("Sair da conta?") },
            text = { Text("Você vai precisar entrar com e-mail e senha novamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        dialogo = null
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sair") }
            },
            dismissButton = { TextButton(onClick = { dialogo = null }) { Text("Cancelar") } }
        )

        null -> Unit
    }
}

@Composable
private fun ItemMenu(
    icone: ImageVector,
    titulo: String,
    subtitulo: String?,
    destrutivo: Boolean = false,
    onClick: () -> Unit
) {
    val cor = if (destrutivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(cor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (destrutivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!destrutivo) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}