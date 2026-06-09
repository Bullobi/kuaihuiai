package io.github.xororz.localdream.data

enum class DarkModePreference {
    SYSTEM,
    LIGHT,
    DARK
}

data class ThemeState(
    val darkModePreference: DarkModePreference = DarkModePreference.SYSTEM,
    val dynamicColors: Boolean = true
)
