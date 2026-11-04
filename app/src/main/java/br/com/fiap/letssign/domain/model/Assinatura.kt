package br.com.fiap.letssign.domain.model

import org.json.JSONObject

/**
 * Tudo o que aparece no carimbo, CONGELADO no momento da assinatura.
 * Se a pessoa mudar o cargo ou a assinatura depois, os documentos já assinados não mudam.
 */
data class CarimboSnapshot(
    val nome: String,
    val cargo: String?,
    val identificacao: String,
    val empresa: String,
    val papel: String,
    val email: String,
    val estilo: SignatureStyle,
    val imagemPath: String?,
    val textoDigitado: String?,
    val assinadoEm: Long,
    val dispositivo: String,
    val codigo: String,
    val rubricaPath: String? = null,
    val iniciais: String = ""
) {
    /** Converte para texto (JSON) para salvar na coluna carimboSnapshot. */
    fun toJson(): String = JSONObject().apply {
        put("nome", nome)
        put("cargo", cargo ?: "")
        put("identificacao", identificacao)
        put("empresa", empresa)
        put("papel", papel)
        put("email", email)
        put("estilo", estilo.name)
        put("imagemPath", imagemPath ?: "")
        put("textoDigitado", textoDigitado ?: "")
        put("assinadoEm", assinadoEm)
        put("dispositivo", dispositivo)
        put("codigo", codigo)
        put("rubricaPath", rubricaPath ?: "")
        put("iniciais", iniciais)
    }.toString()

    companion object {
        fun fromJson(json: String): CarimboSnapshot {
            val o = JSONObject(json)
            return CarimboSnapshot(
                nome = o.getString("nome"),
                cargo = o.optString("cargo").ifBlank { null },
                identificacao = o.getString("identificacao"),
                empresa = o.getString("empresa"),
                papel = o.getString("papel"),
                email = o.getString("email"),
                estilo = SignatureStyle.valueOf(o.getString("estilo")),
                imagemPath = o.optString("imagemPath").ifBlank { null },
                textoDigitado = o.optString("textoDigitado").ifBlank { null },
                assinadoEm = o.getLong("assinadoEm"),
                dispositivo = o.getString("dispositivo"),
                codigo = o.getString("codigo"),
                rubricaPath = o.optString("rubricaPath").ifBlank { null },
                iniciais = o.optString("iniciais")
            )
        }
    }
}

/** Resultado de uma assinatura. */
data class AssinaturaResultado(
    val documentoConcluido: Boolean,
    val jaAssinadoAntes: Boolean
)