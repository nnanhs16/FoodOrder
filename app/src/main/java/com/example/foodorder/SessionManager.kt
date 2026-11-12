
package com.example.foodorder

import android.content.Context

class SessionManager(context: Context) {

    private val preferences = context.getSharedPreferences(
        "FoodOrderSession",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_USER_ID = "CURRENT_USER_ID"
        private const val KEY_LOGIN = "IS_LOGGED_IN"
    }

    fun saveUser(userId: Int) {
        preferences.edit()
            .putInt(KEY_USER_ID, userId)
            .putBoolean(KEY_LOGIN, true)
            .apply()
    }

    fun getUserId(): Int {
        return preferences.getInt(KEY_USER_ID, -1)
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_LOGIN, false) &&
                getUserId() > 0
    }

    fun logout() {
        preferences.edit()
            .clear()
            .apply()
    }
}
