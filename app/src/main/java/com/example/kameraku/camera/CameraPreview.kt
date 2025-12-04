package com.example.kameraku.camera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kameraku.data.PhotoRepository
import com.example.kameraku.ui.PhotoGalleryActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CameraScreen() {
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    val permissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            // Android 13+ (API 33+)
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            // Android 10-12 (API 29-32)
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else -> {
            // Android 9 and below (API 28-)
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        cameraPermissionGranted = permissionsMap[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    if (!cameraPermissionGranted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Izin Kamera Diperlukan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Aplikasi memerlukan izin kamera\nuntuk mengambil foto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Button(onClick = { launcher.launch(permissions) }) {
                    Text("Berikan Izin")
                }
            }
        }
        return
    }

    CameraPreviewScreen()
}

@Composable
fun CameraPreviewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var latestPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }

    // Orientation listener for auto-rotation
    val orientationEventListener = remember {
        object : android.view.OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val rotation = when (orientation) {
                    in 45..134 -> android.view.Surface.ROTATION_270
                    in 135..224 -> android.view.Surface.ROTATION_180
                    in 225..314 -> android.view.Surface.ROTATION_90
                    else -> android.view.Surface.ROTATION_0
                }

                imageCapture?.targetRotation = rotation
            }
        }
    }

    DisposableEffect(Unit) {
        orientationEventListener.enable()
        onDispose {
            orientationEventListener.disable()
        }
    }

    // Load latest photo
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                latestPhotoUri = PhotoRepository(context.contentResolver).getLatestPhoto()?.uri
            } catch (_: Exception) {
                android.util.Log.e("CameraPreview", "Error loading photo")
            }
        }
    }

    // Initialize camera when selector changes
    LaunchedEffect(cameraSelector, previewView) {
        previewView?.let { view ->
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()

                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(view.surfaceProvider) }

                val newImageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashMode)
                    .setTargetRotation(view.display.rotation)
                    .build()

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    newImageCapture
                )
                imageCapture = newImageCapture
                isCameraReady = true

                // Update rotation when device orientation changes
                newImageCapture.targetRotation = view.display.rotation

                android.util.Log.d("CameraPreview", "Camera ready with flash mode: $flashMode, rotation: ${view.display.rotation}")

            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "Camera init failed", e)
                isCameraReady = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Update flash mode when changed
    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
        android.util.Log.d("CameraPreview", "Flash mode updated to: $flashMode")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    previewView = this

                    // Tap-to-focus
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                val factory = (v as PreviewView).meteringPointFactory
                                val point = factory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(point).build()

                                camera?.cameraControl?.startFocusAndMetering(action)
                                Toast.makeText(ctx, "Fokus...", Toast.LENGTH_SHORT).show()
                                true
                            }
                            else -> false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top controls - HANYA Flash (menggabungkan flash + torch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            // Flash toggle (cycle: OFF -> ON -> AUTO -> OFF)
            IconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                    imageCapture?.flashMode = flashMode
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    },
                    contentDescription = "Flash Mode",
                    tint = Color.White
                )
            }
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            context.startActivity(Intent(context, PhotoGalleryActivity::class.java))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuka galeri", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (latestPhotoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(latestPhotoUri)
                            .crossfade(true)
                            .size(200)
                            .build(),
                        contentDescription = "Latest photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Capture button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable(enabled = !isCapturing && isCameraReady) {
                        if (!isCameraReady) {
                            Toast.makeText(context, "Tunggu kamera siap...", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }

                        isCapturing = true
                        imageCapture?.let { ic ->
                            CameraController.takePhoto(
                                context = context,
                                imageCapture = ic,
                                onSuccess = { uri ->
                                    latestPhotoUri = uri
                                    Toast.makeText(context, "Foto disimpan!", Toast.LENGTH_SHORT).show()
                                    isCapturing = false
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                    isCapturing = false
                                }
                            )
                        } ?: run {
                            Toast.makeText(context, "Kamera belum siap", Toast.LENGTH_SHORT).show()
                            isCapturing = false
                        }
                    }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            when {
                                !isCameraReady -> Color.Gray
                                isCapturing -> Color.Gray
                                else -> Color.White
                            }
                        )
                )
            }

            // Switch camera button
            IconButton(
                onClick = {
                    isCameraReady = false
                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    Toast.makeText(
                        context,
                        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "Kamera Belakang" else "Kamera Depan",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Suppress("unused")
@Composable
private fun ErrorScreen(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                "Error Kamera",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
