package com.shakilpatel.boloindia.distributor.data.api

import android.util.Log
import com.shakilpatel.boloindia.distributor.data.local.PrefsManager
import com.shakilpatel.boloindia.distributor.data.models.*
import com.shakilpatel.boloindia.distributor.utils.logRequest
import com.shakilpatel.boloindia.distributor.utils.logResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthApi @Inject constructor(
    private val client: HttpClient,
    private val prefsManager: PrefsManager
) {
    private val json = Json { prettyPrint = true }

    suspend fun login(request: LoginRequest): LoginResponse {
        return try {
            val endpoint = "auth/login"

            logRequest(
                tag = "AUTH_LOGIN",
                url = endpoint,
                method = "POST",
                body = json.encodeToString(request)
            )

            val response: HttpResponse = client.post(endpoint) {
                setBody(request)
            }

            response.logResponse("AUTH_LOGIN")

            if (response.status == HttpStatusCode.OK) {
                val loginResponse = response.body<LoginResponse>()
                Log.d("AUTH_LOGIN", "✅ Login successful - User: ${loginResponse.user.fullName}")
                loginResponse
            } else {
                val errorBody = response.bodyAsText()
                Log.e("AUTH_LOGIN", "❌ Login failed: ${response.status} - $errorBody")
                throw Exception("Login failed: ${response.status}")
            }

        } catch (e: Exception) {
            Log.e("AUTH_LOGIN", "❌ Exception during login", e)
            throw e
        }
    }

    suspend fun register(request: RegisterRequest): ApiResponse<RegisterResponse> {
        return try {
            val endpoint = "auth/register/distributor"

            logRequest(
                tag = "AUTH_REGISTER",
                url = endpoint,
                method = "POST",
                body = json.encodeToString(request)
            )

            val response: HttpResponse = client.post(endpoint) {
                setBody(request)
            }

            response.logResponse("AUTH_REGISTER")
            response.body<ApiResponse<RegisterResponse>>()

        } catch (e: Exception) {
            Log.e("AUTH_REGISTER", "❌ Register error", e)
            throw e
        }
    }

    suspend fun getProfile(): ApiResponse<User> {
        return try {
            val token = prefsManager.getAccessToken()
            val endpoint = "auth/profile"

            logRequest(
                tag = "AUTH_PROFILE",
                url = endpoint,
                method = "GET",
                headers = mapOf("Authorization" to "Bearer $token")
            )

            val response: HttpResponse = client.get(endpoint) {
                if (token != null) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            response.logResponse("AUTH_PROFILE")
            response.body()

        } catch (e: Exception) {
            Log.e("AUTH_PROFILE", "❌ Get profile error", e)
            throw e
        }
    }

    suspend fun updateProfile(
        fullName: String?,
        phone: String?,
        address: String?,
        city: String?,
        state: String?,
        pincode: String?
    ): ApiResponse<User> {
        val token = prefsManager.getAccessToken()
        val endpoint = "auth/profile"

        val requestBody = mapOf(
            "fullName" to fullName,
            "phone" to phone,
            "address" to address,
            "city" to city,
            "state" to state,
            "pincode" to pincode
        )

        logRequest(
            tag = "AUTH_UPDATE",
            url = endpoint,
            method = "PUT",
            headers = mapOf("Authorization" to "Bearer $token"),
            body = json.encodeToString(requestBody)
        )

        val response: HttpResponse = client.put(endpoint) {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(requestBody)
        }

        response.logResponse("AUTH_UPDATE")
        return response.body()
    }

    suspend fun refreshToken(refreshToken: String): RefreshTokenResponse {
        return try {
            val endpoint = "auth/refresh"

            logRequest(
                tag = "AUTH_REFRESH",
                url = endpoint,
                method = "POST",
                body = json.encodeToString(RefreshTokenRequest(refreshToken))
            )

            val response: HttpResponse = client.post(endpoint) {
                setBody(RefreshTokenRequest(refreshToken))
            }

            response.logResponse("AUTH_REFRESH")
            response.body<RefreshTokenResponse>()

        } catch (e: Exception) {
            Log.e("AUTH_REFRESH", "❌ Token refresh error", e)
            throw e
        }
    }
}
