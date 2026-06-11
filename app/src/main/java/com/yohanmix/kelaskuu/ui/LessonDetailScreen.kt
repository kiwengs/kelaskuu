@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)

package com.yohanmix.kelaskuu.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple

@Composable
fun LessonDetailScreen(
    lessonTitle: String,
    description: String,
    videoUrl: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    
    var isFullScreen by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var videoSize by remember { mutableStateOf<VideoSize?>(null) }
    
    val tintColor = MaterialTheme.colorScheme.primary

    // Initialize ExoPlayer (Hanya untuk file video hasil upload)
    val exoPlayer = remember(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                prepare()
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(size: VideoSize) {
                        videoSize = size
                    }
                })
            }
        } else null
    }

    DisposableEffect(videoUrl) {
        onDispose {
            exoPlayer?.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(isFullScreen, videoSize) {
        if (isFullScreen) {
            videoSize?.let { size ->
                if (size.width < size.height) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else if (size.width > size.height) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    title = { Text(lessonTitle, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = tintColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = tintColor
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(if (isFullScreen) PaddingValues(0.dp) else paddingValues)
                .padding(horizontal = if (isFullScreen) 0.dp else 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isFullScreen) Modifier.fillMaxHeight()
                        else Modifier.height(220.dp)
                    )
                    .clip(if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (videoUrl.isNotEmpty() && exoPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                setBackgroundColor(android.graphics.Color.BLACK)
                            }
                        },
                        update = { playerView ->
                            playerView.resizeMode = resizeMode
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Row {
                            IconButton(
                                onClick = {
                                    resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    } else {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Zoom Mode",
                                    tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) KelaskuuPurple else Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { isFullScreen = !isFullScreen },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Text("Video tidak tersedia atau format tidak didukung", color = Color.White)
                }
            }

            if (!isFullScreen) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Deskripsi Materi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = tintColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description.ifEmpty { "Tidak ada deskripsi untuk materi ini." },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
