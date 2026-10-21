package br.com.fiap.letssign.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.domain.model.DocumentStatus

private val AmbarFundo = Color(0xFFFFF1D6)
private val AmbarTexto = Color(0xFF5C3B00)

/** Etiqueta colorida com a situação do documento. */
@Composable
fun StatusBadge(status: DocumentStatus, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val (fundo, texto) = when (status) {
        DocumentStatus.RASCUNHO -> cores.surfaceVariant to cores.onSurfaceVariant
        DocumentStatus.ENVIADO -> cores.primaryContainer to cores.onPrimaryContainer
        DocumentStatus.PARCIALMENTE_ASSINADO -> AmbarFundo to AmbarTexto
        DocumentStatus.CONCLUIDO -> cores.secondaryContainer to cores.onSecondaryContainer
        DocumentStatus.RECUSADO -> cores.errorContainer to cores.onErrorContainer
        DocumentStatus.EXPIRADO -> cores.surfaceVariant to cores.onSurfaceVariant
    }
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = texto,
        modifier = modifier
            .background(fundo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}