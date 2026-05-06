package com.carolina.analizadorseguridadqr.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.carolina.analizadorseguridadqr.ui.theme.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val THEME_PREFERENCES_FILE = "theme_preferences"

private val Context.themePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = THEME_PREFERENCES_FILE,
)

class ThemePreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.themePreferencesDataStore

    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeMode.fromStorageValue(preferences[PreferencesKeys.THEME_MODE])
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
