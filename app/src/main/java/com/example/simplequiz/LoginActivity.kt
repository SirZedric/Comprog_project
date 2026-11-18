package com.example.simplequiz

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** The first person opening a new installation sets its four admin passwords. */
class LoginActivity : BaseActivity() {
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var statusText: TextView
    private var setupDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        usernameInput = findViewById(R.id.loginUsername)
        passwordInput = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.loginButton)
        signupButton = findViewById(R.id.openSignupButton)
        statusText = findViewById(R.id.loginStatus)

        loginButton.setOnClickListener { logIn() }
        signupButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        setReady(false)
        runTask(
            work = {
                db.ensureQuestionsSeeded()
                val userId = Session.getUserId(this)
                Pair(db.hasAdmins(), userId?.let { db.getUser(it) })
            },
            done = { (adminsExist, savedUser) ->
                when {
                    !adminsExist -> showAdminSetup()
                    savedUser != null -> openDashboard()
                    else -> {
                        Session.clear(this)
                        setReady(true)
                    }
                }
            }
        )
    }

    private fun setReady(ready: Boolean) {
        loginButton.isEnabled = ready
        signupButton.isEnabled = ready
        statusText.visibility = if (ready) View.GONE else View.VISIBLE
        statusText.text = "Preparing the local question bank..."
    }

    private fun logIn() {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()
        if (username.isEmpty() || password.isEmpty()) {
            toast("Enter your username and password.")
            return
        }
        loginButton.isEnabled = false
        signupButton.isEnabled = false
        runTask(
            work = { db.login(username, password) },
            done = { user ->
                passwordInput.text.clear()
                if (user == null) {
                    toast("The username or password is incorrect.")
                } else {
                    Session.setUserId(this, user.id)
                    openDashboard()
                }
            },
            finished = {
                loginButton.isEnabled = true
                signupButton.isEnabled = true
            }
        )
    }

    private fun showAdminSetup() {
        if (setupDialog?.isShowing == true) return
        statusText.text = "Set up the four admin accounts for this installation."
        val padding = (20 * resources.displayMetrics.density).toInt()
        val contents = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        contents.addView(TextView(this).apply {
            text = "First installation only. Set a password for each local account: " +
                "admin1, admin2, admin3, and admin4. Use 8 to 128 characters. " +
                "Record these passwords before continuing."
            textSize = 16f
        })
        val passwordFields = (1..4).map { number ->
            EditText(this).apply {
                hint = "Password for admin$number"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                isSaveEnabled = false
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                minHeight = (48 * resources.displayMetrics.density).toInt()
                setSingleLine(true)
                contents.addView(this)
            }
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Create four local admins")
            .setView(ScrollView(this).apply { addView(contents) })
            .setCancelable(false)
            .setPositiveButton("Create admin accounts", null)
            .create()
        setupDialog = dialog
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val passwords = passwordFields.map { field -> field.text.toString() }
                if (passwords.any { it.length !in 8..128 }) {
                    toast("Every admin password must contain 8 to 128 characters.")
                    return@setOnClickListener
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                runTask(
                    work = { db.initializeAdmins(passwords) },
                    done = {
                        passwordFields.forEach { it.text.clear() }
                        dialog.dismiss()
                        setReady(true)
                        toast("Admin accounts created. You can now log in or sign up.")
                    },
                    finished = {
                        if (dialog.isShowing) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        }
                    }
                )
            }
        }
        dialog.show()
    }

    private fun openDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        setupDialog?.dismiss()
        super.onDestroy()
    }
}
