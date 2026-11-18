package com.example.simplequiz

import java.io.Serializable

/** A Serializable snapshot keeps the current quiz stable after an admin edits the bank. */
data class Question(
    val id: Long,
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String
) : Serializable {
    val options: List<String>
        get() = listOf(optionA, optionB, optionC, optionD)

    val correctIndex: Int
        get() = correctAnswer.first() - 'A'
}

data class QuizUser(
    val id: Long,
    val username: String,
    val displayName: String,
    val photoFile: String?,
    val isAdmin: Boolean
)

data class QuizAttempt(
    val id: Long,
    val userId: Long,
    val score: Int,
    val total: Int,
    val completedAt: Long
) {
    val percentage: Double
        get() = if (total > 0) score * 100.0 / total else 0.0
}

data class DashboardStats(
    val attemptCount: Int,
    val averagePercent: Double,
    val bestPercent: Double
)
