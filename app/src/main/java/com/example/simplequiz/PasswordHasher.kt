package com.example.simplequiz

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Use this on the database worker thread: password derivation is intentionally slow. */
object PasswordHasher {
    private const val ITERATIONS = 600_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    data class PasswordRecord(val salt: String, val hash: String, val iterations: Int)

    fun create(password: String): PasswordRecord {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt, ITERATIONS)
        return PasswordRecord(
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(hash),
            ITERATIONS
        )
    }

    fun verify(password: String, salt: String, expectedHash: String, iterations: Int): Boolean {
        if (iterations < ITERATIONS || iterations > 2_000_000) return false
        return try {
            val decodedSalt = Base64.getDecoder().decode(salt)
            val expected = Base64.getDecoder().decode(expectedHash)
            if (decodedSalt.size != SALT_BYTES || expected.size != KEY_BITS / 8) return false
            MessageDigest.isEqual(expected, derive(password, decodedSalt, iterations))
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val characters = password.toCharArray()
        val spec = PBEKeySpec(characters, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            characters.fill('\u0000')
        }
    }
}
