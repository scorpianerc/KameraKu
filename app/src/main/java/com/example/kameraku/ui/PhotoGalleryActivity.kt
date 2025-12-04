package com.example.kameraku.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kameraku.data.PhotoRepository
import com.example.kameraku.model.PhotoItem
import com.example.kameraku.ui.theme.KameraKuTheme
import java.text.SimpleDateFormat
import java.util.*

class PhotoGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KameraKuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoGalleryScreen(
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(onBackClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoRepository = remember { PhotoRepository(context.contentResolver) }
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPhotos by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            android.util.Log.d("PhotoGallery", "Loading photos...")
            photos = photoRepository.getAllPhotos()
            android.util.Log.d("PhotoGallery", "Loaded ${photos.size} photos")
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isSelectionMode) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Galeri Foto (${photos.size})",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "${selectedPhotos.size} dipilih",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedPhotos = emptySet()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (isSelectionMode) "Cancel" else "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Select All button
                        IconButton(
                            onClick = {
                                selectedPhotos = if (selectedPhotos.size == photos.size) {
                                    emptySet()
                                } else {
                                    photos.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Delete button
                        IconButton(
                            onClick = {
                                if (selectedPhotos.isNotEmpty()) {
                                    showDeleteDialog = true
                                }
                            },
                            enabled = selectedPhotos.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = if (selectedPhotos.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else
                                    Color.Gray
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                android.util.Log.d("PhotoGallery", "Refresh button clicked")
                                refreshTrigger++
                            }
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSelectionMode)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text("Hapus Foto?")
                },
                text = {
                    Text(
                        "Apakah Anda yakin ingin menghapus ${selectedPhotos.size} foto? " +
                        "Tindakan ini tidak dapat dibatalkan."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Delete photos
                            selectedPhotos.forEach { photoId ->
                                try {
                                    val uri = android.content.ContentUris.withAppendedId(
                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        photoId
                                    )
                                    context.contentResolver.delete(uri, null, null)
                                    android.util.Log.d("PhotoGallery", "Deleted photo ID: $photoId")
                                } catch (e: Exception) {
                                    android.util.Log.e("PhotoGallery", "Error deleting photo", e)
                                }
                            }

                            showDeleteDialog = false
                            isSelectionMode = false
                            selectedPhotos = emptySet()
                            refreshTrigger++

                            android.widget.Toast.makeText(
                                context,
                                "${selectedPhotos.size} foto dihapus",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                photos.isEmpty() -> {
                    android.util.Log.d("PhotoGallery", "Empty state: no photos found")
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Belum Ada Foto",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ambil foto pertama Anda dengan\nmenekan tombol kamera",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Atau tekan tombol refresh di atas\nuntuk memuat ulang foto",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(photos) { photo ->
                            PhotoGridItem(
                                photo = photo,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedPhotos.contains(photo.id),
                                onClick = {
                                    if (isSelectionMode) {
                                        // Toggle selection
                                        selectedPhotos = if (selectedPhotos.contains(photo.id)) {
                                            selectedPhotos - photo.id
                                        } else {
                                            selectedPhotos + photo.id
                                        }
                                    } else {
                                        // Open photo in viewer
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(photo.uri, "image/*")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                onLongClick = {
                                    // Enter selection mode
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedPhotos = setOf(photo.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotoGridItem(
    photo: PhotoItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateString = remember(photo.dateTaken) { dateFormat.format(Date(photo.dateTaken)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 4.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    .crossfade(true)
                    .size(300)
                    .build(),
                contentDescription = photo.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Selection overlay
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else
                                Color.Black.copy(alpha = 0.3f)
                        )
                )
            }

            // Checkbox
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(28.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.White.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Gray,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Photo info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(6.dp)
            ) {
                Text(
                    text = photo.displayName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateString,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}

