package br.com.fiap.letssign.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import br.com.fiap.letssign.R

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

private val base = Typography()

/** Manrope em todos os estilos: ExtraBold/Bold nos títulos, Regular nos textos. */
val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
    displayMedium = base.displayMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
    displaySmall = base.displaySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Manrope),
    bodyMedium = base.bodyMedium.copy(fontFamily = Manrope),
    bodySmall = base.bodySmall.copy(fontFamily = Manrope),
    labelLarge = base.labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium)
)