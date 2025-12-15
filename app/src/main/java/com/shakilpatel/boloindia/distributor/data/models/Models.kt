package com.shakilpatel.boloindia.distributor.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== AUTH MODELS ====================

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val fullName: String,
    val phone: String,
    val password: String,
    val role: String = "DISTRIBUTOR",
    val address: String,
    val city: String,
    val state: String,
    val pincode: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val accessToken: String,
    val refreshToken: String,
    val user: User
)

@Serializable
data class RegisterResponse(
    val message : String,
    val email :String,
    val userId: Long
)
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val success: Boolean,
    val message: String,
    val data : RefreshToken
)

@Serializable
data class RefreshToken(
    val accessToken: String
)

@Serializable
data class User(
    val id: Long,
    val email: String,
    val fullName: String,
    val phone: String? = null,
    val roles: List<String> = emptyList(),
    val profileUrl: String? = null,
    val status: String? = null
) {
    val role: String
        get() = roles.firstOrNull() ?: "DISTRIBUTOR"
}

// ==================== QR ASSIGNMENT MODELS ====================

@Serializable
data class QRAssignment(
    val qrCodeId: Long,
    val qrCodeSlug: String,
    val qrCodeUrl: String,
    val qrCodeName: String,
    val paymentAmount: Double,
    @SerialName("assignmentStatus") val status: String,
    val isDeployed: Boolean,
    @SerialName("status") val qrStatus: String,
    val assignedAt: String,
    val acceptedAt: String? = null,
    val deployedAt: String? = null
)

@Serializable
data class QRAssignmentDetail(
    val qrCodeId: Long,
    val qrCodeSlug: String,
    val qrCodeUrl: String,
    val qrCodeName: String,
    val paymentAmount: Double,
    @SerialName("assignmentStatus") val status: String,
    val isDeployed: Boolean,
    @SerialName("status") val qrStatus: String,
    val assignedAt: String,
    val acceptedAt: String? = null,
    val deployedAt: String? = null
)

@Serializable
data class AssignmentsResponse(
    val success: Boolean,
    val message: String? = null,
    val data: AssignmentsData
)

@Serializable
data class AssignmentsData(
    val assignments: List<QRAssignment>,
    val total: Int
)

// ✅ FIXED: Accept Assignment Response
@Serializable
data class AcceptAssignmentResponse(
    val success: Boolean,
    val message: String,
    val data: AcceptAssignmentData
)

@Serializable
data class AcceptAssignmentData(
    val qrCodeId: Long,
    val qrCodeSlug: String,
    @SerialName("assignmentStatus")
    val status: String,
    val acceptedAt: String? = null
)

// ✅ FIXED: QR Details Response (from GET /qr/{id})
@Serializable
data class QRDetailsResponse(
    val success: Boolean,
    val message: String,
    val data: QRDetail
)

@Serializable
data class QRDetail(
    val qrCodeId: Long,
    val qrCodeSlug: String,
    val qrCodeUrl: String,
    val qrCodeName: String,
    val formId: String,
    val serviceId: Long,
    val orgId: Long,
    val createdBy: Long,
    val paymentAmount: Double,
    @SerialName("assignmentStatus") val assignmentStatus: String,
    val isDeployed: Boolean,
    @SerialName("status") val status: String,
    val totalScans: Int,
    val totalResponses: Int,
    val createdAt: String,
    val updatedAt: String,
    val assignedTo: Long? = null,
    val assignedAt: String? = null,
    val lastScannedAt: String? = null
)

// ==================== DEPLOYMENT MODELS ====================

@Serializable
data class DeploymentRequest(
    val locationName: String,
    val locationAddress: String,
    val latitude: Double,
    val longitude: Double,
    val deploymentPhotoUrl: String,
    val notes: String? = null
)

@Serializable
data class PhotoUploadResponse(
    val message:String = "",
    val success: Boolean = false,
    val photoUrl: String
)

@Serializable
data class DeploymentResponse(
    val success: Boolean = false,
    val message: String = "",
    val qrCodeId: Long,
    val status: Boolean = false
)

// ==================== CHAT MODELS ====================

@Serializable
data class Message(
    val messageId: Long,
    val senderId: Long,
    val senderName: String,
    val senderRole: String,
    val message: String,
    val sentAt: String,
    val isRead: Boolean
)

// ✅ FIXED: Messages Response
@Serializable
data class MessagesResponse(
    val success: Boolean,
    val message: String? = null,
    val data: MessagesData
)

@Serializable
data class MessagesData(
    val messages: List<Message>,
    val total: Int
)

@Serializable
data class SendMessageRequest(
    val message: String
)

@Serializable
data class UnreadCountResponse(
    val success: Boolean,
    val unreadCount: Int
)

// ==================== ANALYTICS MODELS ====================

@Serializable
data class QRAnalytics(
    val totalScans: Int,
    val totalResponses: Int,
    val scansByDay: List<DailyScan>,
    val deviceBreakdown: Map<String, Int>
)

@Serializable
data class DailyScan(
    val date: String,
    val scans: Int
)

// ==================== GENERIC API RESPONSE ====================

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)
