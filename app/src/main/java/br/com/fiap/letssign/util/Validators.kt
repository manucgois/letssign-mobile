package br.com.fiap.letssign.util

import br.com.fiap.letssign.domain.model.IdentificationType

object Validators {

    private val UFS = setOf(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
        "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    )

    fun onlyDigits(value: String): String = value.filter { it.isDigit() }

    /** Valida o CPF pelos dígitos verificadores (não consulta a Receita). */
    fun isValidCpf(value: String): Boolean {
        val cpf = onlyDigits(value)
        if (cpf.length != 11 || cpf.all { it == cpf[0] }) return false
        val d = cpf.map { it - '0' }

        fun digito(qtd: Int): Int {
            val soma = (0 until qtd).sumOf { d[it] * (qtd + 1 - it) }
            val resto = (soma * 10) % 11
            return if (resto == 10) 0 else resto
        }
        return digito(9) == d[9] && digito(10) == d[10]
    }

    /** OAB = UF + número. Aceita "MG 123456", "MG123.456", "mg-123456"... */
    fun isValidOab(value: String): Boolean {
        val limpo = value.uppercase().replace(Regex("[^A-Z0-9]"), "")
        val match = Regex("^([A-Z]{2})(\\d{3,6})$").find(limpo) ?: return false
        return match.groupValues[1] in UFS
    }

    /** LGPD: CPF mascarado no carimbo, ex.: ***.456.789-** */
    fun maskCpf(value: String): String {
        val cpf = onlyDigits(value)
        if (cpf.length != 11) return value
        return "***.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-**"
    }

    /** Retorna a mensagem de erro, ou null se estiver tudo certo. */
    fun validateIdentification(tipo: IdentificationType, numero: String): String? {
        if (numero.isBlank()) return "Informe o número da identificação"
        return when (tipo) {
            IdentificationType.CPF ->
                if (isValidCpf(numero)) null else "CPF inválido"
            IdentificationType.OAB ->
                if (isValidOab(numero)) null else "Use UF + número, ex.: MG 123456"
            else ->
                if (numero.trim().length >= 3) null else "Número muito curto"
        }
    }

    fun isValidEmail(value: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()

    fun validateFullName(value: String): String? = when {
        value.isBlank() -> "Informe o nome completo"
        value.trim().split(Regex("\\s+")).size < 2 -> "Informe nome e sobrenome"
        else -> null
    }

    fun validatePhone(value: String): String? {
        val digitos = onlyDigits(value)
        return if (digitos.length == 10 || digitos.length == 11) null
        else "Informe DDD + número, ex.: (31) 99999-0000"
    }

    /** Formata para exibição: (31) 99999-0000 ou (31) 3333-0000 */
    fun formatPhone(value: String): String {
        val d = onlyDigits(value)
        return when (d.length) {
            11 -> "(${d.take(2)}) ${d.substring(2, 7)}-${d.substring(7)}"
            10 -> "(${d.take(2)}) ${d.substring(2, 6)}-${d.substring(6)}"
            else -> value.trim()
        }
    }

    /** Senha forte: mínimo 8 caracteres, com letras e números. */
    fun validatePassword(value: String): String? = when {
        value.length < 8 -> "Mínimo de 8 caracteres"
        !value.any { it.isLetter() } || !value.any { it.isDigit() } -> "Use letras e números"
        else -> null
    }
}