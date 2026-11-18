package com.example.simplequiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

/** Displays a result already saved by QuizActivity. It never inserts a score. */
class ResultActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        val score = intent.getIntExtra(EXTRA_SCORE, -1)
        val total = intent.getIntExtra(EXTRA_TOTAL, 0)
        if (total <= 0 || score !in 0..total) {
            toast("No completed result is available.")
            finish()
            return
        }
        findViewById<TextView>(R.id.resultScoreText).text = "$score/$total"
        findViewById<TextView>(R.id.resultPercentText).text = Ui.percent(score * 100.0 / total)
        findViewById<Button>(R.id.resultPlayAgainButton).setOnClickListener {
            startActivity(Intent(this, QuizSetupActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
        }
        findViewById<Button>(R.id.resultDashboardButton).setOnClickListener { openDashboard() }
    }

    override fun onResume() {
        super.onResume()
        requireUser { /* Validate the current session before displaying this screen. */ }
    }

    private fun openDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        finish()
    }

    override fun handleBack() = openDashboard()

    companion object {
        const val EXTRA_SCORE = "score"
        const val EXTRA_TOTAL = "total"
    }
}
