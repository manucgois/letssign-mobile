package br.com.fiap.letssign.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.R
import br.com.fiap.letssign.ui.theme.AzulRatifica

/** Cabeçalho azul de boas-vindas, com um atalho opcional para a próxima ação. */
@Composable
fun HeroHeader(
    saudacao: String,
    titulo: String,
    acaoTitulo: String?,
    acaoSubtitulo: String?,
    acaoIcone: ImageVector,
    onAcao: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AzulRatifica)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.ic_ratifica_mark), contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("Ratifica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }

        Column {
            Text(saudacao, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            Text(titulo, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }

        if (acaoTitulo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .then(if (onAcao != null) Modifier.clickable(onClick = onAcao) else Modifier)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(acaoIcone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(acaoTitulo, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    if (acaoSubtitulo != null) {
                        Text(acaoSubtitulo, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
                if (onAcao != null) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}