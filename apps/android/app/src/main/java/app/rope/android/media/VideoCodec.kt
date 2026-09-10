package app.rope.android.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import app.rope.android.data.VideoRules
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class CompressedVideo(
    val bytes: ByteArray,
    val mime: String,
    val durationMs: Long,
    val name: String,
)

/**
 * Compress a gallery clip to the existing 25 MiB object cap. Not crypto:
 * [encryptObject] still seals the bytes in Rust.
 */
object VideoCodec {
    fun durationMs(path: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun durationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun poster(path: String): Bitmap? {
        val file = File(path)
        if (!file.isFile || file.length() < 8) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun normalizeForSend(context: Context, uri: Uri, mime: String, name: String, cacheDir: File): CompressedVideo? {
        cacheDir.mkdirs()
        val src = File(cacheDir, "vin-${System.nanoTime()}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                src.outputStream().use { input.copyTo(it) }
            } ?: return null
            if (!src.isFile || src.length() <= 0L) return null
            val duration = durationMs(src.absolutePath)
            val outName = name.ifBlank { "video.mp4" }.substringBeforeLast('.') + ".mp4"
            if (!VideoRules.mustCompress(src.length(), mime, name) && VideoRules.fitsCap(src.length())) {
                return CompressedVideo(src.readBytes(), "video/mp4", duration, outName)
            }
            for (height in listOf(VideoRules.TARGET_HEIGHT, VideoRules.FALLBACK_HEIGHT, 360)) {
                val dest = File(cacheDir, "vout-${System.nanoTime()}.mp4")
                val ok = transcode(context, src, dest, height)
                if (ok && VideoRules.fitsCap(dest.length())) {
                    val bytes = dest.readBytes()
                    dest.delete()
                    return CompressedVideo(bytes, "video/mp4", duration, outName)
                }
                dest.delete()
            }
            return if (VideoRules.fitsCap(src.length())) {
                CompressedVideo(src.readBytes(), "video/mp4", duration, outName)
            } else {
                null
            }
        } finally {
            src.delete()
        }
    }

    private fun transcode(context: Context, src: File, dest: File, height: Int): Boolean {
        val latch = CountDownLatch(1)
        val ok = AtomicBoolean(false)
        val appContext = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            try {
                val transformer = Transformer.Builder(appContext)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(
                        object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                ok.set(true)
                                latch.countDown()
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                latch.countDown()
                            }
                        },
                    )
                    .build()
                val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(src)))
                    .setEffects(Effects(emptyList(), listOf(Presentation.createForHeight(height))))
                    .build()
                val composition = Composition.Builder(EditedMediaItemSequence(item)).build()
                transformer.start(composition, dest.absolutePath)
            } catch (_: Exception) {
                latch.countDown()
            }
        }
        return latch.await(180, TimeUnit.SECONDS) && ok.get() && dest.isFile && dest.length() > 8
    }
}
