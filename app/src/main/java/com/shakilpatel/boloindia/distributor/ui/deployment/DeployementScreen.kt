package com.shakilpatel.boloindia.distributor.ui.deployment

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.shakilpatel.boloindia.distributor.data.repository.Repository
import com.shakilpatel.boloindia.distributor.utils.ImageUtils
import com.shakilpatel.boloindia.distributor.utils.LocationHelper
import com.shakilpatel.boloindia.distributor.utils.showToast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

// ViewModel
@HiltViewModel
class DeploymentViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _photos = MutableStateFlow<File?>(null)
    val photos = _photos.asStateFlow()

    private val _locationName = MutableStateFlow("")
    val locationName = _locationName.asStateFlow()

    private val _locationAddress = MutableStateFlow("")
    val locationAddress = _locationAddress.asStateFlow()

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude = _longitude.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun updateLocationName(value: String) {
        _locationName.value = value
    }

    fun updateLocationAddress(value: String) {
        _locationAddress.value = value
    }

    fun updateNotes(value: String) {
        _notes.value = value
    }

    fun setLocation(lat: Double, lng: Double) {
        _latitude.value = lat
        _longitude.value = lng
    }

    fun addPhoto(file: File) {
        _photos.value = file
    }

    fun removePhoto() {
        _photos.value = null
    }

    suspend fun getCurrentLocation(context: android.content.Context) {
        val location = LocationHelper.getCurrentLocation(context)
        if (location != null) {
            _latitude.value = location.latitude
            _longitude.value = location.longitude
        }
    }

    fun submitDeployment(qrCodeId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_photos.value == null) {
                _error.value = "Please upload at least 1 photo"
                return@launch
            }

            if (_latitude.value == null || _longitude.value == null) {
                _error.value = "Location is required"
                return@launch
            }

            _isSubmitting.value = true
            _error.value = null

            // Step 1: Upload photos
            repository.uploadPhotos(qrCodeId, _photos.value!!)
                .onSuccess { photoResponse ->
                    // Step 2: Submit deployment
                    repository.submitDeployment(
                        qrCodeId = qrCodeId,
                        locationName = _locationName.value,
                        locationAddress = _locationAddress.value,
                        latitude = _latitude.value!!,
                        longitude = _longitude.value!!,
                        photoUrl = photoResponse.data?.photoUrl ?: "",
                        notes = _notes.value.ifBlank { null }
                    )
                        .onSuccess {
                            _isSubmitting.value = false
                            onSuccess()
                        }
                        .onFailure {
                            _isSubmitting.value = false
                            _error.value = it.message
                        }
                }
                .onFailure {
                    _isSubmitting.value = false
                    _error.value = it.message
                }
        }
    }
}

// UI
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DeploymentScreen(
    qrCodeId: Long,
    onNavigateBack: () -> Unit,
    onDeploymentSuccess: () -> Unit,
    viewModel: DeploymentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val locationAddress by viewModel.locationAddress.collectAsState()
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val error by viewModel.error.collectAsState()

    var showCamera by remember { mutableStateOf(false) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.getCurrentLocation(context)
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deploy QR Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = {
                        viewModel.submitDeployment(qrCodeId) {
                            context.showToast("Deployment submitted successfully!")
                            onDeploymentSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = !isSubmitting && photos != null &&
                            locationName.isNotBlank() && latitude != null
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Submitting...")
                    } else {
                        Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Submit Deployment")
                    }
                }
            }
        }
    ) { padding ->
        if (!permissionsState.allPermissionsGranted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Camera and Location permissions are required",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                        Text("Grant Permissions")
                    }
                }
            }
            return@Scaffold
        }

        if (showCamera) {
            CameraView(
                onPhotoCaptured = { file ->
                    viewModel.addPhoto(file)
                    showCamera = false
                },
                onClose = { showCamera = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error Message
                error?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Photos Section
                Text(
                    text = "Deployment Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Take at least 1 photo showing QR code placement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (photos != null) {

                    PhotoThumbnail(
                        file = photos!!,
                        onRemove = { viewModel.removePhoto() }
                    )
                }

                if (photos == null) {
                    OutlinedButton(
                        onClick = { showCamera = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Take Photo")
                    }
                }

                HorizontalDivider()

                // Location Information
                Text(
                    text = "Location Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { viewModel.updateLocationName(it) },
                    label = { Text("Location Name *") },
                    placeholder = { Text("e.g., City Hall - Main Entrance") },
                    leadingIcon = { Icon(Icons.Default.Place, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = locationAddress,
                    onValueChange = { viewModel.updateLocationAddress(it) },
                    label = { Text("Location Address") },
                    placeholder = { Text("Full address") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // GPS Coordinates
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GPS Coordinates",
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (latitude != null && longitude != null) {
                                Text(
                                    text = "Lat: %.6f, Lng: %.6f".format(latitude, longitude),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "Fetching location...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    viewModel.getCurrentLocation(context)
                                }
                            }
                        ) {
                            Icon(Icons.Default.MyLocation, "Refresh Location")
                        }
                    }
                }

                HorizontalDivider()

                // Additional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Additional Notes (Optional)") },
                    placeholder = { Text("Any additional information about deployment") },
                    leadingIcon = { Icon(Icons.Default.Notes, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }
    }
}

@Composable
fun PhotoThumbnail(
    file: File,
    onRemove: () -> Unit
) {
    Box {
        Card(
            modifier = Modifier.size(120.dp)
        ) {
            val bitmap = remember {
                BitmapFactory.decodeFile(file.absolutePath)
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Photo",
                modifier = Modifier.fillMaxSize()
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.Cancel,
                "Remove",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun CameraView(
    onPhotoCaptured: (File) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(cameraProviderFuture) {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Camera Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    val outputFile = File(
                        context.cacheDir,
                        "photo_${System.currentTimeMillis()}.jpg"
                    )

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                    imageCapture?.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                // Add watermark and compress
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                                    .launch {
                                        val processedFile =
                                            ImageUtils.processImage(context, outputFile)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            onPhotoCaptured(processedFile)
                                        }
                                    }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Camera, "Capture", modifier = Modifier.size(32.dp))
            }

            OutlinedButton(onClick = onClose) {
                Text("Cancel")
            }
        }
    }
}
