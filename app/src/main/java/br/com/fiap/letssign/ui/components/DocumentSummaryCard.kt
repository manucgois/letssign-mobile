package br.com.fiap.letssign.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.domain.model.DocumentStatus

/** Cor de cada situação: cinza = rascunho, âmbar = em andamento, verde = concluído, vermelho = recusado. */
fun corDoStatus(status: DocumentStatus): Color = when (status) {
    DocumentStatus.RASCUNHO, DocumentStatus.EXPIRADO -> Color(0xFF64748B)
    DocumentStatus.ENVIADO, DocumentStatus.PARCIALMENTE_ASSINADO -> Color(0xFFD97706)
    DocumentStatus.CONCLUIDO -> Color(0xFF0E9F8E)
    DocumentStatus.RECUSADO -> Color(0xFFDC2626)
    DocumentStatus.CANCELADO -> Color(0xFFC75450)
}

/** Texto curto e amigável de cada situação. */
fun rotuloDoStatus(status: DocumentStatus): String = when (status) {
    DocumentStatus.RASCUNHO -> "Rascunho"
    DocumentStatus.ENVIADO -> "Aguardando assinaturas"
    DocumentStatus.PARCIALMENTE_ASSINADO -> "Em andamento"
    DocumentStatus.CONCLUIDO -> "Concluído"
    DocumentStatus.RECUSADO -> "Recusado"
    DocumentStatus.EXPIRADO -> "Expirado"
    DocumentStatus.CANCELADO -> "Cancelado"
}

@Composable
fun BarraDeProgresso(progresso: Float, cor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progresso.coerceIn(0f, 1f))
                .background(cor, RoundedCornerShape(3.dp))
        )
    }
}

private fun iniciaisDe(nome: String): String =
    nome.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** Avatares sobrepostos: (MC)(JC)(AT) +2 */
@Composable
fun AvataresEmpilhados(nomes: List<String>, maximo: Int = 3) {
    val anel = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        nomes.take(maximo).forEachIndexed { i, nome ->
            Box(
                Modifier
                    .size(28.dp)
                    .border(2.dp, anel, CircleShape)
                    .padding(2.dp)
                    .background(corDoSignatario(i), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(iniciaisDe(nome), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        if (nomes.size > maximo) {
            Box(
                Modifier
                    .size(28.dp)
                    .border(2.dp, anel, CircleShape)
                    .padding(2.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+${nomes.size - maximo}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Card de documento: faixa colorida, ícone, título, situação, progresso e avatares. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentoResumoCard(
    titulo: String,
    subtitulo: String,
    cor: Color,
    rotuloStatus: String,
    textoProgresso: String?,
    progresso: Float?,
    nomes: List<String>,
    onClick: () -> Unit
) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Faixa colorida da situação
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(cor)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(cor.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            titulo,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(cor, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        rotuloStatus,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (textoProgresso != null) {
                        Text(
                            textoProgresso,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (progresso != null) BarraDeProgresso(progresso, cor)
                if (nomes.isNotEmpty()) AvataresEmpilhados(nomes)
            }
        }
    }
}

/** Card pequeno de resumo (número + rótulo), que também funciona como filtro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardResumoCompacto(
    valor: Int,
    rotulo: String,
    icone: ImageVector,
    cor: Color,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = if (selecionado) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(cor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
            }
            Text(valor.toString(), style = MaterialTheme.typography.titleLarge)
            Text(
                rotulo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}