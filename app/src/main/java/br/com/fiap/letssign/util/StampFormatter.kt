package br.com.fiap.letssign.util

import br.com.fiap.letssign.domain.model.IdentificationType

/** Monta os textos do carimbo de assinatura. */
object StampFormatter {

    fun identificacao(tipo: IdentificationType, numero: String): String = when (tipo) {
        IdentificationType.CPF -> "CPF ${Validators.maskCpf(numero)}"
        IdentificationType.OAB -> {
            val limpo = numero.uppercase().replace(Regex("[^A-Z0-9]"), "")
            if (limpo.length > 2) "OAB/${limpo.take(2)} ${limpo.drop(2)}" else "OAB ${numero.trim()}"
        }
        else -> "${tipo.label} ${numero.trim()}"
    }

    /** Ex.: "Maria Silva · Advogada · OAB/MG 123456" */
    fun linhaPrincipal(nome: String, cargo: String?, tipo: IdentificationType, numero: String): String =
        listOf(
            nome.trim(),
            cargo?.trim().orEmpty(),
            if (numero.isBlank()) "" else identificacao(tipo, numero)
        ).filter { it.isNotBlank() }.joinToString(" · ")
}