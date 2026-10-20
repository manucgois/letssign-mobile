package br.com.fiap.letssign.util

import java.io.File
import java.security.MessageDigest

/** Calcula o SHA-256 de um arquivo: qualquer alteração no PDF muda completamente o resultado. */
object FileHasher {

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Versão curta para exibir no carimbo, ex.: "A1F3-9C2E". */
    fun shortCode(hash: String): String =
        hash.take(8).uppercase().chunked(4).joinToString("-")
}