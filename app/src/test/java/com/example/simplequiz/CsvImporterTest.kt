package com.example.simplequiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.StringReader

class CsvImporterTest {
    private val header = "id,question,option_a,option_b,option_c,option_d,correct_answer\n"

    @Test
    fun bundledBankContainsOneHundredValidUniqueQuestions() {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("questions.csv")) {
            "The bundled question bank was not added to the test resources."
        }
        val questions = stream.bufferedReader(Charsets.UTF_8).use { CsvImporter.read(it) }
        assertEquals(100, questions.size)
        assertEquals(100, questions.map { it.id }.distinct().size)
        assertTrue(questions.all { it.correctIndex in 0..3 })
    }

    @Test
    fun quotedCommaEscapedQuoteAndBomArePreserved() {
        val csv = "\uFEFF" + header + "1,\"What does \"\"CPU\"\" mean, exactly?\",A,B,C,D,a\r\n"
        val question = CsvImporter.read(StringReader(csv)).single()
        assertEquals("What does \"CPU\" mean, exactly?", question.text)
        assertEquals("A", question.correctAnswer)
    }

    @Test
    fun duplicateQuestionIdsAreRejected() {
        rejects(header + "1,Question,A,B,C,D,A\n1,Other,A,B,C,D,B\n")
    }

    @Test
    fun invalidCorrectAnswerIsRejected() {
        rejects(header + "1,Question,A,B,C,D,E\n")
    }

    @Test
    fun emptyQuestionFieldIsRejected() {
        rejects(header + "1,,A,B,C,D,A\n")
    }

    @Test
    fun wrongHeaderAndUnclosedQuotedFieldAreRejected() {
        rejects("id,wrong\n1,Question\n")
        rejects(header + "1,\"Never closed,A,B,C,D,A\n")
    }

    private fun rejects(csv: String) {
        try {
            CsvImporter.read(StringReader(csv))
            fail("Expected the invalid CSV to be rejected.")
        } catch (_: IllegalArgumentException) {
            // Expected validation failure.
        }
    }
}
