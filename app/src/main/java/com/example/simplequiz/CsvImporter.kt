package com.example.simplequiz

import java.io.Reader
import java.util.Locale

/** Reads quoted CSV fields, escaped quotes, CRLF/LF records, and a UTF-8 BOM. */
object CsvImporter {
    private val expectedHeader = listOf(
        "id", "question", "option_a", "option_b", "option_c", "option_d", "correct_answer"
    )

    fun read(reader: Reader): List<Question> {
        val rows = parseRecords(reader.readText().removePrefix("\uFEFF"))
        require(rows.isNotEmpty()) { "The question CSV is empty." }
        require(rows.first() == expectedHeader) {
            "CSV header must be ${expectedHeader.joinToString(",")}."
        }
        val ids = mutableSetOf<Long>()
        val questions = rows.drop(1).mapIndexed { index, row ->
            val label = "CSV record ${index + 2}"
            require(row.size == 7) { "$label must contain exactly seven columns." }
            val id = row[0].trim().toLongOrNull()
            require(id != null && id > 0) { "$label needs a positive integer ID." }
            require(ids.add(id)) { "$label repeats question ID $id." }
            val fields = row.subList(1, 6).map { it.trim() }
            require(fields.all { it.isNotEmpty() }) { "$label has a blank question or choice." }
            val answer = row[6].trim().uppercase(Locale.ROOT)
            require(answer in listOf("A", "B", "C", "D")) {
                "$label correct_answer must be A, B, C, or D."
            }
            Question(id, fields[0], fields[1], fields[2], fields[3], fields[4], answer)
        }
        require(questions.isNotEmpty()) { "The CSV must contain at least one question." }
        return questions
    }

    private fun parseRecords(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var closedQuote = false
        var fieldStarted = false
        var index = 0

        fun finishField() {
            row.add(field.toString())
            field.setLength(0)
            fieldStarted = false
            closedQuote = false
        }

        fun finishRecord() {
            finishField()
            if (row.any { it.isNotBlank() }) records.add(row.toList())
            row.clear()
        }

        while (index < text.length) {
            val character = text[index]
            if (inQuotes) {
                if (character == '"') {
                    if (index + 1 < text.length && text[index + 1] == '"') {
                        field.append('"')
                        index++
                    } else {
                        inQuotes = false
                        closedQuote = true
                    }
                } else {
                    field.append(character)
                }
            } else {
                when (character) {
                    ',' -> finishField()
                    '\n' -> finishRecord()
                    '\r' -> {
                        finishRecord()
                        if (index + 1 < text.length && text[index + 1] == '\n') index++
                    }
                    '"' -> {
                        require(!fieldStarted && !closedQuote) { "A CSV quote must start a field." }
                        inQuotes = true
                        fieldStarted = true
                    }
                    else -> {
                        if (closedQuote) {
                            require(character == ' ' || character == '\t') {
                                "Unexpected characters after a closing CSV quote."
                            }
                        } else {
                            field.append(character)
                            fieldStarted = true
                        }
                    }
                }
            }
            index++
        }
        require(!inQuotes) { "The CSV contains an unclosed quoted field." }
        if (fieldStarted || closedQuote || field.isNotEmpty() || row.isNotEmpty()) finishRecord()
        return records
    }
}
