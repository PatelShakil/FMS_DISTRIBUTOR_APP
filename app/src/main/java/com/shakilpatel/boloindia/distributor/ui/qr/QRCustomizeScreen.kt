package com.shakilpatel.boloindia.distributor.ui.qr

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.style.QrShape
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.*
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.shakilpatel.boloindia.distributor.BuildConfig
import com.shakilpatel.boloindia.distributor.data.models.QRAssignmentDetail
import com.shakilpatel.boloindia.distributor.data.repository.Repository
import com.shakilpatel.boloindia.distributor.utils.shareImage
import com.shakilpatel.boloindia.distributor.utils.showToast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

// ViewModel
@HiltViewModel
class QRCustomizeViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _qrDetails = MutableStateFlow<QRAssignmentDetail?>(null)
    val qrDetails = _qrDetails.asStateFlow()

    private val _qrUrl = MutableStateFlow("")
    val qrUrl = _qrUrl.asStateFlow()

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap = _qrBitmap.asStateFlow()

    // QR Customization States
    var pixelColor by mutableStateOf(Color.Black)
    var ballColor by mutableStateOf(Color.Black)
    var frameColor by mutableStateOf(Color.Black)
    var backgroundColor by mutableStateOf(Color.White)
    var qrShape by mutableStateOf<QrShape>(QrShape.Default)
    var logoUrl by mutableStateOf<String?>(null)

    fun loadQRDetails(qrCodeId: Long) {
        viewModelScope.launch {
            repository.getQRDetails(qrCodeId)
                .onSuccess { response ->
                    _qrDetails.value = response.data
                    _qrUrl.value = "${BuildConfig.WEB_URL}fill/${response.data?.qrCodeSlug}?qr=$qrCodeId"
                    logoUrl = response.data?.qrCodeUrl ?: ""
                    generateQR()
                }
        }
    }

    fun generateQR() {
        viewModelScope.launch {
            val drawable = QrCodeDrawable(
                QrData.Url(_qrUrl.value),
                createQrVectorOptions {
                    colors {
                        dark = QrVectorColor.Solid(pixelColor.toArgb())
                        ball = QrVectorColor.Solid(ballColor.toArgb())
                        frame = QrVectorColor.Solid(frameColor.toArgb())
//                        background = Modifier.background(QrVectorColor.Solid(backgroundColor.toArgb()))
                        // Background is set separately
                        background {
                            color = QrVectorColor.Solid(backgroundColor.toArgb())
                        }
                    }
                    shapes {
                        darkPixel = QrVectorPixelShape.RoundCorners(.25f)
                        ball = QrVectorBallShape.RoundCorners(.25f)
                        frame = QrVectorFrameShape.RoundCorners(.25f)
                    }
                    codeShape = qrShape
                }
            )

            _qrBitmap.value = drawable.toBitmap(1024, 1024)
        }
    }

    fun saveQRCode(context: android.content.Context): File? {
        return try {
            val bitmap = _qrBitmap.value ?: return null
            val file = File(context.cacheDir, "qr_code_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            null
        }
    }
}

// UI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCustomizeScreen(
    qrCodeId: Long,
    onNavigateToDeployment: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: QRCustomizeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val qrDetails by viewModel.qrDetails.collectAsState()
    val qrBitmap by viewModel.qrBitmap.collectAsState()

    var showColorPicker by remember { mutableStateOf<ColorPickerTarget?>(null) }

    LaunchedEffect(qrCodeId) {
        viewModel.loadQRDetails(qrCodeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize QR Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val file = viewModel.saveQRCode(context)
                            if (file != null) {
                                context.shareImage(
                                    file,
//                                    "Scan this QR code for ${qrDetails?.formData?.formName}"
                                    "Scan this QR code for "
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }
                    Button(
                        onClick = { onNavigateToDeployment(qrCodeId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NavigateNext, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Deploy")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // QR Code Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(viewModel.backgroundColor)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    qrBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            qrDetails?.let { details ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "details.formData.formName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Form ID: details.formData.formId",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Customization Options
            Text(
                text = "Customization",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            // Color Options
            ColorOption(
                label = "Pixel Color",
                color = viewModel.pixelColor,
                onClick = { showColorPicker = ColorPickerTarget.PIXEL }
            )

            ColorOption(
                label = "Ball Color",
                color = viewModel.ballColor,
                onClick = { showColorPicker = ColorPickerTarget.BALL }
            )

            ColorOption(
                label = "Frame Color",
                color = viewModel.frameColor,
                onClick = { showColorPicker = ColorPickerTarget.FRAME }
            )

            ColorOption(
                label = "Background Color",
                color = viewModel.backgroundColor,
                onClick = { showColorPicker = ColorPickerTarget.BACKGROUND }
            )

            // Shape Options
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "QR Shape",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = viewModel.qrShape == QrShape.Default,
                            onClick = {
                                viewModel.qrShape = QrShape.Default
                                viewModel.generateQR()
                            },
                            label = { Text("Square") }
                        )
                        FilterChip(
                            selected = viewModel.qrShape == QrShape.Circle(),
                            onClick = {
                                viewModel.qrShape = QrShape.Circle()
                                viewModel.generateQR()
                            },
                            label = { Text("Circle") }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.pixelColor = Color.Black
                    viewModel.ballColor = Color.Black
                    viewModel.frameColor = Color.Black
                    viewModel.backgroundColor = Color.White
                    viewModel.qrShape = QrShape.Default
                    viewModel.generateQR()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset to Default")
            }
        }
    }

    // Color Picker Dialog
    showColorPicker?.let { target ->
        ColorPickerDialog(
            initialColor = when (target) {
                ColorPickerTarget.PIXEL -> viewModel.pixelColor
                ColorPickerTarget.BALL -> viewModel.ballColor
                ColorPickerTarget.FRAME -> viewModel.frameColor
                ColorPickerTarget.BACKGROUND -> viewModel.backgroundColor
            },
            onDismiss = { showColorPicker = null },
            onColorSelected = { color ->
                when (target) {
                    ColorPickerTarget.PIXEL -> viewModel.pixelColor = color
                    ColorPickerTarget.BALL -> viewModel.ballColor = color
                    ColorPickerTarget.FRAME -> viewModel.frameColor = color
                    ColorPickerTarget.BACKGROUND -> viewModel.backgroundColor = color
                }
                viewModel.generateQR()
                showColorPicker = null
            }
        )
    }
}

@Composable
fun ColorOption(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color, shape = MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val controller = rememberColorPickerController()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a Color") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    controller = controller,
                    onColorChanged = { }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(controller.selectedColor.value)
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class ColorPickerTarget {
    PIXEL, BALL, FRAME, BACKGROUND
}
