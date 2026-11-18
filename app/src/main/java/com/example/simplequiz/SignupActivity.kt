package com.example.simplequiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

/** Public sign-up always creates a regular user, never an admin. */
class SignupActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        val usernameInput = findViewById<EditText>(R.id.signupUsername)
        val passwordInput = findViewById<EditText>(R.id.signupPassword)
        val createButton = findViewById<Button>(R.id.signupButton)
        val backButton = findViewById<Button>(R.id.backToLoginButton)

        backButton.setOnClickListener { finish() }
        createButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (!username.matches(Regex("[A-Za-z0-9_]{3,30}"))) {
                toast("Use 3 to 30 letters, numbers, or underscores for your username.")
                return@setOnClickListener
            }
            if (password.length !in 8..128) {
                toast("Use a password containing 8 to 128 characters.")
                return@setOnClickListener
            }
            createButton.isEnabled = false
            backButton.isEnabled = false
            runTask(
                work = { db.signUp(username, password) },
                done = { user ->
                    passwordInput.text.clear()
                    Session.setUserId(this, user.id)
                    startActivity(Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                },
                finished = {
                    createButton.isEnabled = true
                    backButton.isEnabled = true
                }
            )
        }
    }
}
