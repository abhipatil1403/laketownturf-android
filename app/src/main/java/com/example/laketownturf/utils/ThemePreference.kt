package com.example.laketownturf.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user's dark/light theme preference using SharedPreferences.
 * Dark mode is the default.
 */
object ThemePreference {
    private const val PREFS_NAME = "ltt_theme_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkMode.value = prefs?.getBoolean(KEY_DARK_MODE, true) ?: true
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs?.edit()?.putBoolean(KEY_DARK_MODE, enabled)?.apply()
    }
}
