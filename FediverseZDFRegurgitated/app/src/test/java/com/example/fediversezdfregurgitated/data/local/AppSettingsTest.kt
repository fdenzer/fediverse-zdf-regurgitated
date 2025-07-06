package com.example.fediversezdfregurgitated.data.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppSettingsTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var appSettings: AppSettings

    @Before
    fun setUp() {
        mockContext = mockk()
        mockPrefs = mockk()
        mockEditor = mockk(relaxed = true) // relaxed = true to allow edit().putString().apply() chaining

        every { mockContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor // For chaining

        appSettings = AppSettings(mockContext)
    }

    @Test
    fun `currentTheme saves and retrieves correctly`() {
        // Default theme
        every { mockPrefs.getString(AppSettings.KEY_APP_THEME, null) } returns null
        assertEquals(AppTheme.SYSTEM_DEFAULT, appSettings.currentTheme)

        // Set and get Light theme
        appSettings.currentTheme = AppTheme.LIGHT
        verify { mockEditor.putString(AppSettings.KEY_APP_THEME, AppTheme.LIGHT.preferenceValue) }
        verify { mockEditor.apply() } // Verify apply is called

        every { mockPrefs.getString(AppSettings.KEY_APP_THEME, null) } returns AppTheme.LIGHT.preferenceValue
        assertEquals(AppTheme.LIGHT, appSettings.currentTheme)

        // Set and get Dark theme
        appSettings.currentTheme = AppTheme.DARK
        verify { mockEditor.putString(AppSettings.KEY_APP_THEME, AppTheme.DARK.preferenceValue) }
        every { mockPrefs.getString(AppSettings.KEY_APP_THEME, null) } returns AppTheme.DARK.preferenceValue
        assertEquals(AppTheme.DARK, appSettings.currentTheme)
    }

    @Test
    fun `fontSize saves and retrieves correctly`() {
        // Default font size
        every { mockPrefs.getString(AppSettings.KEY_FONT_SIZE, null) } returns null
        assertEquals(FontSize.MEDIUM, appSettings.fontSize)

        // Set and get Small font size
        appSettings.fontSize = FontSize.SMALL
        verify { mockEditor.putString(AppSettings.KEY_FONT_SIZE, FontSize.SMALL.preferenceValue) }
        every { mockPrefs.getString(AppSettings.KEY_FONT_SIZE, null) } returns FontSize.SMALL.preferenceValue
        assertEquals(FontSize.SMALL, appSettings.fontSize)

        // Set and get Large font size
        appSettings.fontSize = FontSize.LARGE
        verify { mockEditor.putString(AppSettings.KEY_FONT_SIZE, FontSize.LARGE.preferenceValue) }
        every { mockPrefs.getString(AppSettings.KEY_FONT_SIZE, null) } returns FontSize.LARGE.preferenceValue
        assertEquals(FontSize.LARGE, appSettings.fontSize)
    }

    @Test
    fun `isDarkThemeEnabled respects AppTheme settings`() {
        // System theme, system is dark
        every { mockPrefs.getString(AppSettings.KEY_APP_THEME, null) } returns AppTheme.SYSTEM_DEFAULT.preferenceValue
        assertTrue(appSettings.isDarkThemeEnabled(isSystemDark = true))

        // System theme, system is light
        assertFalse(appSettings.isDarkThemeEnabled(isSystemDark = false))

        // Light theme explicitly set
        every { mockPrefs.getString(AppSettings.KEY_APP_THEME, null) } returns AppTheme.LIGHT.preferenceValue
        assertFalse(appSettings.isDarkThemeEnabled(isSystemDark = true)) // System dark is ignored
        assertFalse(appSettings.isDarkThemeEnabled(isSystemDark = false))

        // Dark theme explicitly set
        every { mockPrefs.getString(AppSettings.KEY_APP_THEME, null) } returns AppTheme.DARK.preferenceValue
        assertTrue(appSettings.isDarkThemeEnabled(isSystemDark = true))
        assertTrue(appSettings.isDarkThemeEnabled(isSystemDark = false)) // System light is ignored
    }

    @Test
    fun `AppTheme fromPreferenceValue returns correct enum or default`() {
        assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromPreferenceValue("system"))
        assertEquals(AppTheme.LIGHT, AppTheme.fromPreferenceValue("light"))
        assertEquals(AppTheme.DARK, AppTheme.fromPreferenceValue("dark"))
        assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromPreferenceValue("unknown"))
        assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromPreferenceValue(null))
    }

    @Test
    fun `FontSize fromPreferenceValue returns correct enum or default`() {
        assertEquals(FontSize.SMALL, FontSize.fromPreferenceValue("small"))
        assertEquals(FontSize.MEDIUM, FontSize.fromPreferenceValue("medium"))
        assertEquals(FontSize.LARGE, FontSize.fromPreferenceValue("large"))
        assertEquals(FontSize.MEDIUM, FontSize.fromPreferenceValue("unknown"))
        assertEquals(FontSize.MEDIUM, FontSize.fromPreferenceValue(null))
    }
}
