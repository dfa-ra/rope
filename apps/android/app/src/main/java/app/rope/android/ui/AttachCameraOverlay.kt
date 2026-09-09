package app.rope.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.rope.android.data.AttachCameraRules
import app.rope.android.data.MediaPayload
import app.rope.android.media.AttachCamera
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun AttachCameraOverlay(
    pending: List<Uri>,
    onStage: (Uri) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    onNotice: (String) -> Unit,
    onUnavailable: () -> Unit,
    onMicDenied: () -> Unit,
) {
    val context = LocalContext.current
    val camera = remember { AttachCamera(context) }
    var recording by remember { mutableStateOf(false) }
    var recordMs by remember { mutableLongStateOf(0L) }
    var flash by remember { mutableStateOf(AttachCameraRules.FLASH_OFF) }
    var flashes by remember { mutableStateOf(listOf<String>()) }
    var usingBack by remember { mutableStateOf(AttachCameraRules.DEFAULT_BACK) }
    var ready by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var holdJob by remember { mutableStateOf<Job?>(null) }
    val pendingSize = remember { mutableIntStateOf(pending.size) }
    pendingSize.intValue = pending.size
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) onMicDenied()
    }

    fun hasMic(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(
            context,
            AttachCameraRules.fileProviderAuthority(context.packageName),
            file,
        )

    fun stageFile(file: File) {
        if (!file.isFile || file.length() < 8) {
            file.delete()
            return
        }
        onStage(uriFor(file))
    }

    DisposableEffect(Unit) {
        onDispose {
            camera.release()
        }
    }

    BackHandler { onClose() }

    LaunchedEffect(recording) {
        if (!recording) {
            recordMs = 0
            return@LaunchedEffect
        }
        while (recording) {
            delay(200)
            recordMs += 200
            if (recordMs >= AttachCameraRules.MAX_VIDEO_MS) {
                val take = camera.stopVideo()
                recording = false
                val file = take?.first
                val ms = take?.second ?: 0L
                if (file != null && ms >= AttachCameraRules.MIN_VIDEO_MS) stageFile(file) else file?.delete()
                break
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    holder.addCallback(
                        object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                val displayDeg = displayRotationDeg(ctx)
                                try {
                                    if (camera.hardwareCount() <= 0) {
                                        onUnavailable()
                                        return
                                    }
                                    camera.open(holder, displayDeg)
                                    flashes = camera.supportedFlash()
                                    usingBack = camera.usingBack
                                    ready = true
                                } catch (_: Exception) {
                                    onUnavailable()
                                }
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int,
                            ) {
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                ready = false
                                camera.release()
                            }
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                camera.cancelVideo()
                onClose()
            }) {
                Icon(Icons.Outlined.Close, contentDescription = "Закрыть", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (AttachCameraRules.showFlash(flashes) && !recording) {
                IconButton(
                    onClick = {
                        val next = AttachCameraRules.cyclePhotoFlash(flash, flashes)
                        flash = next
                        camera.setFlash(next)
                    },
                ) {
                    Icon(
                        when (flash) {
                            AttachCameraRules.FLASH_ON -> Icons.Outlined.FlashOn
                            AttachCameraRules.FLASH_AUTO -> Icons.Outlined.FlashAuto
                            else -> Icons.Outlined.FlashOff
                        },
                        contentDescription = "Вспышка",
                        tint = Color.White,
                    )
                }
            }
            if (!recording) {
                IconButton(
                    onClick = {
                        runCatching { camera.flip() }
                        flashes = camera.supportedFlash()
                        usingBack = camera.usingBack
                    },
                ) {
                    Icon(Icons.Outlined.Cameraswitch, contentDescription = "Камера", tint = Color.White)
                }
            }
            IconButton(onClick = onDone) {
                Icon(Icons.Outlined.Check, contentDescription = "Готово", tint = Color.White)
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (pending.isNotEmpty()) {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(pending, key = { _, uri -> uri.toString() }) { i, _ ->
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", color = Color.White)
                        }
                    }
                }
            }
            Text(
                if (recording) MediaPayload.formatDuration(recordMs) else AttachCameraRules.HOLD_HINT,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .background(if (recording) Color.Red else Color.Transparent)
                    .pointerInput(ready) {
                        awaitEachGesture {
                            awaitFirstDown()
                            val holdArmed = AtomicBoolean(false)
                            holdJob?.cancel()
                            holdJob = scope.launch {
                                delay(AttachCameraRules.HOLD_VIDEO_MS)
                                if (!AttachCameraRules.canCaptureMore(pendingSize.intValue)) {
                                    onNotice(AttachCameraRules.ALBUM_FULL_NOTICE)
                                    return@launch
                                }
                                if (!hasMic()) {
                                    onMicDenied()
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@launch
                                }
                                holdArmed.set(true)
                                val dest = camera.newVideoFile()
                                try {
                                    AttachCameraRules.videoFlash(flashes)?.let { camera.setFlash(it) }
                                    camera.startVideo(dest)
                                    recording = true
                                    recordMs = 0
                                } catch (_: Exception) {
                                    dest.delete()
                                    holdArmed.set(false)
                                    onNotice(AttachCameraRules.UNAVAILABLE_NOTICE)
                                }
                            }
                            waitForUpOrCancellation()
                            holdJob?.cancel()
                            holdJob = null
                            if (recording || camera.recording) {
                                val take = camera.stopVideo()
                                recording = false
                                val file = take?.first
                                val ms = take?.second ?: 0L
                                if (file != null && (ms >= AttachCameraRules.MIN_VIDEO_MS || file.length() >= 8_000)) {
                                    stageFile(file)
                                } else {
                                    file?.delete()
                                }
                            } else if (!holdArmed.get()) {
                                if (!AttachCameraRules.canCaptureMore(pendingSize.intValue)) {
                                    onNotice(AttachCameraRules.ALBUM_FULL_NOTICE)
                                } else if (ready) {
                                    val dest = camera.newStillFile()
                                    camera.takePicture(dest) { ok ->
                                        if (ok) stageFile(dest) else dest.delete()
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {}
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AttachSheetCameraCell(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val camera = remember { AttachCamera(context) }
    Box(
        modifier
            .size(72.dp)
            .clip(RoundedCornerShape(RopeShapes.media))
            .clickable {
                camera.release()
                onClick()
            }
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    holder.addCallback(
                        object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                try {
                                    if (camera.hardwareCount() <= 0) return
                                    camera.open(holder, displayRotationDeg(ctx))
                                } catch (_: Exception) {
                                }
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int,
                            ) {
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                camera.release()
                            }
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Icon(Icons.Outlined.PhotoCamera, contentDescription = "Камера", tint = Color.White)
    }
    DisposableEffect(Unit) {
        onDispose { camera.release() }
    }
}

private fun displayRotationDeg(ctx: Context): Int = try {
    @Suppress("DEPRECATION")
    when ((ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation) {
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
} catch (_: Exception) {
    0
}
