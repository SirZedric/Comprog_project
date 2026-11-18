package com.example.simplequiz

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** All four administrators share the same question bank on this installation. */
class AdminQuestionsActivity : BaseActivity() {
    private lateinit var questionsContainer: LinearLayout
    private lateinit var countText: TextView
    private lateinit var addButton: Button
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_questions)
        questionsContainer = findViewById(R.id.questionsContainer)
        countText = findViewById(R.id.questionBankCount)
        addButton = findViewById(R.id.addQuestionButton)

        addButton.setOnClickListener {
            if (!busy) startActivity(Intent(this, QuestionEditorActivity::class.java))
        }
        findViewById<Button>(R.id.adminBackButton).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refreshQuestions()
    }

    private fun refreshQuestions() {
        if (busy) return
        setBusy(true)
        requireUser { user ->
            if (!user.isAdmin) {
                toast("Only administrators can manage questions.")
                finish()
                return@requireUser
            }
            runTask(
                work = { db.getQuestionsForAdmin(user.id) },
                done = { questions -> showQuestions(questions) },
                finished = { setBusy(false) }
            )
        }
    }

    private fun showQuestions(questions: List<Question>) {
        countText.text = "${questions.size} questions currently available"
        questionsContainer.removeAllViews()
        if (questions.isEmpty()) {
            questionsContainer.addView(TextView(this).apply {
                text = "The bank is empty. Add a question to make quizzes available."
                textSize = 16f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, dp(16), 0, dp(16))
            })
            return
        }

        questions.forEach { question ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(getColor(R.color.surface))
                setPadding(dp(16), dp(16), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(16) }
            }
            row.addView(TextView(this).apply {
                text = "#${question.id}  ${question.text}"
                textSize = 17f
                setTextColor(getColor(R.color.text_primary))
            })
            row.addView(TextView(this).apply {
                val answerText = question.options.getOrNull(question.correctIndex).orEmpty()
                text = "Correct answer: ${question.correctAnswer}. $answerText"
                textSize = 15f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, dp(8), 0, dp(8))
            })
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(Button(this).apply {
                text = "Edit"
                minHeight = dp(48)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    if (!busy) startActivity(
                        Intent(this@AdminQuestionsActivity, QuestionEditorActivity::class.java)
                            .putExtra(QuestionEditorActivity.EXTRA_QUESTION, question)
                    )
                }
            })
            actions.addView(Button(this).apply {
                text = "Delete"
                minHeight = dp(48)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    if (!busy) confirmDelete(question)
                }
            })
            row.addView(actions)
            questionsContainer.addView(row)
        }
    }

    private fun confirmDelete(question: Question) {
        AlertDialog.Builder(this)
            .setTitle("Delete question?")
            .setMessage("Remove question #${question.id}? Previously saved scores will stay unchanged.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> deleteQuestion(question.id) }
            .show()
    }

    private fun deleteQuestion(questionId: Long) {
        if (busy) return
        setBusy(true)
        requireUser { user ->
            if (!user.isAdmin) {
                toast("Only administrators can manage questions.")
                finish()
                return@requireUser
            }
            runTask(
                work = {
                    val deleted = db.deleteQuestion(user.id, questionId)
                    deleted to db.getQuestionsForAdmin(user.id)
                },
                done = { (deleted, questions) ->
                    toast(if (deleted) "Question deleted." else "This question was already removed.")
                    showQuestions(questions)
                },
                finished = { setBusy(false) }
            )
        }
    }

    private fun setBusy(value: Boolean) {
        busy = value
        addButton.isEnabled = !value
        findViewById<View>(R.id.adminLoadingText).visibility = if (value) View.VISIBLE else View.GONE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
