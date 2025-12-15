package com.shakilpatel.boloindia.distributor.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "boloindia_prefs")

@Singleton
class PrefsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
    }

    // Flow-based accessors (for UI)
    val accessToken: Flow<String?> = dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = dataStore.data.map { it[REFRESH_TOKEN] }
    val userId: Flow<String?> = dataStore.data.map { it[USER_ID] }
    val userEmail: Flow<String?> = dataStore.data.map { it[USER_EMAIL] }
    val userName: Flow<String?> = dataStore.data.map { it[USER_NAME] }
    val userRole: Flow<String?> = dataStore.data.map { it[USER_ROLE] }

    // Direct suspend accessors (for API calls) - ✅ FIXED
    suspend fun getAccessToken(): String? {
        return try {
            val token = dataStore.data.map { it[ACCESS_TOKEN] }.first()
            Log.d("PrefsManager", "Got access token: ${token?.take(30)}...")
            token
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error getting access token", e)
            null
        }
    }

    suspend fun getRefreshToken(): String? {
        return try {
            dataStore.data.map { it[REFRESH_TOKEN] }.first()
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error getting refresh token", e)
            null
        }
    }

    suspend fun getUserId(): String? {
        return try {
            dataStore.data.map { it[USER_ID] }.first()
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error getting user id", e)
            null
        }
    }

    suspend fun getUserEmail(): String? {
        return try {
            dataStore.data.map { it[USER_EMAIL] }.first()
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error getting user email", e)
            null
        }
    }

    suspend fun getUserName(): String? {
        return try {
            dataStore.data.map { it[USER_NAME] }.first()
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error getting user name", e)
            null
        }
    }

    suspend fun getUserRole(): String? {
        return try {
            dataStore.data.map { it[USER_ROLE] }.first()
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error getting user role", e)
            null
        }
    }

    // Save tokens
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        try {
            Log.d("PrefsManager", "Saving tokens...")
            dataStore.edit { prefs ->
                prefs[ACCESS_TOKEN] = accessToken
                prefs[REFRESH_TOKEN] = refreshToken
            }
            Log.d("PrefsManager", "✅ Tokens saved successfully")
        } catch (e: Exception) {
            Log.e("PrefsManager", "❌ Error saving tokens", e)
            throw e
        }
    }

    // Save user data - ✅ FIXED parameter name
    suspend fun saveUserData(
        id: String,
        email: String,
        name: String,
        role: String
    ) {
        try {
            Log.d("PrefsManager", "Saving user data...")
            dataStore.edit { prefs ->
                prefs[USER_ID] = id
                prefs[USER_EMAIL] = email
                prefs[USER_NAME] = name
                prefs[USER_ROLE] = role
            }
            Log.d("PrefsManager", "✅ User data saved successfully")
        } catch (e: Exception) {
            Log.e("PrefsManager", "❌ Error saving user data", e)
            throw e
        }
    }

    // Clear all data (logout)
    suspend fun clearAll() {
        try {
            Log.d("PrefsManager", "Clearing all data...")
            dataStore.edit { it.clear() }
            Log.d("PrefsManager", "✅ All data cleared")
        } catch (e: Exception) {
            Log.e("PrefsManager", "❌ Error clearing data", e)
            throw e
        }
    }
}
