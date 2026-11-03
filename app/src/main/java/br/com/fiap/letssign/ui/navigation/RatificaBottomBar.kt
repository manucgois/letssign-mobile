package br.com.fiap.letssign.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Um item da barra inferior: para onde vai, o texto e o ícone. */
data class ItemNavegacao(val rota: String, val rotulo: String, val icone: ImageVector)

val ABAS_ADMIN = listOf(
    ItemNavegacao(Routes.ADMIN_HOME, "Início", Icons.Outlined.Home),
    ItemNavegacao(Routes.SIGNERS, "Signatários", Icons.Outlined.Group),
    ItemNavegacao(Routes.ADMIN_PROFILE, "Perfil", Icons.Outlined.Person)
)

val ABAS_SIGNATARIO = listOf(
    ItemNavegacao(Routes.SIGNER_HOME, "Documentos", Icons.Outlined.Description),
    ItemNavegacao(Routes.SIGNER_PROFILE, "Perfil", Icons.Outlined.Person)
)

@Composable
fun RatificaBottomBar(
    itens: List<ItemNavegacao>,
    rotaAtual: String,
    onSelecionar: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        itens.forEach { item ->
            NavigationBarItem(
                selected = item.rota == rotaAtual,
                onClick = { if (item.rota != rotaAtual) onSelecionar(item.rota) },
                icon = { Icon(item.icone, contentDescription = null) },
                label = { Text(item.rotulo) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}