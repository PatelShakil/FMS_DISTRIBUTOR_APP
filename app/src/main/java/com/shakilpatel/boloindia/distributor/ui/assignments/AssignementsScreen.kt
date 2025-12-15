package com.shakilpatel.boloindia.distributor.ui.assignments

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shakilpatel.boloindia.distributor.data.models.QRAssignment
import com.shakilpatel.boloindia.distributor.utils.toRelativeTime
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    onNavigateToQRCustomize: (Long) -> Unit,
    onNavigateToChat: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AssignmentsViewModel = hiltViewModel()
) {
    val pendingAssignments by viewModel.pendingAssignments.collectAsState()
    val acceptedAssignments by viewModel.acceptedAssignments.collectAsState()
    val deployedAssignments by viewModel.deployedAssignments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showRejectDialog by viewModel.showRejectDialog.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pending", "Accepted", "Deployed")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Assignments") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs with counts
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    val count = when (title) {
                        "Pending" -> pendingAssignments.size
                        "Accepted" -> acceptedAssignments.size
                        "Deployed" -> deployedAssignments.size
                        else -> 0
                    }

                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(title)
                                if (count > 0) {
                                    Badge(
                                        containerColor = if (selectedTab == index)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.secondary
                                    ) {
                                        Text(count.toString())
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Loading assignments...")
                        }
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error ?: "Error loading assignments",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.refresh() }) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    val currentAssignments = when (tabs[selectedTab]) {
                        "Pending" -> pendingAssignments
                        "Accepted" -> acceptedAssignments
                        "Deployed" -> deployedAssignments
                        else -> emptyList()
                    }

                    if (currentAssignments.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Assignment,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "No ${tabs[selectedTab].lowercase()} assignments",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentAssignments, key = { it.qrCodeId }) { assignment ->
                                AssignmentDetailCard(
                                    assignment = assignment,
                                    onAccept = { viewModel.acceptAssignment(it) },
                                    onReject = { viewModel.showRejectDialog(it) },
                                    onCustomizeQR = onNavigateToQRCustomize,
                                    onChat = onNavigateToChat
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Reject Dialog
    showRejectDialog?.let { qrCodeId ->
        RejectDialog(
            onDismiss = { viewModel.dismissRejectDialog() },
            onConfirm = { reason ->
                viewModel.rejectAssignment(qrCodeId, reason)
            }
        )
    }

    // Show error snackbar
    LaunchedEffect(error) {
        error?.let {
            // You can add SnackbarHost here if needed
            Log.e("AssignmentsScreen", "Error: $it")
        }
    }
}

@Composable
fun AssignmentDetailCard(
    assignment: QRAssignment,
    onAccept: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onCustomizeQR: (Long) -> Unit,
    onChat: (Long) -> Unit
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                }
                AssignmentStatusChip(status = assignment.status)
            }

            // Payment Amount
            if (assignment.paymentAmount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormatter.format(assignment.paymentAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            // Dates
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Assigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = assignment.assignedAt.toRelativeTime(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                assignment.acceptedAt?.let { acceptedAt ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Accepted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = acceptedAt.toRelativeTime(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                assignment.deployedAt?.let { deployedAt ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Deployed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = deployedAt.toRelativeTime(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Action Buttons based on status
            when (assignment.status) {
                "PENDING" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onReject(assignment.qrCodeId) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject")
                        }
                        Button(
                            onClick = { onAccept(assignment.qrCodeId) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Accept")
                        }
                    }
                }
                "ACCEPTED" -> {
                    if (!assignment.isDeployed) {
                        Button(
                            onClick = { onCustomizeQR(assignment.qrCodeId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Customize & Deploy QR Code")
                        }
                    }
                }
            }

            // Chat button (always available)
            OutlinedButton(
                onClick = { onChat(assignment.qrCodeId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Chat, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Chat with Admin")
            }
        }
    }
}

@Composable
fun AssignmentStatusChip(status: String) {
    val (color, label) = when (status) {
        "PENDING" -> MaterialTheme.colorScheme.tertiary to "Pending"
        "ACCEPTED" -> MaterialTheme.colorScheme.primary to "Accepted"
        "DEPLOYED" -> MaterialTheme.colorScheme.secondary to "Deployed"
        "APPROVED" -> MaterialTheme.colorScheme.primary to "Approved"
        "REJECTED" -> MaterialTheme.colorScheme.error to "Rejected"
        else -> MaterialTheme.colorScheme.onSurfaceVariant to status
    }

    AssistChip(
        onClick = { },
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

@Composable
fun RejectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Assignment") },
        text = {
            Column {
                Text("Please provide a reason for rejection:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    placeholder = { Text("e.g., Location too far, Already assigned elsewhere...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
