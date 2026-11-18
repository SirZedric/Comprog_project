package com.example.simplequiz

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView

/** The same form handles both adding a question and editing an existing one. */
class QuestionEditorActivity : BaseActivity() {
    companion object {
        const val EXTRA_QUESTION = "com.example.simplequiz.QUESTION_TO_EDIT"
    }

    private lateinit var questionInput: EditText
    private lateinit var choiceInputs: List<EditText>
    private lateinit var answerSpinner: Spinner
    private lateinit var saveButton: Button
    private var existingQuestion: Question? = null
    private var saving = false

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_editor)
        questionInput = findViewById(R.id.questionTextInput)
        choiceInputs = listOf(
            findViewById(R.id.optionAInput), findViewById(R.id.optionBInput),
            findViewById(R.id.optionCInput), findViewById(R.id.optionDInput)
        )
        answerSpinner = findViewById(R.id.correctAnswerSpinner)
        saveButton = findViewById(R.id.saveQuestionButton)
        existingQuestion = intent.getSerializableExtra(EXTRA_QUESTION) as? Question

        answerSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, listOf("A", "B", "C", "D")
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        findViewById<TextView>(R.id.questionEditorTitle).text =
            if (existingQuestion == null) "Add question" else "Edit question #${existingQuestion!!.id}"
        // Android restores the editable fields automatically after a rotation.
        if (savedInstanceState == null) {
            existingQuestion?.let { question ->
                questionInput.setText(question.text)
                choiceInputs.forEachIndexed { index, field -> field.setText(question.options[index]) }
                answerSpinner.setSelection(question.correctIndex)
            }
        }
        saveButton.setOnClickListener { saveQuestion() }
        findViewById<Button>(R.id.cancelQuestionButton).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        requireUser { user ->
            if (!user.isAdmin) {
                toast("Only administrators can manage questions.")
                finish()
            }
        }
    }

    private fun saveQuestion() {
        if (saving) return
        val questionText = questionInput.text.toString().trim()
        val choices = choiceInputs.map { it.text.toString().trim() }
        if (questionText.isEmpty()) {
            questionInput.error = "Enter a question."
            questionInput.requestFocus()
            return
        }
        val emptyChoice = choices.indexOfFirst { it.isEmpty() }
        if (emptyChoice >= 0) {
            choiceInputs[emptyChoice].error = "Enter this choice."
            choiceInputs[emptyChoice].requestFocus()
            return
        }
        val question = Question(
            id = existingQuestion?.id ?: 0L,
            text = questionText,
            optionA = choices[0], optionB = choices[1],
            optionC = choices[2], optionD = choices[3],
            correctAnswer = listOf("A", "B", "C", "D")[answerSpinner.selectedItemPosition]
        )
        saving = true
        saveButton.isEnabled = false
        requireUser { user ->
            if (!user.isAdmin) {
                toast("Only administrators can manage questions.")
                finish()
                return@requireUser
            }
            runTask(
                work = { db.saveQuestion(user.id, question) },
                done = {
                    toast("Question saved.")
                    finish()
                },
                finished = {
                    saving = false
                    saveButton.isEnabled = true
                }
            )
        }
    }
}
