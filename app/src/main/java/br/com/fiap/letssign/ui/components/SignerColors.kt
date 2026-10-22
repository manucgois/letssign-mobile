package br.com.fiap.letssign.ui.components

import androidx.compose.ui.graphics.Color

/** Cada signatário ganha uma cor, usada nos campos, chips e resumos. */
private val CORES_SIGNATARIOS = listOf(
    Color(0xFF0E9F8E), // verde-validado
    Color(0xFF3B82F6), // azul
    Color(0xFFD97706), // âmbar
    Color(0xFF8B5CF6), // roxo
    Color(0xFFDB2777)  // rosa
)

fun corDoSignatario(indice: Int): Color =
    CORES_SIGNATARIOS[(if (indice < 0) 0 else indice) % CORES_SIGNATARIOS.size]