package com.example.kameraku.data

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import com.example.kameraku.model.PhotoItem

class PhotoRepository(private val contentResolver: ContentResolver) {

    companion object {
        private const val TAG = "PhotoRepository"
    }

    fun getAllPhotos(): List<PhotoItem> {
        val photos = mutableListOf<PhotoItem>()

        Log.d(TAG, "getAllPhotos: Starting query for KameraKu photos")

        // Try method 1: Query by DISPLAY_NAME (works on all Android versions)
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
            )

            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("KameraKu_%")
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            Log.d(TAG, "Querying with DISPLAY_NAME pattern: KameraKu_%")

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                Log.d(TAG, "Query returned ${cursor.count} photos")

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                while (cursor.moveToNext() && photos.size < 100) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: "Unknown"
                        val dateTaken = cursor.getLong(dateTakenColumn)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        photos.add(PhotoItem(contentUri, name, dateTaken, id))
                        Log.d(TAG, "Added photo: $name (ID: $id)")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading photo item", e)
                    }
                }
            }

            Log.d(TAG, "Successfully loaded ${photos.size} photos")
        } catch (e: Exception) {
            Log.e(TAG, "Error querying photos by DISPLAY_NAME", e)
        }

        // Try method 2: Query by RELATIVE_PATH (Android 10+ fallback)
        if (photos.isEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                Log.d(TAG, "Trying RELATIVE_PATH query as fallback")

                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.RELATIVE_PATH
                )

                val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("%Pictures/KameraKu%")
                val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    Log.d(TAG, "RELATIVE_PATH query returned ${cursor.count} photos")

                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                    while (cursor.moveToNext() && photos.size < 100) {
                        try {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn) ?: "Unknown"
                            val dateTaken = cursor.getLong(dateTakenColumn)
                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )

                            photos.add(PhotoItem(contentUri, name, dateTaken, id))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading photo item in fallback", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in RELATIVE_PATH fallback query", e)
            }
        }

        Log.d(TAG, "Total photos found: ${photos.size}")
        return photos
    }

    fun getLatestPhoto(): PhotoItem? {
        Log.d(TAG, "getLatestPhoto: Starting query")

        return try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
            )

            // Use DISPLAY_NAME which works on all Android versions
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("KameraKu_%")
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC LIMIT 1"

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    Log.d(TAG, "Latest photo found: $name (ID: $id, URI: $contentUri)")
                    PhotoItem(contentUri, name, dateTaken, id)
                } else {
                    Log.d(TAG, "No photos found")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest photo", e)
            null
        }
    }
}

