package com.shakilpatel.boloindia.distributor.ui.assignments

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shakilpatel.boloindia.distributor.data.models.QRAssignment
import com.shakilpatel.boloindia.distributor.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssignmentsViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    // Group assignments by status
    private val _pendingAssignments = MutableStateFlow<List<QRAssignment>>(emptyList())
    val pendingAssignments = _pendingAssignments.asStateFlow()

    private val _acceptedAssignments = MutableStateFlow<List<QRAssignment>>(emptyList())
    val acceptedAssignments = _acceptedAssignments.asStateFlow()

    private val _deployedAssignments = MutableStateFlow<List<QRAssignment>>(emptyList())
    val deployedAssignments = _deployedAssignments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _showRejectDialog = MutableStateFlow<Long?>(null)
    val showRejectDialog = _showRejectDialog.asStateFlow()

    init {
        loadAssignments()
    }

    fun loadAssignments() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d("ASSIGNMENTS_VM", "════════════════════════════════════════════")
            Log.d("ASSIGNMENTS_VM", "Loading assignments...")
            Log.d("ASSIGNMENTS_VM", "════════════════════════════════════════════")

            try {
                repository.getMyAssignments()
                    .onSuccess { response ->
                        val allAssignments = response.data.assignments

                        Log.d("ASSIGNMENTS_VM", "✅ Loaded ${allAssignments.size} assignments")

                        // Group by status
                        val pending = allAssignments.filter { it.status == "PENDING" }
                        val accepted = allAssignments.filter { it.status == "ACCEPTED" && !it.isDeployed }
                        val deployed = allAssignments.filter { it.isDeployed }

                        _pendingAssignments.value = pending
                        _acceptedAssignments.value = accepted
                        _deployedAssignments.value = deployed

                        Log.d("ASSIGNMENTS_VM", "Pending: ${pending.size}")
                        Log.d("ASSIGNMENTS_VM", "Accepted: ${accepted.size}")
                        Log.d("ASSIGNMENTS_VM", "Deployed: ${deployed.size}")
                        Log.d("ASSIGNMENTS_VM", "✅ Assignments grouped successfully")
                    }
                    .onFailure { error ->
                        Log.e("ASSIGNMENTS_VM", "❌ Error loading assignments", error)
                        _error.value = error.message ?: "Failed to load assignments"
                    }

            } catch (e: Exception) {
                Log.e("ASSIGNMENTS_VM", "❌ Exception in loadAssignments", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
                Log.d("ASSIGNMENTS_VM", "════════════════════════════════════════════")
            }
        }
    }

    fun acceptAssignment(qrCodeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true

            Log.d("ASSIGNMENTS_VM", "Accepting assignment: $qrCodeId")

            repository.acceptAssignment(qrCodeId)
                .onSuccess { response ->
                    Log.d("ASSIGNMENTS_VM", "✅ Assignment accepted: ${response.message}")
                    loadAssignments() // Refresh
                }
                .onFailure { error ->
                    Log.e("ASSIGNMENTS_VM", "❌ Failed to accept assignment", error)
                    _error.value = error.message ?: "Failed to accept assignment"
                    _isLoading.value = false
                }
        }
    }

    fun showRejectDialog(qrCodeId: Long) {
        _showRejectDialog.value = qrCodeId
    }

    fun dismissRejectDialog() {
        _showRejectDialog.value = null
    }

    fun rejectAssignment(qrCodeId: Long, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true

            Log.d("ASSIGNMENTS_VM", "Rejecting assignment: $qrCodeId, Reason: $reason")

            repository.rejectAssignment(qrCodeId, reason)
                .onSuccess { response ->
                    Log.d("ASSIGNMENTS_VM", "✅ Assignment rejected")
                    _showRejectDialog.value = null
                    loadAssignments() // Refresh
                }
                .onFailure { error ->
                    Log.e("ASSIGNMENTS_VM", "❌ Failed to reject assignment", error)
                    _error.value = error.message ?: "Failed to reject assignment"
                    _isLoading.value = false
                }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun refresh() {
        loadAssignments()
    }
}