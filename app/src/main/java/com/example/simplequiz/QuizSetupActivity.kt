package com.example.simplequiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

/** Lets the signed-in player choose how many questions to answer. */
class QuizSetupActivity : BaseActivity() {
    private lateinit var availableText: TextView
    private lateinit var countInput: EditText
    private lateinit var startButton: Button
    private var availableCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_setup)
        availableText = findViewById(R.id.setupAvailableText)
        countInput = findViewById(R.id.setupCountInput)
        startButton = findViewById(R.id.setupStartButton)

        findViewById<Button>(R.id.setupBackButton).setOnClickListener { finish() }
        startButton.setOnClickListener {
            val count = countInput.text.toString().trim().toIntOrNull()
            if (count == null || count !in 1..availableCount) {
                countInput.error = "Enter a whole number from 1 to $availableCount."
            } else {
                startActivity(Intent(this, QuizActivity::class.java).apply {
                    putExtra(QuizActivity.EXTRA_QUESTION_COUNT, count)
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startButton.isEnabled = false
        requireUser {
            runTask(
                work = { db.questionCount() },
                done = { count ->
                    availableCount = count
                    availableText.text = if (count == 0) {
                        "The question bank is empty. Ask an administrator to add questions."
                    } else {
                        "$count questions available. Choose from 1 to $count."
                    }
                    countInput.isEnabled = count > 0
                    startButton.isEnabled = count > 0
                    if (countInput.text.isBlank() && count > 0) {
                        countInput.setText(minOf(10, count).toString())
                    }
                }
            )
        }
    }
}
