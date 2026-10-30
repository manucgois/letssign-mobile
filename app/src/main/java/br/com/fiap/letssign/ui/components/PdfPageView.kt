package br.com.fiap.letssign.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.ui.theme.Tinta

/** Um campo pronto para desenhar: posição e tamanho de 0 a 1, cor e texto. */
data class CampoVisual(
    val id: Long,
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float,
    val cor: Color,
    val rotulo: String
)

/**
 * Página do PDF com os campos por cima.
 * onCampoClick = null → só visualização; com valor → os campos ficam tocáveis.
 */
@Composable
fun PdfPageView(
    bitmap: Bitmap?,
    campos: List<CampoVisual>,
    modifier: Modifier = Modifier,
    onCampoClick: ((Long) -> Unit)? = null
) {
    val proporcao = bitmap?.let { it.width.toFloat() / it.height } ?: (1f / 1.414f)
    val imagem = remember(bitmap) { bitmap?.asImageBitmap() }
    val formatoCampo = RoundedCornerShape(8.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(proporcao)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        if (imagem != null) {
            Image(
                bitmap = imagem,
                contentDescription = "Página do documento",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        // Proporção (0 a 1) × tamanho da página NESTA tela = posição real
        campos.forEach { campo ->
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * campo.x, y = maxHeight * campo.y)
                    .size(width = maxWidth * campo.largura, height = maxHeight * campo.altura)
                    .background(campo.cor.copy(alpha = 0.18f), formatoCampo)
                    .border(1.5.dp, campo.cor, formatoCampo)
                    .then(
                        if (onCampoClick != null) Modifier.clickable { onCampoClick(campo.id) }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    campo.rotulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Tinta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}