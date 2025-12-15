package com.shakilpatel.boloindia.distributor.data.api

import android.util.Log
import com.shakilpatel.boloindia.distributor.data.local.PrefsManager
import com.shakilpatel.boloindia.distributor.data.models.*
import com.shakilpatel.boloindia.distributor.ui.chat.MarkReadResponse
import com.shakilpatel.boloindia.distributor.ui.chat.MessagesResponse
import com.shakilpatel.boloindia.distributor.ui.chat.SendMessageResponse
import com.shakilpatel.boloindia.distributor.utils.logRequest
import com.shakilpatel.boloindia.distributor.utils.logResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File
import javax.inject.Inject
import io.ktor.client.plugins.*
import io.ktor.http.*

class QRApi @Inject constructor(
    private val client: HttpClient,
    private val prefsManager: PrefsManager,
    private val tokenManager: TokenManager
) {

    private suspend fun <T> makeAuthenticatedRequest(
        block: suspend (String) -> T
    ): T {

        var token = prefsManager.getAccessToken()
            ?: throw Exception("Not authenticated. Please login again.")

        try {
            return block(token)

        } catch (e: ResponseException) {
            // Catches BOTH ClientRequestException & ServerResponseException
            val status = e.response.status
            Log.e("QR_API", "Status Code: $status")

            if (status == HttpStatusCode.Unauthorized ||
                status == HttpStatusCode.InternalServerError
            ) {
                return retryWithRefresh(block)
            }

            throw e
        }
    }


    private suspend fun <T> retryWithRefresh(
        block: suspend (String) -> T
    ): T {

        Log.d("QR_API", "Refreshing token...")

        val refreshed = tokenManager.refreshTokenIfNeeded()
        if (!refreshed) {
            throw Exception("Session expired. Please login again.")
        }

        val newToken = prefsManager.getAccessToken()
            ?: throw Exception("Token refresh failed")

        Log.d("QR_API", "Retrying request with refreshed token...")
        return block(newToken)
    }


    suspend fun getPendingAssignments(): AssignmentsResponse {
        Log.d("QR_API", "Getting pending assignments...")
        return makeAuthenticatedRequest { token ->
            val endpoint = "qr/assignments/pending"

            logRequest(
                tag = "QR_ASSIGNMENTS",
                url = endpoint,
                method = "GET",
                headers = mapOf("Authorization" to "Bearer $token")
            )

            val response: HttpResponse = client.get(endpoint) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            Log.d("QR_API", "Response: $response")

            response.logResponse("QR_ASSIGNMENTS")
            response.body()
        }
    }

    suspend fun acceptAssignment(qrCodeId: Long): AcceptAssignmentResponse {
        return makeAuthenticatedRequest { token ->
            val endpoint = "qr/$qrCodeId/accept"

            logRequest(
                tag = "QR_ACCEPT",
                url = endpoint,
                method = "POST",
                headers = mapOf("Authorization" to "Bearer $token")
            )

            val response: HttpResponse = client.post(endpoint) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.logResponse("QR_ACCEPT")
            response.body()
        }
    }


    // Get all my assignments
    // Get all my assignments
    suspend fun getMyAssignments(): ApiResponse<AssignmentsData> {
        return makeAuthenticatedRequest { token ->
            val endpoint = "qr/my-assignments"

            logRequest(
                tag = "QR_MY_ASSIGNMENTS",
                url = endpoint,
                method = "GET",
                headers = mapOf("Authorization" to "Bearer $token")
            )

            val response: HttpResponse = client.get(endpoint) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            response.logResponse("QR_MY_ASSIGNMENTS")
            response.body()
        }
    }


    // Get single QR code details
    suspend fun getQRCodeById(qrCodeId: Long): ApiResponse<QRAssignmentDetail> {
        return makeAuthenticatedRequest { token ->
            client.get("qr/$qrCodeId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        }
    }

    // Reject assignment
    suspend fun rejectAssignment(qrCodeId: Long, reason: String): ApiResponse<Unit> {
        return makeAuthenticatedRequest { token ->
            client.post("qr/$qrCodeId/reject") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(mapOf("reason" to reason))
            }.body()
        }
    }

    // Upload deployment photos
    suspend fun uploadDeploymentPhotos(
        qrCodeId: Long,
        photo: File
    ): ApiResponse<PhotoUploadResponse> {
        return makeAuthenticatedRequest { token ->
            client.submitFormWithBinaryData(
                url = "qr/$qrCodeId/upload-photo",
                formData = formData {
                        append("file", photo.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=${photo.name}")
                        })

                }
            ) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        }
    }

    // Submit deployment
    suspend fun submitDeployment(
        qrCodeId: Long,
        request: DeploymentRequest
    ): ApiResponse<DeploymentResponse> {
        return makeAuthenticatedRequest { token ->
            client.post("qr/$qrCodeId/deploy") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }.body()
        }
    }

    // Chat - Get messages
    suspend fun getMessages(qrCodeId: Long): MessagesResponse {
        return makeAuthenticatedRequest { token ->
            val res = client.get("qr/$qrCodeId/messages") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            res.logResponse("GET_MESSAGE")
            res.body()
        }
    }

    // Chat - Send message
    suspend fun sendMessage(qrCodeId: Long, message: com.shakilpatel.boloindia.distributor.ui.chat.SendMessageRequest): SendMessageResponse {
        return makeAuthenticatedRequest { token ->
            val res = client.post("qr/$qrCodeId/message") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(message)
            }

            res.logResponse("SEND_MESSAGE")
            res.body()
        }
    }

    // Chat - Mark message as read
    suspend fun markMessageAsRead(messageId: Long): MarkReadResponse {
        return makeAuthenticatedRequest { token ->
            val res = client.put("qr/message/$messageId/read") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            res.logResponse("READ_MESSAGE")
            res.body()
        }
    }

    // Chat - Get unread count
    suspend fun getUnreadCount(qrCodeId: Long): UnreadCountResponse {
        return makeAuthenticatedRequest { token ->
            client.get("qr/messages/unread-count") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("qrCodeId", qrCodeId)
            }.body()
        }
    }

    // Analytics
    suspend fun getQRAnalytics(qrCodeId: Long, days: Int = 7): ApiResponse<QRAnalytics> {
        return makeAuthenticatedRequest { token ->
            client.get("qr/$qrCodeId/analytics") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("days", days)
            }.body()
        }
    }
}
