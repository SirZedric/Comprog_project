package com.example.simplequiz

import android.content.Context

/** Store only the signed-in user's ID. Passwords are never saved in preferences. */
object Session {
    private const val PREFS = "quiz_session"
    private const val USER_ID = "user_id"

    fun getUserId(context: Context): Long? {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(USER_ID, -1L)
        return id.takeIf { it > 0L }
    }

    fun setUserId(context: Context, id: Long) {
        require(id > 0L)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(USER_ID, id).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
