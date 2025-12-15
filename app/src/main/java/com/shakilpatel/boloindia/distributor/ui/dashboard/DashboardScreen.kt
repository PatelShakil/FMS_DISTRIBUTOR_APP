package com.shakilpatel.boloindia.distributor.ui.dashboard

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shakilpatel.boloindia.distributor.data.models.QRAssignment
import com.shakilpatel.boloindia.distributor.data.repository.Repository
import com.shakilpatel.boloindia.distributor.utils.toRelativeTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
data class DashboardStats(
    val pendingAssignments: Int = 0,
    val acceptedAssignments: Int = 0,
    val completedDeployments: Int = 0,
    val totalEarnings: Double = 0.0,
    val totalQRCodes: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _stats = MutableStateFlow(DashboardStats())
    val stats = _stats.asStateFlow()

    private val _pendingAssignments = MutableStateFlow<List<QRAssignment>>(emptyList())
    val pendingAssignments = _pendingAssignments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d("DASHBOARD_VM", "════════════════════════════════════════════")
            Log.d("DASHBOARD_VM", "Loading dashboard data...")
            Log.d("DASHBOARD_VM", "════════════════════════════════════════════")

            try {
                // Load all assignments
                repository.getMyAssignments()
                    .onSuccess { response ->
                        val allAssignments = response.data.assignments

                        Log.d("DASHBOARD_VM", "✅ Loaded ${allAssignments.size} total assignments")

                        // Filter assignments by status
                        val pending = allAssignments.filter { it.status == "PENDING" }
                        val accepted = allAssignments.filter { it.status == "ACCEPTED" }
                        val deployed = allAssignments.filter { it.isDeployed }

                        // Calculate total earnings (accepted + deployed)
                        val earnings = allAssignments
                            .filter { it.status == "ACCEPTED" || it.isDeployed }
                            .sumOf { it.paymentAmount }

                        Log.d("DASHBOARD_VM", "Pending: ${pending.size}")
                        Log.d("DASHBOARD_VM", "Accepted: ${accepted.size}")
                        Log.d("DASHBOARD_VM", "Deployed: ${deployed.size}")
                        Log.d("DASHBOARD_VM", "Total Earnings: ₹$earnings")

                        // Update stats
                        _stats.value = DashboardStats(
                            pendingAssignments = pending.size,
                            acceptedAssignments = accepted.size,
                            completedDeployments = deployed.size,
                            totalEarnings = earnings,
                            totalQRCodes = allAssignments.size
                        )

                        // Update pending assignments list (for quick actions)
                        _pendingAssignments.value = pending.take(5) // Show max 5 recent

                        Log.d("DASHBOARD_VM", "✅ Dashboard data loaded successfully")
                    }
                    .onFailure { error ->
                        Log.e("DASHBOARD_VM", "❌ Error loading dashboard data", error)
                        _error.value = error.message ?: "Failed to load dashboard data"
                    }

            } catch (e: Exception) {
                Log.e("DASHBOARD_VM", "❌ Exception in loadDashboardData", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
                Log.d("DASHBOARD_VM", "════════════════════════════════════════════")
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}


// UI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPending: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val pendingAssignments by viewModel.pendingAssignments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, "Profile")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Welcome Back!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Active Distributor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Earnings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Earnings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = currencyFormatter.format(stats.totalEarnings),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Stats Cards Grid
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending",
                        value = stats.pendingAssignments.toString(),
                        icon = Icons.Default.PendingActions,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = onNavigateToPending
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Accepted",
                        value = stats.acceptedAssignments.toString(),
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onClick = onNavigateToAssignments
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Deployed",
                        value = stats.completedDeployments.toString(),
                        icon = Icons.Default.QrCode2,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = onNavigateToAssignments
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Total QRs",
                        value = stats.totalQRCodes.toString(),
                        icon = Icons.Default.GridView,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onNavigateToAssignments
                    )
                }

                // Pending Assignments Section
                if (pendingAssignments.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pending Assignments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (pendingAssignments.size > 3) {
                            TextButton(onClick = onNavigateToPending) {
                                Text("View All (${pendingAssignments.size})")
                            }
                        }
                    }

                    pendingAssignments.take(3).forEach { assignment ->
                        AssignmentCard(
                            assignment = assignment,
                            onClick = onNavigateToPending
                        )
                    }
                } else if (!isLoading) {
                    // Empty state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No Pending Assignments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You're all caught up!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Loading Overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Loading dashboard...")
                        }
                    }
                }
            }

            // Error Snackbar
            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AssignmentCard(
    assignment: QRAssignment,
    onClick: () -> Unit
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assignment.qrCodeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = assignment.qrCodeSlug,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = assignment.assignedAt.toRelativeTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (assignment.paymentAmount > 0) {
                        Text(
                            text = currencyFormatter.format(assignment.paymentAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
