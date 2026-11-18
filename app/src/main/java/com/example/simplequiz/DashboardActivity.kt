package com.example.simplequiz

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

/** Every query uses the logged-in user's ID so accounts have separate history. */
class DashboardActivity : BaseActivity() {
    private lateinit var nameText: TextView
    private lateinit var accountText: TextView
    private lateinit var statisticsText: TextView
    private lateinit var historyContainer: LinearLayout
    private lateinit var profilePhoto: ImageView
    private lateinit var startButton: Button
    private lateinit var profileButton: Button
    private lateinit var adminButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        nameText = findViewById(R.id.dashboardName)
        accountText = findViewById(R.id.dashboardAccount)
        statisticsText = findViewById(R.id.dashboardStatistics)
        historyContainer = findViewById(R.id.historyContainer)
        profilePhoto = findViewById(R.id.dashboardPhoto)
        startButton = findViewById(R.id.startQuizButton)
        profileButton = findViewById(R.id.openProfileButton)
        adminButton = findViewById(R.id.manageQuestionsButton)

        startButton.setOnClickListener {
            startActivity(Intent(this, QuizSetupActivity::class.java))
        }
        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        adminButton.setOnClickListener {
            startActivity(Intent(this, AdminQuestionsActivity::class.java))
        }
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            Session.clear(this)
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        startButton.isEnabled = false
        profileButton.isEnabled = false
        adminButton.visibility = View.GONE
        requireUser { user ->
            nameText.text = user.displayName.ifBlank { user.username }
            accountText.text = "@${user.username}  |  " +
                if (user.isAdmin) "Admin" else "User"
            Ui.setPhoto(profilePhoto, user.photoFile)
            startButton.isEnabled = true
            profileButton.isEnabled = true
            adminButton.visibility = if (user.isAdmin) View.VISIBLE else View.GONE
            runTask(
                work = { Pair(db.getStats(user.id), db.getAttempts(user.id)) },
                done = { (stats, attempts) ->
                    statisticsText.text = if (stats.attemptCount == 0) {
                        "Quizzes taken: 0\nAverage score: --\nBest score: --"
                    } else {
                        "Quizzes taken: ${stats.attemptCount}\n" +
                            "Average score: ${Ui.percent(stats.averagePercent)}\n" +
                            "Best score: ${Ui.percent(stats.bestPercent)}"
                    }
                    showHistory(attempts)
                }
            )
        }
    }

    private fun showHistory(attempts: List<QuizAttempt>) {
        historyContainer.removeAllViews()
        if (attempts.isEmpty()) {
            historyContainer.addView(TextView(this).apply {
                text = "No completed quizzes yet. Start your first quiz above."
                textSize = 16f
                setTextColor(getColor(R.color.text_secondary))
            })
            return
        }
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val density = resources.displayMetrics.density
        attempts.forEach { attempt ->
            val percent = if (attempt.total == 0) 0.0 else {
                attempt.score.toDouble() / attempt.total * 100.0
            }
            historyContainer.addView(TextView(this).apply {
                text = "${attempt.score}/${attempt.total}  (${Ui.percent(percent)})\n" +
                    dateFormat.format(Date(attempt.completedAt))
                textSize = 16f
                setTextColor(getColor(R.color.text_primary))
                setBackgroundColor(getColor(R.color.surface))
                val padding = (16 * density).toInt()
                setPadding(padding, padding, padding, padding)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * density).toInt() }
            })
        }
    }
}
