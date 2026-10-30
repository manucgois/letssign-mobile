package br.com.fiap.letssign.domain.model

/**
 * Resultado de um envio para assinatura.
 * jaEnviadoAntes = true quando a mesma solicitação já tinha sido concluída
 * (ex.: clique duplo ou nova tentativa após um envio que deu certo).
 */
data class EnvioResultado(
    val jaEnviadoAntes: Boolean,
    val totalSignatarios: Int
)