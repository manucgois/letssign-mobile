package br.com.fiap.letssign.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Guarda os traços desenhados e sabe transformá-los em imagem (PNG). */
class SignaturePadState {
    val strokes = mutableStateListOf<List<Offset>>()
    var size: IntSize = IntSize.Zero

    val isEmpty: Boolean get() = strokes.isEmpty()

    fun clear() = strokes.clear()

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
    }

    /**
     * Gera a imagem com fundo transparente e traço escuro,
     * pronta para ser "carimbada" no PDF (que tem fundo branco).
     */
    fun toBitmap(strokeWidthPx: Float = 8f): Bitmap? {
        if (isEmpty || size.width == 0 || size.height == 0) return null
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.rgb(11, 23, 38) // Tinta #0B1726
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        strokes.forEach { pontos ->
            if (pontos.size > 1) {
                val path = android.graphics.Path().apply {
                    moveTo(pontos.first().x, pontos.first().y)
                    pontos.drop(1).forEach { lineTo(it.x, it.y) }
                }
                canvas.drawPath(path, paint)
            }
        }
        return bitmap
    }
}

@Composable
fun rememberSignaturePadState(): SignaturePadState = remember { SignaturePadState() }

@Composable
fun SignaturePad(
    state: SignaturePadState,
    modifier: Modifier = Modifier,
    strokeColor: Color = MaterialTheme.colorScheme.onSurface
) {
    // Traço que está sendo desenhado agora (antes de soltar o dedo)
    var tracoAtual by remember { mutableStateOf(listOf<Offset>()) }

    Canvas(
        modifier = modifier
            .onSizeChanged { state.size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { inicio -> tracoAtual = listOf(inicio) },
                    onDrag = { change, _ ->
                        change.consume()
                        tracoAtual = tracoAtual + change.position
                    },
                    onDragEnd = {
                        if (tracoAtual.size > 1) state.strokes.add(tracoAtual)
                        tracoAtual = emptyList()
                    },
                    onDragCancel = { tracoAtual = emptyList() }
                )
            }
    ) {
        val estilo = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        (state.strokes + listOf(tracoAtual)).forEach { pontos ->
            if (pontos.size > 1) {
                val path = Path().apply {
                    moveTo(pontos.first().x, pontos.first().y)
                    pontos.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path = path, color = strokeColor, style = estilo)
            }
        }
    }
}

/** Mostra, em miniatura, o que foi desenhado no quadro (usado na prévia do carimbo). */
@Composable
fun SignaturePreview(
    state: SignaturePadState,
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier) {
        val origem = state.size
        if (origem.width == 0 || origem.height == 0) return@Canvas
        val escala = minOf(size.width / origem.width, size.height / origem.height)
        val estilo = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        state.strokes.forEach { pontos ->
            if (pontos.size > 1) {
                val path = Path().apply {
                    moveTo(pontos.first().x * escala, pontos.first().y * escala)
                    pontos.drop(1).forEach { lineTo(it.x * escala, it.y * escala) }
                }
                drawPath(path = path, color = color, style = estilo)
            }
        }
    }
}