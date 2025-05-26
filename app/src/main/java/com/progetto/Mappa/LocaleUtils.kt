package com.progetto.Mappa

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale

fun saveLanguageToPreferences(context: Context, langCode: String) {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    prefs.edit().putString("language", langCode).apply()
}

fun setLocaleAndRestart(context: Context, langCode: String) {
    val locale = Locale(langCode)
    Locale.setDefault(locale)

    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)

    val newContext = context.createConfigurationContext(config)

    val intent = Intent(newContext, (context as Activity)::class.java)
    context.finish()
    context.startActivity(intent)
}

