package com.shakilpatel.boloindia.distributor.data.api

import android.util.Log
import com.shakilpatel.boloindia.distributor.data.local.PrefsManager
import com.shakilpatel.boloindia.distributor.data.models.RefreshTokenRequest
import com.shakilpatel.boloindia.distributor.data.models.RefreshTokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val prefsManager: PrefsManager,
    private val client: HttpClient
) {

    suspend fun refreshTokenIfNeeded(): Boolean {
        return try {
            val refreshToken = prefsManager.getRefreshToken() ?: return false

            Log.d("TOKEN_MANAGER", "Refreshing access token...")

            val response: HttpResponse = client.post("auth/refresh") {
                setBody(RefreshTokenRequest(refreshToken))
            }

            if (response.status == HttpStatusCode.OK) {
                val refreshResponse = response.body<RefreshTokenResponse>()

                // Save new tokens
                prefsManager.saveTokens(
                    refreshResponse.data.accessToken,
                    prefsManager.getRefreshToken() ?: ""
                )

                Log.d("TOKEN_MANAGER", "✅ Token refreshed successfully")
                true
            } else {
                Log.e("TOKEN_MANAGER", "❌ Token refresh failed: ${response.status}")
                prefsManager.clearAll()
                false
            }

        } catch (e: Exception) {
            Log.e("TOKEN_MANAGER", "Token refresh error", e)
            prefsManager.clearAll()
            false
        }
    }

    suspend fun getValidAccessToken(): String? {
        var token = prefsManager.getAccessToken()

        // Check if token is expired (you can decode JWT to check expiry)
        // For now, we'll just try to refresh if API call fails

        return token
    }
}
