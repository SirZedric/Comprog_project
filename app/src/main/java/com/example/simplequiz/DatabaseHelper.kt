package com.example.simplequiz

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Locale

/** All public methods are called through the shared worker in BaseActivity.kt. */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext, "simple_quiz.db", null, 1
) {
    private val appContext = context.applicationContext

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL COLLATE NOCASE UNIQUE,
                display_name TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                password_iterations INTEGER NOT NULL,
                photo_file TEXT,
                role TEXT NOT NULL CHECK(role IN ('user', 'admin'))
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE questions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                question TEXT NOT NULL,
                option_a TEXT NOT NULL,
                option_b TEXT NOT NULL,
                option_c TEXT NOT NULL,
                option_d TEXT NOT NULL,
                correct_answer TEXT NOT NULL CHECK(correct_answer IN ('A','B','C','D'))
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE quiz_attempts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                attempt_token TEXT NOT NULL,
                score INTEGER NOT NULL,
                total_questions INTEGER NOT NULL CHECK(total_questions > 0),
                completed_at INTEGER NOT NULL,
                CHECK(score >= 0 AND score <= total_questions),
                UNIQUE(user_id, attempt_token)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX attempts_user_date ON quiz_attempts(user_id, completed_at DESC)")
        db.execSQL("CREATE TABLE app_metadata (name TEXT PRIMARY KEY, value TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the initial schema. Add non-destructive migrations here when it changes.
        error("No migration is defined from database version $oldVersion to $newVersion.")
    }

    fun hasAdmins(): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM users WHERE role = ? LIMIT 1", arrayOf("admin")
    ).use { it.moveToFirst() }

    /** Four local admins are created once; passwords are chosen on the setup screen. */
    fun initializeAdmins(passwords: List<String>) {
        require(passwords.size == 4) { "Enter a password for each of the four administrators." }
        passwords.forEach { validatePassword(it) }
        // Do the costly hashing before obtaining a write transaction.
        val passwordRecords = passwords.map { PasswordHasher.create(it) }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val initialized = db.rawQuery(
                "SELECT 1 FROM app_metadata WHERE name = ?", arrayOf("admins_initialized")
            ).use { it.moveToFirst() }
            check(!initialized && !hasAdmins()) { "Administrator accounts have already been created." }
            passwordRecords.forEachIndexed { index, record ->
                val username = "admin${index + 1}"
                db.insertOrThrow("users", null, userValues(username, username, record, "admin"))
            }
            putMetadata(db, "admins_initialized", "1")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun signUp(username: String, password: String): QuizUser {
        val cleaned = username.trim()
        require(cleaned.matches(Regex("[A-Za-z0-9_]{3,30}"))) {
            "Username must have 3–30 letters, numbers, or underscores."
        }
        require(!cleaned.lowercase(Locale.ROOT).matches(Regex("admin[1-4]"))) {
            "That username is reserved for an administrator."
        }
        validatePassword(password)
        check(hasAdmins()) { "Finish administrator setup before creating regular accounts." }
        val record = PasswordHasher.create(password)
        val id = try {
            writableDatabase.insertOrThrow("users", null, userValues(cleaned, cleaned, record, "user"))
        } catch (_: SQLiteConstraintException) {
            throw IllegalArgumentException("That username is already taken.")
        }
        return checkNotNull(getUser(id))
    }

    fun login(username: String, password: String): QuizUser? {
        if (password.length !in 8..128) return null
        return readableDatabase.rawQuery(
            "SELECT * FROM users WHERE username = ? COLLATE NOCASE LIMIT 1",
            arrayOf(username.trim())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val accepted = PasswordHasher.verify(
                password,
                cursor.string("password_salt"),
                cursor.string("password_hash"),
                cursor.int("password_iterations")
            )
            if (accepted) cursor.toUser() else null
        }
    }

    fun getUser(userId: Long): QuizUser? = readableDatabase.rawQuery(
        "SELECT * FROM users WHERE id = ?", arrayOf(userId.toString())
    ).use { if (it.moveToFirst()) it.toUser() else null }

    fun updateProfile(userId: Long, displayName: String, photoFile: String?) {
        val name = displayName.trim()
        require(name.length in 1..60) { "Name must have 1–60 characters." }
        require(photoFile == null || photoFile.matches(Regex("[A-Za-z0-9_.-]+"))) {
            "Invalid profile picture filename."
        }
        val values = ContentValues().apply {
            put("display_name", name)
            if (photoFile == null) putNull("photo_file") else put("photo_file", photoFile)
        }
        check(writableDatabase.update("users", values, "id = ?", arrayOf(userId.toString())) == 1) {
            "The user account could not be found."
        }
    }

    fun questionCount(): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) AS total FROM questions", null
    ).use { it.moveToFirst(); it.int("total") }

    fun getRandomQuestions(limit: Int): List<Question> {
        require(limit > 0) { "Choose at least one question." }
        return readableDatabase.rawQuery(
            "SELECT * FROM questions ORDER BY RANDOM() LIMIT ?", arrayOf(limit.toString())
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toQuestion()) } }
    }

    fun getAllQuestions(): List<Question> = readableDatabase.rawQuery(
        "SELECT * FROM questions ORDER BY id", null
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toQuestion()) } }

    fun getQuestionsForAdmin(actorId: Long): List<Question> {
        requireAdmin(actorId)
        return getAllQuestions()
    }

    fun saveQuestion(actorId: Long, question: Question): Long {
        requireAdmin(actorId)
        val cleaned = validateQuestion(question)
        val values = questionValues(cleaned, includeId = false)
        return if (cleaned.id == 0L) {
            writableDatabase.insertOrThrow("questions", null, values)
        } else {
            check(writableDatabase.update(
                "questions", values, "id = ?", arrayOf(cleaned.id.toString())
            ) == 1) { "That question no longer exists." }
            cleaned.id
        }
    }

    fun deleteQuestion(actorId: Long, questionId: Long): Boolean {
        requireAdmin(actorId)
        return writableDatabase.delete("questions", "id = ?", arrayOf(questionId.toString())) == 1
    }

    /** Token uniqueness prevents a second save after rotation, back navigation, or rapid taps. */
    fun saveAttempt(userId: Long, attemptToken: String, score: Int, total: Int): Long {
        require(attemptToken.isNotBlank() && attemptToken.length <= 100) { "Invalid quiz attempt token." }
        require(total > 0 && score in 0..total) { "Invalid quiz score." }
        val db = writableDatabase
        db.beginTransaction()
        try {
            check(getUser(userId) != null) { "Please sign in again." }
            val previous = db.rawQuery(
                "SELECT id, score, total_questions FROM quiz_attempts WHERE user_id = ? AND attempt_token = ?",
                arrayOf(userId.toString(), attemptToken)
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    check(cursor.int("score") == score && cursor.int("total_questions") == total) {
                        "This quiz attempt has already been saved with different results."
                    }
                    cursor.long("id")
                } else null
            }
            val id = previous ?: db.insertOrThrow("quiz_attempts", null, ContentValues().apply {
                put("user_id", userId)
                put("attempt_token", attemptToken)
                put("score", score)
                put("total_questions", total)
                put("completed_at", System.currentTimeMillis())
            })
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    fun getAttempts(userId: Long): List<QuizAttempt> = readableDatabase.rawQuery(
        "SELECT * FROM quiz_attempts WHERE user_id = ? ORDER BY completed_at DESC, id DESC",
        arrayOf(userId.toString())
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(QuizAttempt(
                cursor.long("id"), cursor.long("user_id"), cursor.int("score"),
                cursor.int("total_questions"), cursor.long("completed_at")
            ))
        }
    }

    /** Average is the arithmetic mean of attempt percentages, so all quiz lengths are comparable. */
    fun getStats(userId: Long): DashboardStats = readableDatabase.rawQuery(
        """
            SELECT COUNT(*) AS attempts,
                   COALESCE(AVG(100.0 * score / total_questions), 0) AS average_percent,
                   COALESCE(MAX(100.0 * score / total_questions), 0) AS best_percent
            FROM quiz_attempts WHERE user_id = ?
        """.trimIndent(), arrayOf(userId.toString())
    ).use { cursor ->
        cursor.moveToFirst()
        DashboardStats(cursor.int("attempts"), cursor.double("average_percent"), cursor.double("best_percent"))
    }

    /** A marker prevents deleted questions from returning on the next app launch. */
    fun ensureQuestionsSeeded(): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val seeded = db.rawQuery(
                "SELECT 1 FROM app_metadata WHERE name = ?", arrayOf("question_bank_seeded")
            ).use { it.moveToFirst() }
            if (!seeded) {
                val questions = appContext.assets.open("questions.csv").bufferedReader(Charsets.UTF_8).use {
                    CsvImporter.read(it)
                }
                questions.forEach { question ->
                    db.insertOrThrow("questions", null, questionValues(question, includeId = true))
                }
                putMetadata(db, "question_bank_seeded", "1")
            }
            val count = questionCount()
            db.setTransactionSuccessful()
            return count
        } finally {
            db.endTransaction()
        }
    }

    private fun requireAdmin(actorId: Long) {
        check(getUser(actorId)?.isAdmin == true) { "An administrator account is required." }
    }

    private fun validateQuestion(question: Question): Question {
        require(question.id >= 0) { "Invalid question ID." }
        val cleaned = question.copy(
            text = question.text.trim(), optionA = question.optionA.trim(),
            optionB = question.optionB.trim(), optionC = question.optionC.trim(),
            optionD = question.optionD.trim(), correctAnswer = question.correctAnswer.trim().uppercase(Locale.ROOT)
        )
        require(cleaned.text.isNotEmpty() && cleaned.options.all { it.isNotEmpty() }) {
            "Enter a question and all four answer choices."
        }
        require(cleaned.correctAnswer in listOf("A", "B", "C", "D")) {
            "Choose the correct answer: A, B, C, or D."
        }
        return cleaned
    }

    private fun validatePassword(password: String) {
        require(password.length in 8..128) { "Password must have 8–128 characters." }
    }

    private fun userValues(
        username: String, displayName: String, record: PasswordHasher.PasswordRecord, role: String
    ) = ContentValues().apply {
        put("username", username)
        put("display_name", displayName)
        put("password_salt", record.salt)
        put("password_hash", record.hash)
        put("password_iterations", record.iterations)
        put("role", role)
    }

    private fun questionValues(question: Question, includeId: Boolean) = ContentValues().apply {
        if (includeId) put("id", question.id)
        put("question", question.text)
        put("option_a", question.optionA)
        put("option_b", question.optionB)
        put("option_c", question.optionC)
        put("option_d", question.optionD)
        put("correct_answer", question.correctAnswer)
    }

    private fun putMetadata(db: SQLiteDatabase, name: String, value: String) {
        db.insertOrThrow("app_metadata", null, ContentValues().apply {
            put("name", name)
            put("value", value)
        })
    }

    private fun Cursor.string(column: String): String = getString(getColumnIndexOrThrow(column))
    private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
    private fun Cursor.double(column: String): Double = getDouble(getColumnIndexOrThrow(column))

    private fun Cursor.toUser(): QuizUser {
        val photoIndex = getColumnIndexOrThrow("photo_file")
        return QuizUser(
            long("id"), string("username"), string("display_name"),
            if (isNull(photoIndex)) null else getString(photoIndex), string("role") == "admin"
        )
    }

    private fun Cursor.toQuestion() = Question(
        long("id"), string("question"), string("option_a"), string("option_b"),
        string("option_c"), string("option_d"), string("correct_answer")
    )
}
