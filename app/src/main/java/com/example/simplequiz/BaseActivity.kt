package com.example.simplequiz

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.window.OnBackInvokedDispatcher
import android.widget.Toast
import java.util.concurrent.Executors

/** Shared background work and session checks for these small Activity screens. */
open class BaseActivity : Activity() {
    protected val db: DatabaseHelper
        get() = synchronized(BaseActivity::class.java) {
            sharedDatabase ?: DatabaseHelper(applicationContext).also { sharedDatabase = it }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Screens use ordinary Android widgets and the platform light theme.
        window.statusBarColor = getColor(R.color.primary)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) { handleBack() }
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        // Keep controls clear of system bars when newer Android targets enforce edge-to-edge.
        @Suppress("DEPRECATION")
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(left + insets.systemWindowInsetLeft,
                top + insets.systemWindowInsetTop,
                right + insets.systemWindowInsetRight,
                bottom + insets.systemWindowInsetBottom)
            insets.consumeSystemWindowInsets()
        }
        root.requestApplyInsets()
    }

    protected open fun handleBack() { finish() }

    @Deprecated("Older Android versions use this callback.")
    override fun onBackPressed() = handleBack()

    protected fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /** Database reads, writes, CSV loading, and password hashing run off the UI thread. */
    protected fun <T> runTask(
        work: () -> T,
        done: (T) -> Unit,
        finished: () -> Unit = {}
    ) {
        worker.execute {
            try {
                val result = work()
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        try { done(result) } finally { finished() }
                    }
                }
            } catch (exception: Exception) {
                val message = when (exception) {
                    is IllegalArgumentException -> exception.message ?: "Please check your input."
                    is SecurityException -> "This action is not allowed for this account."
                    else -> "The action could not be completed. Please try again."
                }
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        toast(message)
                        finished()
                    }
                }
            }
        }
    }

    protected fun requireUser(onReady: (QuizUser) -> Unit) {
        val userId = Session.getUserId(this)
        if (userId == null) {
            returnToLogin()
            return
        }
        runTask(
            work = { db.getUser(userId) },
            done = { user ->
                if (user == null) returnToLogin() else onReady(user)
            }
        )
    }

    private fun returnToLogin() {
        Session.clear(this)
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    companion object {
        private var sharedDatabase: DatabaseHelper? = null
        private val worker = Executors.newSingleThreadExecutor()
    }
}
