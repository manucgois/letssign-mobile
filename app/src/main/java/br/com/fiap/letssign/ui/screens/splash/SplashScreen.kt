package br.com.fiap.letssign.ui.screens.splash

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import br.com.fiap.letssign.R
import br.com.fiap.letssign.ui.theme.AzulRatifica
import br.com.fiap.letssign.ui.theme.VerdeValidadoClaro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DURACAO_MS = 1800

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val opacidade = remember { Animatable(0f) }
    val escala = remember { Animatable(0.85f) }
    val progresso = remember { Animatable(0f) }

    // Ícones da barra de status em BRANCO enquanto a splash estiver na tela
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val anterior = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (anterior != null) {
                controller.isAppearanceLightStatusBars = anterior
                controller.isAppearanceLightNavigationBars = anterior
            }
        }
    }

    LaunchedEffect(Unit) {
        // As três animações começam juntas
        launch { opacidade.animateTo(1f, tween(600)) }
        launch { escala.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { progresso.animateTo(1f, tween(DURACAO_MS - 200, easing = FastOutSlowInEasing)) }
        delay(DURACAO_MS.toLong())
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AzulRatifica)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = opacidade.value
                    scaleX = escala.value
                    scaleY = escala.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_ratifica_mark),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Ratifica",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Seu contrato ratificado na palma da mão.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }

        // Barrinha de carregamento
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .width(72.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progresso.value)
                    .background(VerdeValidadoClaro)
            )
        }
    }
}