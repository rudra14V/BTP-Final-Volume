package com.example.btp.data

import android.graphics.drawable.Drawable

/**
 * Data representation of an installed application.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isProtected: Boolean = false
)
