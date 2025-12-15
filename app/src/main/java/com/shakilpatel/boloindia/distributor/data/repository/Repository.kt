package com.shakilpatel.boloindia.distributor.data.repository

import android.util.Log
import com.shakilpatel.boloindia.distributor.data.api.AuthApi
import com.shakilpatel.boloindia.distributor.data.api.QRApi
import com.shakilpatel.boloindia.distributor.data.local.PrefsManager
import com.shakilpatel.boloindia.distributor.data.models.*
import com.shakilpatel.boloindia.distributor.ui.chat.MarkReadResponse
import com.shakilpatel.boloindia.distributor.ui.chat.MessagesResponse
import com.shakilpatel.boloindia.distributor.ui.chat.SendMessageRequest
import com.shakilpatel.boloindia.distributor.ui.chat.SendMessageResponse
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    private val authApi: AuthApi,
    private val qrApi: QRApi,
    private val prefsManager: PrefsManager
) {

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            Log.d("REPOSITORY", "════════════════════════════════════════════")
            Log.d("REPOSITORY", "Login attempt started")
            Log.d("REPOSITORY", "Email: $email")
            Log.d("REPOSITORY", "════════════════════════════════════════════")

            val request = LoginRequest(email, password)
            val response = authApi.login(request)

            Log.d("REPOSITORY", "════════════════════════════════════════════")
            Log.d("REPOSITORY", "Login API call successful")
            Log.d("REPOSITORY", "Response: $response")
            Log.d("REPOSITORY", "Saving tokens...")
            Log.d("REPOSITORY", "════════════════════════════════════════════")

            // Save tokens
            prefsManager.saveTokens(response.accessToken, response.refreshToken)

            // Save user data
            prefsManager.saveUserData(
                id = response.user.id.toString(),
                email = response.user.email,
                name = response.user.fullName,
                role = response.user.role
            )

            Log.d("REPOSITORY", "Tokens and user data saved successfully")
            Log.d("REPOSITORY", "════════════════════════════════════════════")

            Result.success(response)

        } catch (e: Exception) {
            Log.e("REPOSITORY", "════════════════════════════════════════════")
            Log.e("REPOSITORY", "LOGIN FAILED IN REPOSITORY")
            Log.e("REPOSITORY", "Exception: ${e.javaClass.simpleName}")
            Log.e("REPOSITORY", "Message: ${e.message}")
            Log.e("REPOSITORY", "Cause: ${e.cause}")
            Log.e("REPOSITORY", "════════════════════════════════════════════")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun register(
        email: String,
        fullName: String,
        phone: String,
        password: String,
        address: String,
        city: String,
        state: String,
        pincode: String
    ): Result<ApiResponse<RegisterResponse>> {
        return try {
            val request = RegisterRequest(
                email = email,
                fullName = fullName,
                phone = phone,
                password = password,
                role = "DISTRIBUTOR",
                address = address,
                city = city,
                state = state,
                pincode = pincode
            )

            Log.d("REPOSITORY", "Register request: $request")
            val response = authApi.register(request)
            Result.success(response)

        } catch (e: Exception) {
            Log.e("REPOSITORY", "Register failed", e)
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            prefsManager.clearAll()
            Log.d("REPOSITORY", "Logout successful")
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Logout error", e)
        }
    }

    suspend fun getProfile(): Result<ApiResponse<User>> {
        return try {
            val response = authApi.getProfile()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Get profile error", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingAssignments(): Result<AssignmentsResponse> {
        return try {
            Log.d("REPOSITORY", "Calling QR API for pending assignments...")
            val response = qrApi.getMyAssignments() // ✅ Use getMyAssignments instead

            Log.d("REPOSITORY", "✅ Got ${response.data?.assignments?.size ?: 0} assignments")
            Log.d("REPOSITORY", "Total: ${response.data?.total ?: 0}")

            // Filter only PENDING assignments
            val pendingAssignments = response.data?.assignments?.filter {
                it.status == "PENDING"
            } ?: emptyList()

            val result = AssignmentsResponse(
                success = response.success,
                message = response.message,
                data = AssignmentsData(
                    assignments = pendingAssignments,
                    total = pendingAssignments.size
                )
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "❌ Get pending assignments error", e)
            Result.failure(e)
        }
    }

    suspend fun getMyAssignments(): Result<AssignmentsResponse> {
        return try {
            Log.d("REPOSITORY", "Getting all my assignments...")
            val response = qrApi.getMyAssignments()

            Log.d("REPOSITORY", "✅ Got ${response.data?.assignments?.size ?: 0} assignments")

            val result = AssignmentsResponse(
                success = response.success,
                message = response.message,
                data = response.data ?: AssignmentsData(emptyList(), 0)
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "❌ Get assignments error", e)
            Result.failure(e)
        }
    }

    suspend fun getQRDetails(qrCodeId: Long): Result<ApiResponse<QRAssignmentDetail>> {
        return try {
            val response = qrApi.getQRCodeById(qrCodeId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Get QR details error", e)
            Result.failure(e)
        }
    }

    suspend fun acceptAssignment(qrCodeId: Long): Result<AcceptAssignmentResponse> {
        return try {
            val response = qrApi.acceptAssignment(qrCodeId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Accept assignment error", e)
            Result.failure(e)
        }
    }

    suspend fun rejectAssignment(qrCodeId: Long, reason: String): Result<ApiResponse<Unit>> {
        return try {
            val response = qrApi.rejectAssignment(qrCodeId, reason)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Reject assignment error", e)
            Result.failure(e)
        }
    }

    suspend fun uploadPhotos(qrCodeId: Long, photo: File): Result<ApiResponse<PhotoUploadResponse>> {
        return try {
            val response = qrApi.uploadDeploymentPhotos(qrCodeId, photo)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Upload photos error", e)
            Result.failure(e)
        }
    }

    suspend fun submitDeployment(
        qrCodeId: Long,
        locationName: String,
        locationAddress: String,
        latitude: Double,
        longitude: Double,
        photoUrl: String,
        notes: String?
    ): Result<ApiResponse<DeploymentResponse>> {
        return try {
            val request = DeploymentRequest(
                locationName = locationName,
                locationAddress = locationAddress,
                latitude = latitude,
                longitude = longitude,
                deploymentPhotoUrl = photoUrl,
                notes = notes
            )
            val response = qrApi.submitDeployment(qrCodeId, request)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Submit deployment error", e)
            Result.failure(e)
        }
    }

    suspend fun getMessages(qrCodeId: Long): Result<MessagesResponse> {
        return try {
            val response = qrApi.getMessages(qrCodeId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Get messages error", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(qrCodeId: Long, message: SendMessageRequest): Result<SendMessageResponse> {
        return try {
            val response = qrApi.sendMessage(qrCodeId, message)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Send message error", e)
            Result.failure(e)
        }
    }

    suspend fun markMessageAsRead(messageId: Long): Result<MarkReadResponse> {
        return try {
            val response = qrApi.markMessageAsRead(messageId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Mark message as read error", e)
            Result.failure(e)
        }
    }
}
