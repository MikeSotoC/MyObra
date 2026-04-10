package com.uchi.myobra.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREF_NAME  = "myobra_prefs"
    private const val KEY_THEME  = "theme_mode"
    private const val THEME_LIGHT = 0
    private const val THEME_DARK  = 1

    fun init(context: Context) {
        val prefs = prefs(context)
        val mode  = prefs.getInt(KEY_THEME, THEME_LIGHT)
        applyMode(mode)
    }

    fun isDark(context: Context): Boolean =
        prefs(context).getInt(KEY_THEME, THEME_LIGHT) == THEME_DARK

    fun toggle(context: Context) {
        val next = if (isDark(context)) THEME_LIGHT else THEME_DARK
        prefs(context).edit().putInt(KEY_THEME, next).apply()
        applyMode(next)
    }

    private fun applyMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(
            if (mode == THEME_DARK) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
