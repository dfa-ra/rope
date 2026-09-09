package app.rope.android.media

import android.content.Context
import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import app.rope.android.data.VideoCallRules
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
 * with the view tree; EGL surface is created when the texture has a positive size.
 *
 * FillMaxSize AndroidView often reports 0×0 first. Creating EGL then never resizing
 * leaves remote video black while the fixed-size local PIP can still show frames.
 * Do not wrap this view in Compose `graphicsLayer` / FadeIn.
 */
class CallVideoRenderer(context: Context) :
    TextureView(context),
    VideoSink,
    TextureView.SurfaceTextureListener {

    private val eglRenderer = EglRenderer("rope-tex")
    private var started = false
    private var released = false
    private var surfaceReady = false
    private var hasEglSurface = false

    init {
        // Opaque TextureView inside Compose often draws black; video still fills the view.
        isOpaque = false
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
            ensureEglSurface(texture, width, height)
        }
    }

    fun roundCorners(radiusPx: Float) {
        if (radiusPx <= 0f) {
            outlineProvider = null
            clipToOutline = false
            return
        }
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
        clipToOutline = true
    }

    override fun onFrame(frame: VideoFrame) {
        if (released || !started) return
        eglRenderer.onFrame(frame)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val w = right - left
        val h = bottom - top
        if (VideoCallRules.rendererSurfaceReady(w, h) && started && !released) {
            eglRenderer.setLayoutAspectRatio(w.toFloat() / h.toFloat())
        }
        val texture = surfaceTexture
        if (texture != null && surfaceReady) {
            ensureEglSurface(texture, w, h)
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceReady = true
        ensureEglSurface(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        ensureEglSurface(surface, width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        surfaceReady = false
        hasEglSurface = false
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
        hasEglSurface = false
        runCatching { eglRenderer.release() }
            .onFailure { Log.w("rope-webrtc", "tex release", it) }
    }

    @Synchronized
    private fun ensureEglSurface(surface: SurfaceTexture, width: Int, height: Int) {
        if (!started || released) return
        if (!VideoCallRules.rendererSurfaceReady(width, height)) return
        surface.setDefaultBufferSize(width, height)
        if (!hasEglSurface) {
            eglRenderer.createEglSurface(surface)
            hasEglSurface = true
        }
        eglRenderer.setLayoutAspectRatio(width.toFloat() / height.toFloat())
    }
}
