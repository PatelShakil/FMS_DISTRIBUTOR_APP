package com.shakilpatel.boloindia.distributor.di

import android.util.Log
import com.shakilpatel.boloindia.distributor.BuildConfig
import com.shakilpatel.boloindia.distributor.data.api.AuthApi
import com.shakilpatel.boloindia.distributor.data.api.QRApi
import com.shakilpatel.boloindia.distributor.data.api.TokenManager
import com.shakilpatel.boloindia.distributor.data.local.PrefsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideHttpClient(
        json: Json,
        prefsManager: PrefsManager
    ): HttpClient {
        return HttpClient(Android) {
            // JSON Content Negotiation
            install(ContentNegotiation) {
                json(json)
            }

            // COMPREHENSIVE LOGGING
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.v("KTOR", message)
                    }
                }
                level = LogLevel.ALL
            }

            // REQUEST/RESPONSE OBSERVER
            install(ResponseObserver) {
                onResponse { response ->
                    Log.d("HTTP_RESPONSE", "╔════════════════════════════════════════════════════════")
                    Log.d("HTTP_RESPONSE", "║ URL: ${response.request.url}")
                    Log.d("HTTP_RESPONSE", "║ METHOD: ${response.request.method.value}")
                    Log.d("HTTP_RESPONSE", "║ STATUS: ${response.status.value} ${response.status.description}")
                    Log.d("HTTP_RESPONSE", "║ Headers:")
                    response.headers.forEach { key, values ->
                        Log.d("HTTP_RESPONSE", "║   $key: ${values.joinToString()}")
                    }
                    Log.d("HTTP_RESPONSE", "╚════════════════════════════════════════════════════════")
                }
            }

            // Timeout Configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }

            // Default Request Configuration with logging
            defaultRequest {
                url(BuildConfig.BASE_URL)
                contentType(ContentType.Application.Json)

                // Log every request
                Log.d("HTTP_REQUEST", "╔════════════════════════════════════════════════════════")
                Log.d("HTTP_REQUEST", "║ BASE URL: ${BuildConfig.BASE_URL}")
                Log.d("HTTP_REQUEST", "║ ENDPOINT: ${url.build()}")
                Log.d("HTTP_REQUEST", "║ METHOD: ${attributes.allKeys}")
                Log.d("HTTP_REQUEST", "╚════════════════════════════════════════════════════════")
            }

            // Global error handling with logging
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    Log.e("HTTP_ERROR", "╔════════════════════════════════════════════════════════")
                    Log.e("HTTP_ERROR", "║ EXCEPTION OCCURRED")
                    Log.e("HTTP_ERROR", "║ Type: ${exception.javaClass.simpleName}")
                    Log.e("HTTP_ERROR", "║ Message: ${exception.message}")
                    Log.e("HTTP_ERROR", "║ URL: ${request.url}")
                    Log.e("HTTP_ERROR", "║ Method: ${request.method.value}")
                    Log.e("HTTP_ERROR", "╚════════════════════════════════════════════════════════")
                    exception.printStackTrace()
                }
            }

            expectSuccess = true
        }
    }

    @Provides
    @Singleton
    fun provideAuthApi(client: HttpClient, prefsManager: PrefsManager): AuthApi {
        return AuthApi(client, prefsManager)
    }

    @Provides
    @Singleton
    fun provideTokenManager(client: HttpClient, prefsManager: PrefsManager): TokenManager {
        return TokenManager(prefsManager, client)
    }

    @Provides
    @Singleton
    fun provideQRApi(
        client: HttpClient,
        prefsManager: PrefsManager,
        tokenManager: TokenManager
    ): QRApi {
        return QRApi(client, prefsManager, tokenManager)
    }
}
