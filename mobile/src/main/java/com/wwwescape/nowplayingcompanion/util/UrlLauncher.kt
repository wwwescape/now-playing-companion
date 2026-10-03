package com.wwwescape.nowplayingcompanion.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri

/** Opens [url] in the user's browser. Only web links are allowed, and a device with no browser
 * is a no-op rather than a crash. Same helper as the sibling apps. */
fun openUrl(context: Context, url: String) {
    val uri = url.toUri()
    if (uri.scheme?.lowercase() !in setOf("https", "http")) {
        Log.w("UrlLauncher", "Refusing to open non-web URL")
        return
    }
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w("UrlLauncher", "No app available to open URL", e)
    }
}
