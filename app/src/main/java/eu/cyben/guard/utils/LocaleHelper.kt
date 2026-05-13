package eu.cyben.guard.utils

import java.util.Locale

object LocaleHelper {
    val deviceLanguage: String
        get() = when (Locale.getDefault().language) {
            "it" -> "it"
            "fr" -> "fr"
            "de" -> "de"
            "es" -> "es"
            else -> "en"
        }

    val ttsLocale: Locale
        get() = when (deviceLanguage) {
            "it" -> Locale.ITALIAN
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "es" -> Locale("es", "ES")
            else -> Locale.ENGLISH
        }
}
