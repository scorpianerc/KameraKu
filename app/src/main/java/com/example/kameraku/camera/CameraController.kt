package com.example.kameraku.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("unused")
class CameraController {

    companion object {
        private const val TAG = "CameraController"

        private fun savePhotoToFile(
            context: Context,
            imageCapture: ImageCapture,
            onSuccess: (Uri) -> Unit,
            onError: (String) -> Unit
        ) {
            try {
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val displayName = "KameraKu_$timestamp.jpg"

                // Create directory
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "KameraKu")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }

                val photoFile = File(appDir, displayName)

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            try {
                                // Scan file to make it visible in gallery
                                val uri = Uri.fromFile(photoFile)
                                val contentValues = ContentValues().apply {
                                    put(MediaStore.Images.Media.DATA, photoFile.absolutePath)
                                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                }

                                val insertedUri = context.contentResolver.insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    contentValues
                                )

                                Log.d(TAG, "Photo saved to file and added to MediaStore: ${insertedUri ?: uri}")
                                onSuccess(insertedUri ?: uri)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error adding to MediaStore, but file saved", e)
                                onSuccess(Uri.fromFile(photoFile))
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "File save failed: ${exception.message}", exception)
                            onError("Gagal menyimpan ke file: ${exception.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during file save setup", e)
                onError("Error: ${e.message}")
            }
        }

        fun takePhoto(
            context: Context,
            imageCapture: ImageCapture,
            onSuccess: (Uri) -> Unit,
            onError: (String) -> Unit
        ) {
            // For Android 9 and below, use file-based saving
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                Log.d(TAG, "Using file-based save for Android < 10")
                savePhotoToFile(context, imageCapture, onSuccess, onError)
                return
            }

            // For Android 10+, use MediaStore
            try {
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val displayName = "KameraKu_$timestamp.jpg"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KameraKu")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(
                        context.contentResolver,
                        collection,
                        contentValues
                    )
                    .build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = outputFileResults.savedUri
                            if (savedUri != null) {
                                try {
                                    // Mark as finished writing
                                    contentValues.clear()
                                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                                    context.contentResolver.update(savedUri, contentValues, null, null)

                                    Log.d(TAG, "Photo saved successfully via MediaStore: $savedUri")
                                    onSuccess(savedUri)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error updating IS_PENDING, but photo saved", e)
                                    onSuccess(savedUri)
                                }
                            } else {
                                Log.e(TAG, "SavedUri is null, trying file-based fallback")
                                savePhotoToFile(context, imageCapture, onSuccess, onError)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "MediaStore save failed: ${exception.message}, trying file-based fallback", exception)
                            Log.e(TAG, "Error code: ${exception.imageCaptureError}")

                            // Fallback to file-based save
                            savePhotoToFile(context, imageCapture, onSuccess, onError)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during MediaStore save, trying file-based fallback", e)
                savePhotoToFile(context, imageCapture, onSuccess, onError)
            }
        }

        fun toggleFlash(camera: Camera?, currentFlashMode: Int): Int {
            return if (camera?.cameraInfo?.hasFlashUnit() == true) {
                when (currentFlashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
            } else {
                ImageCapture.FLASH_MODE_OFF
            }
        }

        fun toggleTorch(camera: Camera?, isEnabled: Boolean): Boolean {
            return if (camera?.cameraInfo?.hasFlashUnit() == true) {
                camera.cameraControl.enableTorch(!isEnabled)
                !isEnabled
            } else {
                false
            }
        }

        fun switchCamera(currentSelector: CameraSelector): CameraSelector {
            return if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
    }
}
