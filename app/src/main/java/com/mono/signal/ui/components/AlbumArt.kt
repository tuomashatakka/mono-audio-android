package com.mono.signal.ui.components

import android.graphics.Bitmap
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.mono.signal.model.Track
import com.mono.signal.ui.theme.MonoTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Album artwork for [track], loaded off-thread via [android.content.ContentResolver.loadThumbnail].
 * Falls back to the aurora gradient wash when a track has no embedded art.
 */
@Composable
fun AlbumArt(
    track: Track?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = track?.id) {
        value = track?.let {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.loadThumbnail(it.uri, Size(1024, 1024), null)
                }.getOrNull()
            }
        }
    }

    Box(modifier = modifier.background(MonoTokens.aurora())) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = track?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
