package com.brunos3d.wearosclaude.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.brunos3d.wearosclaude.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persistent user-tunable bits. We keep the surface area tiny — the watch is
 * a viewer; tuning the budgets lives on the backend.
 */
private val Context.dataStore by preferencesDataStore(name = "wearos-claude-settings")

private val KEY_BACKEND = stringPreferencesKey("backend_url")
private val KEY_TOKEN = stringPreferencesKey("auth_token")

class Settings(private val context: Context) {

    data class Snapshot(val backendUrl: String, val authToken: String?)

    val flow: Flow<Snapshot> = context.dataStore.data.map { prefs ->
        // BuildConfig defaults seed the very first launch. After the user
        // edits via Settings, DataStore wins.
        val defaultToken = BuildConfig.DEFAULT_AUTH_TOKEN.takeIf { it.isNotBlank() }
        Snapshot(
            backendUrl = prefs[KEY_BACKEND]?.takeIf { it.isNotBlank() }
                ?: BuildConfig.DEFAULT_BACKEND_URL,
            authToken = prefs[KEY_TOKEN]?.takeIf { it.isNotBlank() } ?: defaultToken,
        )
    }

    suspend fun snapshot(): Snapshot = flow.first()

    suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { it[KEY_BACKEND] = url.trim() }
    }

    suspend fun setAuthToken(token: String?) {
        context.dataStore.edit {
            if (token.isNullOrBlank()) it.remove(KEY_TOKEN) else it[KEY_TOKEN] = token.trim()
        }
    }
}
