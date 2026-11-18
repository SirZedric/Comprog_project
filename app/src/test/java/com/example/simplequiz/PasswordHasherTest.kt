package com.example.simplequiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class PasswordHasherTest {
    @Test
    fun correctPasswordWorksAndChangedPasswordFails() {
        val record = PasswordHasher.create("QuizDemo!2026")
        assertTrue(PasswordHasher.verify("QuizDemo!2026", record.salt, record.hash, record.iterations))
        assertFalse(PasswordHasher.verify("quizDemo!2026", record.salt, record.hash, record.iterations))
        assertFalse(PasswordHasher.verify("WrongPassword!", record.salt, record.hash, record.iterations))
        assertEquals(600_000, record.iterations)
        assertEquals(16, Base64.getDecoder().decode(record.salt).size)
        assertEquals(32, Base64.getDecoder().decode(record.hash).size)
    }

    @Test
    fun equalPasswordsReceiveIndependentSalts() {
        val first = PasswordHasher.create("SamePassword!2026")
        val second = PasswordHasher.create("SamePassword!2026")
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
        assertTrue(PasswordHasher.verify("SamePassword!2026", first.salt, first.hash, first.iterations))
        assertTrue(PasswordHasher.verify("SamePassword!2026", second.salt, second.hash, second.iterations))
    }

    @Test
    fun passwordWhitespaceRemainsSignificant() {
        val record = PasswordHasher.create(" QuizDemo!2026 ")
        assertTrue(PasswordHasher.verify(" QuizDemo!2026 ", record.salt, record.hash, record.iterations))
        assertFalse(PasswordHasher.verify("QuizDemo!2026", record.salt, record.hash, record.iterations))
    }

    @Test
    fun malformedRecordsAreRejected() {
        assertFalse(PasswordHasher.verify("QuizDemo!2026", "invalid%", "invalid%", 600_000))
        assertFalse(PasswordHasher.verify("QuizDemo!2026", "AA==", "AA==", 600_000))
        assertFalse(PasswordHasher.verify("QuizDemo!2026", "", "", 599_999))
        assertFalse(PasswordHasher.verify("QuizDemo!2026", "", "", 2_000_001))
    }
}
