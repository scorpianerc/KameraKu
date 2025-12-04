package com.example.kameraku.model

import android.net.Uri

data class PhotoItem(
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long,
    val id: Long
)

