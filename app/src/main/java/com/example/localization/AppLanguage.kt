package com.example.localization

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean
) {
    ARABIC("ar", "العربية", "🇸🇦", true),
    ENGLISH("en", "English", "🇬🇧", false),
    FRENCH("fr", "Français", "🇫🇷", false);

    val layoutDirection: LayoutDirection
        get() = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ARABIC
        }
    }
}
