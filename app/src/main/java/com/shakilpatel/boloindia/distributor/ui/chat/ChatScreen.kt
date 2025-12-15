package com.shakilpatel.boloindia.distributor.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.shakilpatel.boloindia.distributor.data.repository.Repository
import com.shakilpatel.boloindia.distributor.utils.toRelativeTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

// ============================================================================
// DATA MODELS - Aligned with Backend API Response
// ============================================================================

/**
 * Message model matching backend QRMessage entity
 * Backend fields: messageId, senderId, senderType, messageText, isRead, createdAt
 */
@Serializable
data class QRMessage(
    val messageId: Long,
    val senderId: Long,
    val senderType: String,      // "DISTRIBUTOR" or "ORGADMIN"
    val messageText: String,
    val isRead: Boolean,
    val createdAt: String
)

/**
 * Messages data wrapper
 */
@Serializable
data class MessagesData(
    val messages: List<QRMessage>,
    val total: Int
)

/**
 * Complete messages response from GET /api/qr/{qrCodeId}/messages
 */
@Serializable
data class MessagesResponse(
    val success: Boolean,
    val message: String,
    val data: MessagesData
)

/**
 * Request body for POST /api/qr/{qrCodeId}/message
 */
@Serializable
data class SendMessageRequest(
    val message: String
)

/**
 * Single message response from POST /api/qr/{qrCodeId}/message
 */
@Serializable
data class SendMessageData(
    val messageId: Long,
    val qrCodeId: Long,
    val senderId: Long,
    val senderType: String,
    val messageText: String,
    val createdAt: String
)

@Serializable
data class SendMessageResponse(
    val success: Boolean,
    val message: String,
    val data: SendMessageData
)

/**
 * Mark as read response from PUT /api/qr/message/{messageId}/read
 */
@Serializable
data class MarkReadResponse(
    val success: Boolean,
    val message: String
)

// ============================================================================
// VIEW MODEL
// ============================================================================

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<QRMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var currentQrCodeId: Long? = null
    private var pollingJob: kotlinx.coroutines.Job? = null

    fun startChat(qrCodeId: Long) {
        currentQrCodeId = qrCodeId
        loadMessages()
        startPolling()
    }

    fun stopChat() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopChat()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(30_000) // 30 seconds - professional polling interval
                currentQrCodeId?.let { loadMessages(silent = true) }
            }
        }
    }

    fun loadMessages(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _isLoading.value = true
            _error.value = null

            currentQrCodeId?.let { qrCodeId ->
                repository.getMessages(qrCodeId)
                    .onSuccess { response ->
                        _messages.value = response.data.messages

                        // Mark unread admin messages as read when viewed
                        response.data.messages
                            .filter { !it.isRead && it.senderType != "DISTRIBUTOR" }
                            .forEach { message ->
                                markMessageAsRead(message.messageId)
                            }
                    }
                    .onFailure { throwable ->
                        if (!silent) {
                            _error.value = throwable.message ?: "Failed to load messages"
                        }
                    }
            }

            if (!silent) _isLoading.value = false
        }
    }

    private fun markMessageAsRead(messageId: Long) {
        viewModelScope.launch {
            repository.markMessageAsRead(messageId)
                .onFailure {
                    // Silent failure - not critical for UX
                }
        }
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true
            _error.value = null

            currentQrCodeId?.let { qrCodeId ->
                repository.sendMessage(qrCodeId, SendMessageRequest(messageText))
                    .onSuccess {
                        // Immediately add sent message to UI for instant feedback
                        loadMessages(silent = true)
                    }
                    .onFailure { throwable ->
                        _error.value = throwable.message ?: "Failed to send message"
                    }
            }

            _isSending.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

// ============================================================================
// UI COMPOSABLES
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    qrCodeId: Long,
    qrCodeName: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(qrCodeId) {
        viewModel.startChat(qrCodeId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopChat()
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show error snackbar
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = qrCodeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Chat with Admin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadMessages() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        enabled = !isSending,
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        },
                        enabled = messageText.isNotBlank() && !isSending,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send"
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (messages.isEmpty()) {
                EmptyMessagesPlaceholder()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.messageId }
                    ) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessagesPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Start the conversation with admin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MessageBubble(message: QRMessage) {
    val isMe = message.senderType == "DISTRIBUTOR"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMe)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isMe) 0.dp else 1.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Show sender name for admin messages
                if (!isMe) {
                    Text(
                        text = "Admin", // Backend doesn't send senderName, using static
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.createdAt.toRelativeTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Read indicator for sent messages only
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead)
                                Icons.Default.DoneAll
                            else
                                Icons.Default.Done,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            modifier = Modifier.size(14.dp),
                            tint = if (message.isRead)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
