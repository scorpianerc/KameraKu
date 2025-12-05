# KameraKu - Aplikasi Kamera Android

Aplikasi kamera Android modern dengan fitur lengkap menggunakan CameraX API dan Jetpack Compose.

## Fitur Utama

### Fitur Wajib
- **Live Camera Preview**: Tampilan preview kamera real-time dengan resolusi tinggi
- **Capture Photo**: Tombol ambil foto dengan animasi flash
- **MediaStore Integration**: Foto disimpan otomatis ke galeri sistem di folder `Pictures/KameraKu`
- **Thumbnail Preview**: Menampilkan thumbnail foto terakhir yang diambil
- **Photo Gallery**: Galeri lengkap untuk melihat semua foto yang telah diambil

### Fitur Opsional
- **Flash Control**: Toggle flash dengan 3 mode (Off, On, Auto) dengan visual feedback
- **Camera Switch**: Beralih antara kamera depan dan belakang dengan smooth transition
- **Auto-Rotate Photos**: Foto otomatis ter-rotate sesuai orientasi device (portrait/landscape)
- **Tap-to-Focus**: Ketuk layar untuk fokus ke area tertentu

### Fitur Tambahan
- **Modern UI**: Desain antarmuka yang menarik dengan Material Design 3
- **Photo Info**: Menampilkan nama file dan tanggal pengambilan foto
- **Grid Gallery**: Tampilan galeri grid 2 kolom dengan thumbnail yang responsif
- **Multi-Delete Photos**: Long press untuk selection mode, delete banyak foto sekaligus
- **Select All**: Pilih/lepas semua foto dengan satu tap
- **Delete Confirmation**: Dialog konfirmasi sebelum menghapus foto
- **Image Viewer**: Membuka foto dalam aplikasi galeri sistem untuk melihat detail
- **Permission Handling**: Manajemen izin yang user-friendly dengan fallback
- **Loading States**: Indikator loading dan empty states yang informatif
- **Refresh Gallery**: Manual refresh untuk memuat foto terbaru
- **Real-time Updates**: Thumbnail dan galeri update otomatis setelah capture/delete

## Arsitektur

### Struktur Proyek
```
com.example.kameraku/
├── MainActivity.kt              # Activity utama dengan camera screen
├── camera/
│   ├── CameraPreview.kt        # UI kamera dengan preview dan controls
│   └── CameraController.kt     # Logic untuk capture, flash, dan switch
├── data/
│   └── PhotoRepository.kt      # Repository untuk akses MediaStore
├── model/
│   └── PhotoItem.kt           # Data model untuk foto
└── ui/
    ├── PhotoGalleryActivity.kt # Activity untuk galeri foto
    └── theme/                  # Material Design 3 theme
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

### Teknologi yang Digunakan
- **Kotlin**: Bahasa pemrograman utama
- **Jetpack Compose**: Modern UI toolkit
- **CameraX**: API kamera Android yang modern dan konsisten
- **Material Design 3**: Desain system terbaru dari Google
- **Coil**: Library untuk loading image secara efisien
- **MediaStore API**: Penyimpanan foto ke galeri sistem
- **Coroutines**: Untuk operasi asynchronous

## Alur Izin (Permission Flow)

### Izin yang Diperlukan

Aplikasi memerlukan izin berbeda tergantung versi Android:

#### Android 13+ (API 33+)
- **CAMERA**: Untuk mengakses kamera device
- **READ_MEDIA_IMAGES**: Untuk membaca foto dari galeri (granular permission)

#### Android 10-12 (API 29-32)
- **CAMERA**: Untuk mengakses kamera device
- **READ_EXTERNAL_STORAGE**: Untuk membaca foto dari storage
- **requestLegacyExternalStorage**: Kompatibilitas dengan Scoped Storage

#### Android 9 dan Sebelumnya (API 28-)
- **CAMERA**: Untuk mengakses kamera device
- **READ_EXTERNAL_STORAGE**: Untuk membaca foto
- **WRITE_EXTERNAL_STORAGE**: Untuk menyimpan foto ke storage

### Flow Permission

```
1. Aplikasi dibuka
   ↓
2. Deteksi versi Android
   ↓
3. Request permission sesuai versi:
   - Android 13+: CAMERA + READ_MEDIA_IMAGES
   - Android 10-12: CAMERA + READ_EXTERNAL_STORAGE
   - Android 9-: CAMERA + READ + WRITE EXTERNAL_STORAGE
   ↓
4. User memberikan atau menolak izin
   ↓
5a. Jika izin DIBERIKAN:
    - Inisialisasi CameraX
    - Setup OrientationEventListener (auto-rotate)
    - Bind camera ke lifecycle
    - Tampilkan preview
    - Enable semua kontrol (capture, switch, flash)
    ↓
5b. Jika izin DITOLAK:
    - Tampilkan empty state informatif
    - Icon kamera besar dengan pesan
    - Tombol "Berikan Izin" untuk request ulang
    - Penjelasan kenapa izin diperlukan
```

### Implementasi di Kode

**AndroidManifest.xml**
```xml
<!-- Camera permission (semua Android version) -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Storage permissions (version-specific) -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

<!-- Camera features -->
<uses-feature android:name="android.hardware.camera.any" />
<uses-feature android:name="android.hardware.camera.flash" android:required="false" />

<application
    ...
    android:requestLegacyExternalStorage="true">
    <!-- Untuk kompatibilitas Android 10 -->
</application>
```
Bagian ini mendeklarasikan permissions dan features yang dibutuhkan aplikasi di level manifest. Permission CAMERA diperlukan untuk mengakses kamera device pada semua versi Android. Untuk storage, terdapat strategi berbeda berdasarkan SDK version dimana WRITE_EXTERNAL_STORAGE hanya berlaku hingga Android 9 (API 28), READ_EXTERNAL_STORAGE berlaku hingga Android 12L (API 32), sedangkan READ_MEDIA_IMAGES adalah permission baru untuk Android 13 ke atas yang lebih spesifik hanya untuk akses gambar. Deklarasi uses-feature memberitahu Google Play Store bahwa aplikasi memerlukan kamera, namun flash bersifat opsional sehingga aplikasi tetap bisa diinstall di device tanpa flash. Atribut requestLegacyExternalStorage pada application tag mengaktifkan mode legacy storage untuk kompatibilitas dengan Android 10 yang memperkenalkan Scoped Storage, memberikan aplikasi akses storage seperti versi Android sebelumnya.

**Runtime Permission Request (Version-Specific)**
```kotlin
val permissions = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
        // Android 13+ (API 33+)
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    }
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
        // Android 10-12 (API 29-32)
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
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
    // Camera permission adalah yang paling penting
}

// Request permissions saat app dibuka
LaunchedEffect(Unit) {
    launcher.launch(permissions)
}
```
Kode ini mengimplementasikan strategi runtime permission yang adaptif terhadap versi Android yang berbeda. Menggunakan expression when dengan Build.VERSION.SDK_INT, aplikasi menentukan kombinasi permissions yang tepat untuk diminta: pada Android 13 ke atas hanya memerlukan CAMERA dan READ_MEDIA_IMAGES karena sistem sudah menggunakan granular media permissions, pada Android 10 hingga 12 menggunakan CAMERA dan READ_EXTERNAL_STORAGE karena Scoped Storage sudah diterapkan namun belum ada media permissions terpisah, sedangkan Android 9 ke bawah memerlukan ketiga permissions termasuk WRITE_EXTERNAL_STORAGE karena masih menggunakan sistem storage tradisional. Launcher dibuat menggunakan rememberLauncherForActivityResult dengan contract RequestMultiplePermissions yang mengembalikan map hasil permission request, dimana callbacknya akan memeriksa apakah CAMERA permission sudah diizinkan karena permission ini sangat diperlukan untuk fungsi aplikasi. LaunchedEffect dengan key Unit memastikan permission request dijalankan sekali saat composable pertama kali dibuat.

**Permission State UI**
```kotlin
if (!cameraPermissionGranted) {
    // Tampilkan empty state dengan tombol request permission
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Camera, modifier = Modifier.size(64.dp))
            Text("Izin Kamera Diperlukan")
            Text("Aplikasi memerlukan izin kamera untuk mengambil foto")
            Button(onClick = { launcher.launch(permissions) }) {
                Text("Berikan Izin")
            }
        }
    }
    return
}

// Jika izin granted, tampilkan camera preview
CameraPreviewScreen()
```
Kode ini mengatur tampilan UI berdasarkan status camera permission. Ketika camera permission belum diberikan, aplikasi menampilkan empty state yang informatif dan actionable di tengah layar menggunakan Box dengan fillMaxSize dan center alignment. Empty state terdiri dari Column yang berisi ikon kamera, text yang menjelaskan mengapa permission diperlukan, dan button yang memungkinkan user untuk trigger permission request ulang melalui launcher yang sama. Penggunaan return statement setelah blok if memastikan kode tidak melanjutkan ke CameraPreviewScreen ketika permission belum diberikan, mencegah crash atau error akibat mencoba mengakses kamera tanpa izin. Sebaliknya, jika cameraPermissionGranted bernilai true, aplikasi langsung menampilkan CameraPreviewScreen yang merupakan komponen utama untuk mengoperasikan kamera.

## MediaStore Integration

### Dual Save Strategy

Aplikasi menggunakan **dua strategi penyimpanan** untuk kompatibilitas maksimal dengan semua versi Android:

#### Strategy 1: MediaStore API (Android 10+)

Untuk Android 10 ke atas, menggunakan **Scoped Storage** dengan MediaStore API:

```kotlin
fun takePhoto(context: Context, imageCapture: ImageCapture, onSuccess: (Uri) -> Unit) {
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val timestamp = dateFormat.format(Date())
    val displayName = "KameraKu_$timestamp.jpg"
    
    // 1. Buat metadata foto dengan IS_PENDING flag
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KameraKu")
        put(MediaStore.Images.Media.IS_PENDING, 1)  // Hide dari gallery saat sedang ditulis
    }
    
    // 2. Gunakan VOLUME_EXTERNAL_PRIMARY untuk Android 10+
    val collection = MediaStore.Images.Media.getContentUri(
        MediaStore.VOLUME_EXTERNAL_PRIMARY
    )
    
    // 3. Buat output options dengan MediaStore URI
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        collection,
        contentValues
    ).build()
    
    // 4. Capture foto
    imageCapture.takePicture(outputOptions, executor, object : OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: OutputFileResults) {
            val savedUri = outputFileResults.savedUri
            
            // 5. Mark as finished (IS_PENDING = 0)
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(savedUri, contentValues, null, null)
            
            onSuccess(savedUri)
        }
        
        override fun onError(exception: ImageCaptureException) {
            // Fallback ke file-based save
            savePhotoToFile(context, imageCapture, onSuccess, onError)
        }
    })
}
```
Strategi pertama menggunakan MediaStore API. Fungsi ini membuat timestamp unik menggunakan SimpleDateFormat untuk nama file yang unik setiap kali foto diambil. ContentValues diisi dengan metadata foto termasuk display name, MIME type image/jpeg, relative path ke folder Pictures/KameraKu, dan IS_PENDING flag diset ke 1 yang berfungsi menyembunyikan foto dari gallery selama proses penulisan berlangsung untuk mencegah aplikasi lain mengakses file yang belum complete. URI collection dibuat menggunakan VOLUME_EXTERNAL_PRIMARY yang merupakan primary external storage untuk Android 10 ke atas, menggantikan pendekatan file path yang sudah deprecated. OutputFileOptions dibuat dengan ContentResolver dan collection URI agar CameraX menulis langsung ke MediaStore tanpa perlu WRITE_EXTERNAL_STORAGE permission. Setelah foto berhasil disimpan, IS_PENDING flag diupdate menjadi 0 melalui contentResolver.update agar foto menjadi visible di gallery dan dapat diakses aplikasi lain. Jika terjadi error saat saving, fungsi otomatis fallback ke savePhotoToFile.

**Penjelasan IS_PENDING**:
- `IS_PENDING = 1`: File sedang ditulis, tidak visible di gallery
- `IS_PENDING = 0`: File selesai ditulis, visible di gallery
- Mencegah app lain membaca file yang belum selesai

#### Strategy 2: File-Based Save (Android 9-, Fallback)

Untuk Android 9 ke bawah atau jika MediaStore gagal:

```kotlin
fun savePhotoToFile(context: Context, imageCapture: ImageCapture, onSuccess: (Uri) -> Unit) {
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val timestamp = dateFormat.format(Date())
    val displayName = "KameraKu_$timestamp.jpg"
    
    // 1. Buat directory Pictures/KameraKu
    val picturesDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES
    )
    val appDir = File(picturesDir, "KameraKu")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    
    // 2. Buat file
    val photoFile = File(appDir, displayName)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    // 3. Capture ke file
    imageCapture.takePicture(outputOptions, executor, object : OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: OutputFileResults) {
            // 4. Tambahkan ke MediaStore secara manual
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, photoFile.absolutePath)
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            
            val insertedUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            onSuccess(insertedUri ?: Uri.fromFile(photoFile))
        }
        
        override fun onError(exception: ImageCaptureException) {
            onError("Gagal menyimpan foto: ${exception.message}")
        }
    })
}
```
Strategi kedua adalah file based save berfungsi sebagai fallback jika MediaStore API gagal. Fungsi ini menggunakan Environment.getExternalStoragePublicDirectory untuk mendapatkan directory Pictures publik, kemudian membuat subdirectory KameraKu menggunakan mkdirs jika belum ada, memastikan struktur folder tersedia sebelum menyimpan file. File object dibuat dengan path lengkap dan dijadikan OutputFileOptions untuk CameraX menulis foto langsung ke file system tanpa melalui MediaStore. Setelah foto berhasil ditulis ke file, fungsi melakukan insert manual ke MediaStore dengan ContentValues yang berisi absolute path file, display name, dan MIME type, sehingga foto menjadi visible di gallery meskipun disimpan dengan cara legacy. URI yang dikembalikan adalah hasil insert ke MediaStore atau jika gagal menggunakan Uri.fromFile sebagai fallback terakhir, memastikan aplikasi selalu mendapat valid URI untuk foto yang tersimpan. Pendekatan ini memerlukan WRITE_EXTERNAL_STORAGE permission untuk Android 9 ke bawah.

#### Keuntungan Dual Strategy:
- ✅ **Android 10+**: Tidak perlu `WRITE_EXTERNAL_STORAGE`, Scoped Storage compliant
- ✅ **Android 9-**: Tetap berfungsi dengan file-based save
- ✅ **Fallback**: Jika MediaStore gagal, otomatis pakai file-based
- ✅ **Reliability**: Double safety net untuk memastikan foto selalu tersimpan
- ✅ **Compatibility**: Works di semua Android version (7.0+)

### MediaStore

Aplikasi menggunakan **query by filename pattern** 

#### Primary Query 

```kotlin
fun getAllPhotos(): List<PhotoItem> {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN
    )
    
    // Query by DISPLAY_NAME - works di semua Android version
    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("KameraKu_%")  // Semua foto dengan prefix KameraKu_
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    
    contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        
        while (cursor.moveToNext() && photos.size < 100) {  // Limit 100 foto
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val dateTaken = cursor.getLong(dateTakenColumn)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            
            photos.add(PhotoItem(contentUri, name, dateTaken, id))
        }
    }
    
    return photos
}
```
Primary query menggunakan DISPLAY_NAME sebagai filter. Projection array menentukan kolom yang akan diambil yaitu ID untuk unique identifier, DISPLAY_NAME untuk nama file, dan DATE_TAKEN untuk sorting berdasarkan waktu pengambilan foto. Selection clause menggunakan LIKE operator dengan pattern "KameraKu_%" yang berarti semua nama file yang dimulai dengan prefix KameraKu_ akan terquery, memastikan hanya foto dari aplikasi ini yang muncul tanpa tercampur foto lain. Sort order menggunakan DATE_TAKEN DESC untuk menampilkan foto terbaru di urutan paling atas. Cursor diproses dalam use block untuk automatic resource cleanup, dimana setiap row diiterasi dengan moveToNext hingga mencapai limit 100 foto untuk menghindari memory issues pada device dengan banyak foto. ContentUris.withAppendedId digunakan untuk membuat content URI lengkap dari ID foto yang bisa digunakan untuk loading image atau operasi lain.

#### Fallback Query (Android 10+ Only)

Jika primary query gagal, coba dengan RELATIVE_PATH:

```kotlin
// Only for Android 10+ as fallback
if (photos.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("%Pictures/KameraKu%")
    // ... same query logic
}
```
Fallback query dijalankan jika primary query tidak menghasilkan foto apapun dan device menjalankan Android 10 atau lebih tinggi. Query menggunakan RELATIVE_PATH column yang merupakan fitur baru di Android 10 untuk mencari foto berdasarkan folder path relatif Pictures/KameraKu. Pattern "%Pictures/KameraKu%" dengan wildcard di awal dan akhir memastikan path akan sesuai meskipun ada variasi seperti "/storage/emulated/0/Pictures/KameraKu" atau path lain yang mengandung substring tersebut. Fallback berguna jika foto disimpan dengan cara lain yang tidak menggunakan naming convention KameraKu_ atau jika ada inkonsistensi dalam metadata DISPLAY_NAME. Dengan kombinasi primary dan fallback query, aplikasi memiliki double safety net untuk menemukan semua foto yang pernah disimpan.

### Menghapus Foto dari MediaStore

```kotlin
// Dari PhotoGalleryActivity.kt - Delete confirmation dialog
// Delete photos dari MediaStore
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
refreshTrigger++  // Auto-refresh gallery after delete

Toast.makeText(context, "${selectedPhotos.size} foto dihapus", Toast.LENGTH_SHORT).show()
```
Proses delete mengiterasi setiap photo ID yang dipilih user dan menggunakan ContentUris.withAppendedId untuk membuat URI spesifik untuk setiap foto dari ID nya. ContentResolver.delete dipanggil dengan URI untuk menghapus foto dari MediaStore, operasi ini menghapus file fisik foto dari storage pada Android 10 ke atas karena MediaStore mengelola lifecycle file secara otomatis. Try-catch block membungkus operasi delete untuk menangani error seperti permission issues atau file sudah terhapus. Setelah semua foto terhapus, state variables direset dimana showDeleteDialog diset false untuk menutup confirmation dialog, isSelectionMode dimatikan untuk kembali ke normal browsing mode, dan selectedPhotos dikosongkan. RefreshTrigger diincrement untuk trigger recomposition dan refresh gallery list, memastikan UI langsung update menampilkan foto yang tersisa tanpa perlu manual refresh dari user. Toast notification ditampilkan dengan jumlah foto yang berhasil dihapus untuk memberikan feedback yang clear kepada user bahwa operasi delete telah selesai.

## Rotasi dan Orientasi

### Auto-Rotation dengan OrientationEventListener

Aplikasi mengimplementasikan **auto-rotation real-time** menggunakan `OrientationEventListener` untuk mendeteksi rotasi device dan menyesuaikan orientasi foto secara otomatis.

#### Implementasi OrientationEventListener

```kotlin
// Setup orientation listener untuk auto-rotate
val orientationEventListener = remember {
    object : android.view.OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            
            // Convert orientation (0-360°) ke Surface.ROTATION constant
            val rotation = when (orientation) {
                in 45..134 -> android.view.Surface.ROTATION_270    // Landscape kiri (90° CCW)
                in 135..224 -> android.view.Surface.ROTATION_180   // Portrait terbalik (180°)
                in 225..314 -> android.view.Surface.ROTATION_90    // Landscape kanan (90° CW)
                else -> android.view.Surface.ROTATION_0            // Portrait normal (0°)
            }
            
            // Update ImageCapture target rotation
            imageCapture?.targetRotation = rotation
        }
    }
}

// Enable listener saat composable active
DisposableEffect(Unit) {
    orientationEventListener.enable()
    onDispose {
        orientationEventListener.disable()  // Cleanup
    }
}
```
OrientationEventListener diwrap dalam remember block agar instance listener tidak dibuat ulang setiap recomposition. Callback onOrientationChanged dipanggil setiap kali device orientation berubah dengan parameter orientation dalam derajat 0-360, check ORIENTATION_UNKNOWN dilakukan untuk mengabaikan nilai yang tidak valid ketika sensor tidak dapat menentukan orientasi. When expression melakukan mapping dari continuous degree values ke discrete Surface.ROTATION constants dengan range checking, misalnya 45-134 derajat dianggap landscape kiri dengan ROTATION_270, memastikan hanya ada 4 kemungkinan rotation state yang clear dan tidak ambiguous. Target rotation dari ImageCapture object langsung diupdate tanpa delay, sehingga foto berikutnya yang diambil akan memiliki orientasi yang benar sesuai posisi device saat memfoto. DisposableEffect dengan key Unit memastikan listener dienable hanya sekali saat composable pertama kali muncul dan didisable saat composable didestroy melalui onDispose callback.

#### Set Target Rotation saat Camera Init

```kotlin
// Initialize ImageCapture dengan target rotation
val newImageCapture = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    .setFlashMode(flashMode)
    .setTargetRotation(view.display.rotation)  // Set initial rotation
    .build()

// Update rotation setelah camera ready
imageCapture = newImageCapture
newImageCapture.targetRotation = view.display.rotation
```
ImageCapture object dibuat menggunakan builder pattern dengan konfigurasi capture mode MINIMIZE_LATENCY untuk mengurangi delay saat mengambil foto dan meningkatkan responsiveness shutter button. Flash mode diset sesuai preferensi user yang sudah tersimpan dalam state flashMode yaitu flash on, off, atau auto. Target rotation initial diset menggunakan view.display.rotation yang memberikan current display rotation saat camera pertama kali diinisialisasi, memastikan foto pertama sudah memiliki orientasi yang benar sesuai posisi device. Setelah ImageCapture object selesai dibuild, reference disimpan ke variable imageCapture untuk digunakan OrientationEventListener, kemudian target rotation diupdate untuk double check bahwa rotation state sudah sinkron dengan display. Pendekatan ini mengkombinasikan initial static rotation dengan dynamic rotation updates dari OrientationEventListener, menciptakan sistem orientasi dimana foto selalu tersimpan dengan EXIF orientation metadata yang benar, sehingga gallery apps dapat menampilkan foto dengan orientasi yang benar tanpa manual rotation dari user.

### Cara Kerja Auto-Rotation

```
User memutar HP
    ↓
OrientationEventListener deteksi sudut (0-360°)
    ↓
Convert ke Surface.ROTATION constant:
    - 0° - 44° = ROTATION_0 (Portrait)
    - 45° - 134° = ROTATION_270 (Landscape kiri)
    - 135° - 224° = ROTATION_180 (Portrait terbalik)
    - 225° - 314° = ROTATION_90 (Landscape kanan)
    - 315° - 359° = ROTATION_0 (Portrait)
    ↓
Update imageCapture.targetRotation
    ↓
Foto berikutnya ter-save dengan orientasi yang benar
    ↓
EXIF metadata menyimpan orientasi info
```

### Fitur Orientasi

#### 1. **Real-time Rotation Detection**
- Mendeteksi rotasi device secara real-time
- Update target rotation tanpa delay
- Tidak perlu restart camera

#### 2. **EXIF Metadata**
```kotlin
// CameraX otomatis menambahkan EXIF orientation ke foto
// Tidak perlu manual processing
implementation("androidx.exifinterface:exifinterface:1.3.7")
```
CameraX secara otomatis menambahkan informasi rotasi (orientation) ke dalam metadata EXIF pada foto yang diambil. Jadi tidak perlu lagi mengatur rotasi gambar secara manual saat menampilkan atau memproses foto.

#### 3. **Image Loading dengan Auto-Rotate**
```kotlin
// Coil otomatis membaca EXIF dan merotasi image saat display
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(photoUri)
        .crossfade(true)
        .size(300)  // Thumbnail size
        .build(),
    contentDescription = "Photo",
    contentScale = ContentScale.Crop
)
```
Kode ini menggunakan library Coil untuk menampilkan gambar dengan AsyncImage. Coil otomatis membaca data EXIF dari gambar dan menyesuaikan rotasinya saat ditampilkan, sehingga foto tidak akan terlihat miring. photoUri digunakan sebagai sumber gambar, crossfade(true) memberi efek transisi halus, size(300) mengatur ukuran thumbnail, dan contentScale = ContentScale.Crop memastikan gambar memenuhi area tampilan dengan proporsional.

#### 4. **Portrait & Landscape Support**
- **Portrait Normal (0°)**: HP tegak, foto portrait
- **Landscape Kanan (90°)**: HP landscape kanan, foto landscape
- **Portrait Terbalik (180°)**: HP terbalik, foto portrait terbalik
- **Landscape Kiri (270°)**: HP landscape kiri, foto landscape

## Cara Menjalankan

### Prerequisites
- Android Studio Hedgehog (2023.1.1) atau lebih baru
- Android SDK API Level 22 (Lollipop) atau lebih tinggi
- Device atau emulator dengan kamera

## Cara Menggunakan

### Mengambil Foto

1. **Buka aplikasi** - Preview kamera akan langsung muncul
2. **Atur pengaturan** (opsional):
   - Ketuk ikon flash (atas kiri) untuk mengubah mode flash
   - Ketuk ikon torch (atas kanan) untuk menyalakan senter
   - Ketuk ikon switch (bawah kanan) untuk ganti kamera
3. **Ambil foto** - Ketuk tombol lingkaran besar putih di tengah bawah
4. **Animasi flash** akan muncul saat foto berhasil diambil
5. **Toast notification** menunjukkan foto tersimpan

### Melihat Galeri Foto

1. **Ketuk thumbnail** di kiri bawah layar kamera
2. **Galeri terbuka** menampilkan semua foto KameraKu dalam grid 2 kolom
3. **Ketuk foto** untuk membuka dalam viewer fullscreen
4. **Info foto** (nama dan tanggal) ditampilkan di bawah setiap thumbnail
5. **Kembali** dengan tombol back di toolbar
6. **Refresh** dengan tombol refresh di toolbar untuk reload foto

### Menghapus Foto (Multi-Delete)

1. **Long press** pada foto di galeri
2. **Selection mode aktif** - Checkbox muncul di semua foto
3. **Tap foto** untuk select/deselect (bisa pilih banyak foto)
4. **Select All** dengan tombol pilih semua di toolbar untuk pilih semua
5. **Delete** dengan tombol tempat sampah di toolbar
6. **Konfirmasi** akan muncul menanyakan apakah yakin menghapus
7. **Tap "Hapus"** untuk menghapus atau "Batal" untuk membatalkan
8. **Toast notification** menunjukkan berapa foto yang dihapus
9. **Gallery auto-refresh** setelah delete

**Visual Feedback**:
- Selected foto: Border biru + overlay biru terang + checkbox aktif
- Not selected: Overlay hitam gelap + checkbox kosong
- Delete button: Merah saat ada foto terpilih, abu-abu saat kosong

### Menggunakan Auto-Rotate

1. **Posisikan HP** dalam orientasi yang diinginkan (portrait/landscape)
2. **Tunggu sebentar** (~0.5 detik) untuk OrientationEventListener detect
3. **Ambil foto** - Foto otomatis ter-save dengan orientasi yang benar
4. **Buka galeri** - Foto ditampilkan dengan orientasi yang benar
5. **Tidak perlu manual rotate** - Semua otomatis!

### Tap-to-Focus

1. **Tap** di area mana saja pada camera preview
2. **Toast "Fokus..."** akan muncul
3. **Camera fokus** ke area yang di-tap
4. **Ambil foto** setelah fokus tercapai

### Kontrol Kamera

| Kontrol | Fungsi | Lokasi | Feedback |
|---------|--------|--------|----------|
| **Flash** | Toggle OFF/ON/AUTO | Kanan atas | Icon berubah warna, toast notification |
| **Capture** | Ambil foto | Tengah bawah | Button abu-abu → putih (ready indicator) |
| **Gallery** | Buka galeri | Kiri bawah | Thumbnail update otomatis |
| **Switch** | Ganti kamera depan/belakang | Kanan bawah | Toast "Kamera Depan/Belakang" |
| **Tap Preview** | Fokus ke area | Di mana saja di preview | Toast "Fokus..." |
| **Long Press** | Selection mode (di galeri) | Pada foto | Checkbox muncul |
| **Refresh** | Reload galeri | Toolbar galeri | Loading indicator |
| **Select All** | Pilih semua foto | Toolbar galeri (selection mode) | Semua foto ter-select |
| **Delete** | Hapus foto terpilih | Toolbar galeri (selection mode) | Confirmation dialog |

## Lokasi Penyimpanan

Foto disimpan di:
```
/storage/emulated/0/Pictures/KameraKu/
```

Format nama file:
```
KameraKu_YYYYMMDD_HHMMSS.jpg
```

Contoh:
```
KameraKu_20231204_143022.jpg
```

### Device Requirements

**Minimum**:
- Android 7.0 (Nougat) - API 24
- RAM: 2GB
- Storage: 100MB free space
- Camera: Rear or front camera

**Recommended**:
- Android 10+ (Q) - API 29+
- RAM: 4GB+
- Storage: 500MB free space
- Camera: Rear + front camera with flash

### Tech Stack
- **Language**: Kotlin 1.9.0
- **UI**: Jetpack Compose
- **Camera**: CameraX 1.3.4
- **Image Loading**: Coil 2.5.0
- **Architecture**: MVVM-like with Compose
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

---
## Developer

SCORPIAN ERICKDA - 225150200111061
Developed with using Android Studio and Jetpack Compose

---

**Version**: 1.0 (Optimized)  
**Min SDK**: 24 (Android 7.0 Nougat)  
**Target SDK**: 36
