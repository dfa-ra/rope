package app.rope.android.media

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * TextureView sink for in-call video.
 *
 * SurfaceViewRenderer cannot show frames inside Compose: init in `AndroidView.update`
 * runs after attach, so SurfaceHolder.surfaceCreated already fired with no EGL, and
 * the Compose overlay punches a hole the SurfaceView never wins. TextureView composites
 * with the view tree; EGL surface is created when the texture appears, after init.
 */
class CallVideoRenderer(context: Context) :
    TextureView(context),
    VideoSink,
    TextureView.SurfaceTextureListener {

    private val eglRenderer = EglRenderer("rope-tex")
    private var started = false
    private var released = false
    private var surfaceReady = false

    init {
        isOpaque = true
        surfaceTextureListener = this
    }

    @Synchronized
    fun init(sharedContext: EglBase.Context, mirror: Boolean) {
        if (released || started) return
        eglRenderer.init(sharedContext, EglBase.CONFIG_PLAIN, GlRectDrawer())
        eglRenderer.setMirror(mirror)
        started = true
        val texture = surfaceTexture
        if (surfaceReady && texture != null) {
            eglRenderer.createEglSurface(texture)
        }
    }

    override fun onFrame(frame: VideoFrame) {
        if (released || !started) return
        eglRenderer.onFrame(frame)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceReady = true
        if (started && !released) {
            eglRenderer.createEglSurface(surface)
        }
        applyAspect(width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        applyAspect(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        surfaceReady = false
        if (!started || released) return true
        val done = CountDownLatch(1)
        eglRenderer.releaseEglSurface { done.countDown() }
        runCatching { done.await(400, TimeUnit.MILLISECONDS) }
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    @Synchronized
    fun release() {
        if (released) return
        released = true
        started = false
        surfaceReady = false
        runCatching { eglRenderer.release() }
            .onFailure { Log.w("rope-webrtc", "tex release", it) }
    }

    private fun applyAspect(width: Int, height: Int) {
        if (height > 0 && started && !released) {
            eglRenderer.setLayoutAspectRatio(width.toFloat() / height.toFloat())
        }
    }
}
