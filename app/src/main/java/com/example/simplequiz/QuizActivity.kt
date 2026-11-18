package com.example.simplequiz

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.util.UUID

/** Questions are copied into an attempt so later admin edits do not change it. */
class QuizActivity : BaseActivity() {
    private lateinit var progressText: TextView
    private lateinit var questionText: TextView
    private lateinit var feedbackText: TextView
    private lateinit var nextButton: Button
    private lateinit var answerButtons: List<Button>

    private var questions = arrayListOf<Question>()
    private var questionIndex = 0
    private var selectedIndex = -1
    private var score = 0
    private var ownerId = 0L
    private var attemptToken = UUID.randomUUID().toString()
    private var completed = false
    private var loadingQuestions = false
    private var savingResult = false
    private var showingExitDialog = false

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)
        progressText = findViewById(R.id.quizProgressText)
        questionText = findViewById(R.id.quizQuestionText)
        feedbackText = findViewById(R.id.quizFeedbackText)
        nextButton = findViewById(R.id.quizNextButton)
        answerButtons = listOf(
            findViewById(R.id.quizAnswerA), findViewById(R.id.quizAnswerB),
            findViewById(R.id.quizAnswerC), findViewById(R.id.quizAnswerD)
        )

        savedInstanceState?.let { state ->
            questions = (state.getSerializable(STATE_QUESTIONS) as? ArrayList<Question>)
                ?: arrayListOf()
            questionIndex = state.getInt(STATE_INDEX)
            selectedIndex = state.getInt(STATE_SELECTED, -1)
            score = state.getInt(STATE_SCORE)
            ownerId = state.getLong(STATE_OWNER)
            attemptToken = state.getString(STATE_TOKEN) ?: attemptToken
            completed = state.getBoolean(STATE_COMPLETED)
        }

        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener { selectAnswer(index) }
        }
        nextButton.setOnClickListener {
            when {
                savingResult -> Unit
                completed -> saveAndShowResult()
                selectedIndex == -1 -> toast("Select an answer first.")
                questionIndex == questions.lastIndex -> {
                    completed = true
                    saveAndShowResult()
                }
                else -> {
                    questionIndex++
                    selectedIndex = -1
                    renderQuestion()
                }
            }
        }
        findViewById<Button>(R.id.quizExitButton).setOnClickListener { confirmExit() }
        setLoading()
    }

    override fun onResume() {
        super.onResume()
        requireUser { user ->
            if (ownerId != 0L && ownerId != user.id) {
                toast("This quiz belongs to a different signed-in account.")
                finish()
                return@requireUser
            }
            ownerId = user.id
            if (questions.isNotEmpty()) {
                renderQuestion()
                if (completed) saveAndShowResult()
            } else if (!loadingQuestions) {
                loadQuestions()
            }
        }
    }

    private fun loadQuestions() {
        val count = intent.getIntExtra(EXTRA_QUESTION_COUNT, 0)
        if (count <= 0) {
            toast("Choose the number of questions before starting.")
            finish()
            return
        }
        loadingQuestions = true
        runTask(
            work = { db.getRandomQuestions(count) },
            done = { loaded ->
                if (loaded.size != count) {
                    toast("The question bank changed. Please choose your quiz size again.")
                    finish()
                } else {
                    questions = ArrayList(loaded)
                    renderQuestion()
                }
            },
            finished = { loadingQuestions = false }
        )
    }

    private fun setLoading() {
        progressText.text = "Loading quiz..."
        questionText.text = "Please wait."
        feedbackText.text = ""
        answerButtons.forEach { it.isEnabled = false }
        nextButton.isEnabled = false
    }

    private fun renderQuestion() {
        if (questions.isEmpty() || questionIndex !in questions.indices) return
        val question = questions[questionIndex]
        progressText.text = "Question ${questionIndex + 1} of ${questions.size}"
        questionText.text = question.text

        answerButtons.forEachIndexed { index, button ->
            button.text = "${('A'.code + index).toChar()}. ${question.options[index]}"
            val color = when {
                selectedIndex != -1 && index == question.correctIndex -> R.color.correct
                index == selectedIndex && selectedIndex != question.correctIndex -> R.color.incorrect
                else -> R.color.surface
            }
            button.backgroundTintList = ColorStateList.valueOf(getColor(color))
            button.setTextColor(
                if (color == R.color.surface) getColor(R.color.text_primary) else Color.WHITE
            )
            button.isEnabled = selectedIndex == -1 && !completed
        }

        feedbackText.text = when {
            selectedIndex == -1 -> "Choose one answer."
            selectedIndex == question.correctIndex -> "Correct!"
            else -> "Incorrect. Correct answer: ${question.correctAnswer}. " +
                question.options[question.correctIndex]
        }
        feedbackText.setTextColor(getColor(when {
            selectedIndex == -1 -> R.color.text_secondary
            selectedIndex == question.correctIndex -> R.color.correct
            else -> R.color.incorrect
        }))
        nextButton.text = when {
            savingResult -> "Saving result..."
            completed -> "Retry saving result"
            questionIndex == questions.lastIndex -> "Finish Quiz"
            else -> "Next Question"
        }
        nextButton.isEnabled = selectedIndex != -1 && !savingResult
    }

    private fun selectAnswer(index: Int) {
        if (selectedIndex != -1 || questions.isEmpty() || completed || ownerId == 0L) return
        selectedIndex = index
        if (index == questions[questionIndex].correctIndex) score++
        renderQuestion()
    }

    private fun saveAndShowResult() {
        if (!completed || savingResult || questions.isEmpty() || ownerId == 0L) return
        savingResult = true
        renderQuestion()
        runTask(
            work = { db.saveAttempt(ownerId, attemptToken, score, questions.size) },
            done = {
                startActivity(Intent(this, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_SCORE, score)
                    putExtra(ResultActivity.EXTRA_TOTAL, questions.size)
                })
                finish()
            },
            finished = {
                savingResult = false
                renderQuestion()
            }
        )
    }

    private fun confirmExit() {
        if (savingResult || completed) {
            toast("Finish saving your result first.")
            return
        }
        if (showingExitDialog) return
        showingExitDialog = true
        AlertDialog.Builder(this)
            .setTitle("Leave this quiz?")
            .setMessage("An unfinished quiz is not added to your score history.")
            .setNegativeButton("Continue", null)
            .setPositiveButton("Leave") { _, _ -> finish() }
            .setOnDismissListener { showingExitDialog = false }
            .show()
    }

    override fun handleBack() = confirmExit()

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(STATE_QUESTIONS, questions)
        outState.putInt(STATE_INDEX, questionIndex)
        outState.putInt(STATE_SELECTED, selectedIndex)
        outState.putInt(STATE_SCORE, score)
        outState.putLong(STATE_OWNER, ownerId)
        outState.putString(STATE_TOKEN, attemptToken)
        outState.putBoolean(STATE_COMPLETED, completed)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val EXTRA_QUESTION_COUNT = "question_count"
        private const val STATE_QUESTIONS = "questions"
        private const val STATE_INDEX = "question_index"
        private const val STATE_SELECTED = "selected_index"
        private const val STATE_SCORE = "score"
        private const val STATE_OWNER = "owner_id"
        private const val STATE_TOKEN = "attempt_token"
        private const val STATE_COMPLETED = "completed"
    }
}
