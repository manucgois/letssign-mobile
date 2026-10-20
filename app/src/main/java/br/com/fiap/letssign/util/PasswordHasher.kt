package br.com.fiap.letssign.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Transforma a senha em um hash irreversível usando PBKDF2 (padrão recomendado para senhas).
 * Nunca guardamos a senha original no banco.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000 // quanto maior, mais difícil de quebrar (e mais lento)
    private const val KEY_LENGTH = 256
    private const val SALT_SIZE = 16

    /** Gera um "salt": valor aleatório que deixa cada hash único, mesmo para senhas iguais. */
    fun generateSalt(): String {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.decode(salt, Base64.NO_WRAP),
            ITERATIONS,
            KEY_LENGTH
        )
        return try {
            val bytes = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            spec.clearPassword() // apaga a senha da memória
        }
    }

    /** Compara em tempo constante (evita ataques que medem o tempo da comparação). */
    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val actual = hash(password, salt)
        return MessageDigest.isEqual(actual.toByteArray(), expectedHash.toByteArray())
    }
}