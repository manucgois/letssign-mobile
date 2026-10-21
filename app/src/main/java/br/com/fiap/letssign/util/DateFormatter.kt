package br.com.fiap.letssign.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Converte os timestamps (Long) salvos no banco em texto para a tela. */
object DateFormatter {

    private val ptBr = Locale.forLanguageTag("pt-BR")
    private val dataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", ptBr)
    private val data = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBr)

    /** Ex.: 03/11/2026 às 14:32 */
    fun dataHora(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dataHora)

    /** Ex.: 03/11/2026 */
    fun data(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(data)
}