package br.com.fiap.letssign.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val ETAPAS_DOCUMENTO = listOf("Documento", "Signatários", "Campos", "Revisão")

/** Barra de progresso do assistente. etapaAtual começa em 1. */
@Composable
fun StepIndicator(etapaAtual: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Etapa $etapaAtual de ${ETAPAS_DOCUMENTO.size} · ${ETAPAS_DOCUMENTO[etapaAtual - 1]}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ETAPAS_DOCUMENTO.forEachIndexed { index, _ ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            color = if (index < etapaAtual) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}