package com.linkcore.emulator

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Keeps Bruma's language independent of the device language on every supported Android version. */
object BrumaLocale {
    /** DS firmware supports these seven languages; other UI languages use English. */
    @JvmStatic
    fun ndsLanguage(context: Context): Int {
        val tag = selectedTag(context)
        val locale = if (tag.isNotBlank()) Locale.forLanguageTag(tag)
            else android.content.res.Resources.getSystem().configuration.locales[0]
        return when (locale.language) {
            "ja" -> 0; "fr" -> 2; "de" -> 3; "it" -> 4; "es" -> 5; "zh" -> 6
            else -> 1
        }
    }
    private const val PREFS = "bruma_language"
    private const val KEY = "tag"

    @JvmStatic
    fun selectedTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = context.getSystemService(android.app.LocaleManager::class.java)
            return manager?.applicationLocales?.toLanguageTags().orEmpty()
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
    }

    @JvmStatic
    fun attach(base: Context): Context {
        val tag = selectedTag(base)
        if (tag.isBlank()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    @JvmStatic
    fun setLanguage(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(android.app.LocaleManager::class.java)
                ?.applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).commit()
            if (tag.isBlank()) Locale.setDefault(LocaleList.getDefault()[0])
            else Locale.setDefault(Locale.forLanguageTag(tag))
        }
    }
}
