package app.rope.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.rope.android.data.CallMedia
import app.rope.android.data.CallSignal
import app.rope.android.data.IceRtcPlan
import app.rope.android.data.IceServerSpec
import app.rope.android.data.IceServers
import app.rope.android.data.IceUnstick
import app.rope.android.data.VideoCallRules
import app.rope.android.net.PinnedClient
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.BuiltinAudioDecoderFactoryFactory
import org.webrtc.BuiltinAudioEncoderFactoryFactory
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionDependencies
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SSLCertificateVerifier
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSink
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class WebRtcSession(
    context: Context,
    iceServers: List<IceServerSpec> = emptyList(),
    private val pinnedFingerprint: String = "",
    hintHost: String? = null,
    publicIp: String? = null,
    private val polite: Boolean = false,
    private val wantVideo: Boolean = false,
    private val startCamera: Boolean = wantVideo,
    private val onLocalSignal: (CallSignal) -> Unit,
    private val onIce: (state: String, viaRelay: Boolean) -> Unit,
    private val onCameraFailed: () -> Unit = {},
) {
    private val app = context.applicationContext
    private val plan = IceServers.plan(iceServers, hintHost, publicIp)
    private val audioDevice: JavaAudioDeviceModule
    private val factory: PeerConnectionFactory
    private var pc: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private val eglBase: EglBase
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var capturer: VideoCapturer? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var remoteVideo: VideoTrack? = null
    private var localSink: VideoSink? = null
    private var remoteSink: VideoSink? = null
    private var videoWanted = wantVideo
    private var sendCamera = startCamera
    private var callee = polite
    private val pendingIce = mutableListOf<IceCandidate>()
    private var remoteSet = false
    private var localSet = false
    private var makingOffer = false
    private var pendingRemote: CallSignal? = null
    private var viaRelay = false
    private var fellBack = false
    @Volatile
    private var closed = false
    @Volatile
    private var pcReady = false
    @Volatile
    private var micEnabled = true
    private val mainHandler = Handler(Looper.getMainLooper())
    private val fallbackDirect = Runnable {
        if (closed || viaRelay || fellBack) return@Runnable
        Log.w("rope-webrtc", "no relay candidate in ${IceUnstick.NO_PROGRESS_MS}ms — fallback ALL")
        allowDirect()
    }

    private val observer = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            if (closed) return
            if (CallMedia.isRelayCandidate(candidate.sdp)) viaRelay = true
            val mid = candidate.sdpMid?.trim().orEmpty().ifEmpty { "0" }
            onLocalSignal(
                CallSignal(
                    CallSignal.ICE,
                    candidate = candidate.sdp,
                    sdpMid = mid,
                    sdpMLineIndex = candidate.sdpMLineIndex.coerceAtLeast(0),
                ),
            )
        }

        override fun onIceCandidateError(event: IceCandidateErrorEvent) {
            Log.w("rope-webrtc", "ice error ${event.errorCode} ${event.url} ${event.errorText}")
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            if (closed) return
            if (state == PeerConnection.IceConnectionState.CONNECTED ||
                state == PeerConnection.IceConnectionState.COMPLETED
            ) {
                attachRemoteAudio()
            }
            onIce(state.name, viaRelay)
        }

        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
            if (closed) return
            if (state == PeerConnection.PeerConnectionState.CLOSED) return
            onIce(state.name, viaRelay)
        }

        override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) {
            viaRelay = CallMedia.isRelayCandidate(event.local.sdp) || CallMedia.isRelayCandidate(event.remote.sdp)
            pc?.iceConnectionState()?.let { onIceConnectionChange(it) }
        }

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
            Log.i("rope-webrtc", "gather ${state.name} relay=$viaRelay turn=${plan.forceRelay}")
        }

        override fun onAddTrack(receiver: org.webrtc.RtpReceiver, streams: Array<out MediaStream>) {
            enableRemoteTrack(receiver.track())
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            enableRemoteTrack(transceiver.receiver.track())
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) = Unit
        override fun onAddStream(p0: MediaStream) = Unit
        override fun onRemoveStream(p0: MediaStream) = Unit
        override fun onDataChannel(p0: DataChannel) = Unit
        override fun onRenegotiationNeeded() {
            if (closed || !pcReady) return
            val stable = pc?.signalingState() == PeerConnection.SignalingState.STABLE
            if (!VideoCallRules.offerOnRenegotiationNeeded(
                    pcReady = true,
                    callee = callee,
                    signalingStable = stable,
                    makingOffer = makingOffer,
                )
            ) {
                return
            }
            createOffer()
        }
    }

    init {
        CallAudio.apply(app, true)
        ensureInit(app)
        eglBase = EglBase.create()
        audioDevice = JavaAudioDeviceModule.builder(app)
            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .createAudioDeviceModule()
        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDevice)
            .setAudioEncoderFactoryFactory(BuiltinAudioEncoderFactoryFactory())
            .setAudioDecoderFactoryFactory(BuiltinAudioDecoderFactoryFactory())
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, false, false))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        val deps = PeerConnectionDependencies.builder(observer).apply {
            // TURNS uses the VPS self-signed cert. Default WebRTC TLS rejects it.
            setSSLCertificateVerifier(SSLCertificateVerifier { der ->
                val got = runCatching { PinnedClient.fingerprintHex(der) }.getOrDefault("")
                val ok = PinnedClient.tlsPinAllows(got, pinnedFingerprint)
                if (!ok) {
                    Log.w("rope-webrtc", "tls cert $got pin=$pinnedFingerprint — reject")
                }
                ok
            })
        }.createPeerConnectionDependencies()
        val cfg = rtcConfig(plan)
        pc = factory.createPeerConnection(cfg, deps)
            ?: factory.createPeerConnection(cfg, observer)
        pc?.setAudioPlayout(true)
        pc?.setAudioRecording(true)
        val source = factory.createAudioSource(audioSourceConstraints())
        audioSource = source
        val track = factory.createAudioTrack("rope-audio", source)
        track.setEnabled(true)
        audioTrack = track
        val added = runCatching { pc?.addTrack(track, listOf("rope")) }.getOrNull()
        if (added == null) {
            pc?.addTransceiver(
                track,
                RtpTransceiver.RtpTransceiverInit(
                    RtpTransceiver.RtpTransceiverDirection.SEND_RECV,
                    listOf("rope"),
                ),
            )
        }
        pc?.transceivers?.forEach { t ->
            if (t.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO) {
                runCatching { t.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV }
            }
        }
        if (wantVideo) {
            videoWanted = true
            if (sendCamera) {
                val ok = startCameraLocked()
                if (!ok) {
                    sendCamera = false
                    onCameraFailed()
                    reserveVideoTransceiverLocked()
                }
            } else {
                reserveVideoTransceiverLocked()
            }
        }
        Log.i(
            "rope-webrtc",
            "pc ice=${plan.servers.flatMap { it.urls }} relay=${plan.forceRelay} creds=${plan.servers.any { !it.username.isNullOrBlank() }}",
        )
        if (plan.forceRelay) {
            mainHandler.postDelayed(fallbackDirect, IceUnstick.NO_PROGRESS_MS)
        }
        pcReady = true
    }

    fun createOffer(iceRestart: Boolean = false, renegotiate: Boolean = false) {
        if (closed) return
        if (makingOffer && !iceRestart) {
            Log.i("rope-webrtc", "skip offer: already making one")
            return
        }
        if (callee && !iceRestart && !renegotiate) {
            Log.i("rope-webrtc", "skip offer: this side is callee")
            return
        }
        val state = pc?.signalingState()
        if (!iceRestart && state != null && state != PeerConnection.SignalingState.STABLE) {
            Log.w("rope-webrtc", "skip offer: signaling=$state")
            return
        }
        makingOffer = true
        pc?.createOffer(sdpSink { desc ->
            pc?.setLocalDescription(object : SdpObserver by noopSdp {
                override fun onSetSuccess() {
                    localSet = true
                    makingOffer = false
                    onLocalSignal(CallSignal(CallSignal.OFFER, sdp = desc.description))
                    pendingRemote?.let {
                        pendingRemote = null
                        applyRemoteSdp(it)
                    }
                }
                override fun onSetFailure(err: String) {
                    makingOffer = false
                    Log.w("rope-webrtc", "setLocal offer: $err")
                    if (VideoCallRules.sdpErrorFailsIce()) onIce("FAILED", viaRelay)
                }
            }, desc)
        }, offerConstraints(videoWanted, iceRestart))
    }

    fun prepareCallee() {
        callee = true
    }

    fun allowDirect() {
        if (closed || fellBack) return
        fellBack = true
        mainHandler.removeCallbacks(fallbackDirect)
        val cfg = rtcConfig(plan.allowDirect())
        val ok = runCatching { pc?.setConfiguration(cfg) == true }.getOrDefault(false)
        Log.i("rope-webrtc", "setConfiguration ALL=$ok (was relay-only=${plan.forceRelay})")
    }

    fun setMicEnabled(on: Boolean) {
        if (closed) return
        micEnabled = on
        audioTrack?.setEnabled(on)
        runCatching { pc?.setAudioRecording(on) }
    }

    fun eglContext(): EglBase.Context = eglBase.eglBaseContext

    fun attachLocalSink(sink: VideoSink) {
        if (closed) return
        localSink?.let { videoTrack?.removeSink(it) }
        localSink = sink
        videoTrack?.addSink(sink)
    }

    fun attachRemoteSink(sink: VideoSink) {
        if (closed) return
        remoteSink?.let { remoteVideo?.removeSink(it) }
        remoteSink = sink
        remoteVideo?.addSink(sink)
    }

    fun detachLocalSink(sink: VideoSink) {
        if (localSink !== sink) return
        videoTrack?.removeSink(sink)
        localSink = null
    }

    fun detachRemoteSink(sink: VideoSink) {
        if (remoteSink !== sink) return
        remoteVideo?.removeSink(sink)
        remoteSink = null
    }

    fun detachSinks() {
        localSink?.let { videoTrack?.removeSink(it) }
        remoteSink?.let { remoteVideo?.removeSink(it) }
        localSink = null
        remoteSink = null
    }

    fun flipCamera() {
        if (closed) return
        (capturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    fun setCameraEnabled(on: Boolean) {
        if (closed) return
        sendCamera = on
        if (on) {
            val hadTrack = videoTrack != null
            if (videoTrack == null) {
                if (!startCameraLocked()) {
                    sendCamera = false
                    onCameraFailed()
                    return
                }
            }
            videoTrack?.setEnabled(true)
            runCatching {
                capturer?.startCapture(VideoCallRules.WIDTH, VideoCallRules.HEIGHT, VideoCallRules.FPS)
            }
            val stable = pc?.signalingState() == PeerConnection.SignalingState.STABLE
            if (VideoCallRules.explicitOfferOnCameraUnmute(
                    hadLocalTrack = hadTrack,
                    signalingStable = stable,
                    makingOffer = makingOffer,
                )
            ) {
                createOffer(renegotiate = true)
            }
        } else {
            videoTrack?.setEnabled(false)
            runCatching { capturer?.stopCapture() }
        }
    }

    fun restartIce() {
        if (!viaRelay) allowDirect()
        if (callee) return
        pendingRemote = null
        remoteSet = false
        makingOffer = false
        runCatching { pc?.restartIce() }
        createOffer(iceRestart = true)
    }

    fun handleRemote(signal: CallSignal) {
        if (closed) return
        when (signal.kind) {
            CallSignal.OFFER -> {
                if (!signal.wireSafe()) return
                if (VideoCallRules.ignoreRemoteOfferOnGlare(
                        makingOffer = makingOffer,
                        haveLocalOffer = pc?.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER,
                        polite = polite,
                    )
                ) {
                    Log.i("rope-webrtc", "glare: ignore remote offer (impolite)")
                    return
                }
                applyRemoteSdp(signal)
            }
            CallSignal.ANSWER -> {
                if (!signal.wireSafe()) return
                if (!localSet) {
                    pendingRemote = signal
                    return
                }
                applyRemoteSdp(signal)
            }
            CallSignal.ICE -> {
                if (!signal.wireSafe()) return
                if (CallMedia.isRelayCandidate(signal.candidate)) viaRelay = true
                val mid = signal.sdpMid.trim().ifEmpty { "0" }
                val ice = IceCandidate(mid, signal.sdpMLineIndex.coerceAtLeast(0), signal.candidate)
                if (!remoteSet) pendingIce += ice else pc?.addIceCandidate(ice)
            }
        }
    }

    fun close() {
        closed = true
        mainHandler.removeCallbacks(fallbackDirect)
        try {
            detachSinks()
            runCatching { capturer?.stopCapture() }
            capturer?.dispose()
            videoTrack?.setEnabled(false)
            videoTrack?.dispose()
            videoSource?.dispose()
            surfaceHelper?.dispose()
            audioTrack?.setEnabled(false)
            audioTrack?.dispose()
            audioSource?.dispose()
            pc?.close()
            pc?.dispose()
            factory.dispose()
            audioDevice.release()
            eglBase.release()
        } catch (e: Exception) {
            Log.w("rope-webrtc", e)
        }
        audioTrack = null
        audioSource = null
        videoTrack = null
        videoSource = null
        capturer = null
        surfaceHelper = null
        remoteVideo = null
        pc = null
        CallAudio.apply(app, false)
    }

    private fun applyRemoteSdp(signal: CallSignal) {
        val type = if (signal.kind == CallSignal.OFFER) {
            SessionDescription.Type.OFFER
        } else {
            SessionDescription.Type.ANSWER
        }
        val desc = SessionDescription(type, signal.sdp)
        if (signal.kind == CallSignal.OFFER && VideoCallRules.sdpHasVideo(signal.sdp)) {
            videoWanted = true
            if (sendCamera && videoTrack == null && !startCameraLocked()) {
                sendCamera = false
                onCameraFailed()
                reserveVideoTransceiverLocked()
            } else if (!sendCamera) {
                reserveVideoTransceiverLocked()
            }
        }
        pc?.setRemoteDescription(object : SdpObserver by noopSdp {
            override fun onSetSuccess() {
                remoteSet = true
                makingOffer = false
                flushIce()
                if (signal.kind == CallSignal.OFFER) {
                    callee = true
                    pc?.createAnswer(sdpSink { answer ->
                        pc?.setLocalDescription(object : SdpObserver by noopSdp {
                            override fun onSetSuccess() {
                                localSet = true
                                onLocalSignal(CallSignal(CallSignal.ANSWER, sdp = answer.description))
                            }
                            override fun onSetFailure(err: String) {
                                Log.w("rope-webrtc", "setLocal answer: $err")
                                if (VideoCallRules.sdpErrorFailsIce()) onIce("FAILED", viaRelay)
                            }
                        }, answer)
                    }, offerConstraints(VideoCallRules.answerReceivesVideo(videoWanted, signal.sdp)))
                }
            }
            override fun onSetFailure(err: String) {
                Log.w("rope-webrtc", "setRemote ${signal.kind}: $err")
                if (signal.kind == CallSignal.OFFER && VideoCallRules.sdpErrorFailsIce()) {
                    onIce("FAILED", viaRelay)
                }
            }
        }, desc)
    }

    private fun flushIce() {
        pendingIce.forEach { pc?.addIceCandidate(it) }
        pendingIce.clear()
    }

    private fun enableRemoteTrack(track: MediaStreamTrack?) {
        if (track == null) return
        track.setEnabled(true)
        if (track is AudioTrack || track.kind() == MediaStreamTrack.AUDIO_TRACK_KIND) {
            attachRemoteAudio()
            Log.i("rope-webrtc", "remote ${track.kind()} ${track.id()} enabled")
        }
        if (track is VideoTrack || track.kind() == MediaStreamTrack.VIDEO_TRACK_KIND) {
            val vt = track as? VideoTrack ?: return
            remoteVideo = vt
            remoteSink?.let { vt.addSink(it) }
            Log.i("rope-webrtc", "remote video ${vt.id()} enabled")
        }
    }

    private fun startCameraLocked(): Boolean {
        if (closed || videoTrack != null) return videoTrack != null
        return try {
            val enumerator = if (Camera2Enumerator.isSupported(app)) {
                Camera2Enumerator(app)
            } else {
                Camera1Enumerator(false)
            }
            val names = enumerator.deviceNames
            val chosen = names.firstOrNull { enumerator.isFrontFacing(it) }
                ?: names.firstOrNull()
                ?: return false
            val cap = enumerator.createCapturer(chosen, null) ?: return false
            val helper = SurfaceTextureHelper.create("rope-capture", eglBase.eglBaseContext)
            val source = factory.createVideoSource(cap.isScreencast)
            cap.initialize(helper, app, source.capturerObserver)
            cap.startCapture(VideoCallRules.WIDTH, VideoCallRules.HEIGHT, VideoCallRules.FPS)
            val track = factory.createVideoTrack(VideoCallRules.TRACK_ID, source)
            track.setEnabled(true)
            attachLocalVideoLocked(track)
            localSink?.let { track.addSink(it) }
            capturer = cap
            surfaceHelper = helper
            videoSource = source
            videoTrack = track
            videoWanted = true
            Log.i("rope-webrtc", "camera $chosen ${VideoCallRules.WIDTH}x${VideoCallRules.HEIGHT}")
            true
        } catch (e: Exception) {
            Log.w("rope-webrtc", "camera", e)
            false
        }
    }

    private fun reserveVideoTransceiverLocked() {
        if (closed) return
        val hasVideo = pc?.transceivers?.any {
            it.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO
        } == true
        if (hasVideo) return
        pc?.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(
                RtpTransceiver.RtpTransceiverDirection.SEND_RECV,
                listOf(VideoCallRules.STREAM_ID),
            ),
        )
    }

    private fun attachLocalVideoLocked(track: VideoTrack) {
        val existing = pc?.transceivers?.firstOrNull {
            it.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO
        }
        if (existing != null) {
            val replaced = runCatching { existing.sender.setTrack(track, false) }.getOrDefault(false)
            if (!replaced) {
                runCatching { pc?.addTrack(track, listOf(VideoCallRules.STREAM_ID)) }
            }
            runCatching { existing.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV }
        } else {
            val added = runCatching { pc?.addTrack(track, listOf(VideoCallRules.STREAM_ID)) }.getOrNull()
            if (added == null) {
                pc?.addTransceiver(
                    track,
                    RtpTransceiver.RtpTransceiverInit(
                        RtpTransceiver.RtpTransceiverDirection.SEND_RECV,
                        listOf(VideoCallRules.STREAM_ID),
                    ),
                )
            }
        }
        pc?.transceivers?.forEach { t ->
            if (t.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO) {
                runCatching { t.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV }
            }
        }
    }

    private fun attachRemoteAudio() {
        pc?.setAudioPlayout(true)
        pc?.setAudioRecording(micEnabled)
        audioTrack?.setEnabled(micEnabled)
        CallAudio.confirm(app)
    }

    companion object {
        @Volatile
        private var started = false

        fun ensureInit(context: Context) {
            if (started) return
            synchronized(this) {
                if (started) return
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                        .createInitializationOptions(),
                )
                started = true
            }
        }

        fun rtcConfig(plan: IceRtcPlan): PeerConnection.RTCConfiguration {
            val ice = plan.servers.map { spec ->
                val builder = PeerConnection.IceServer.builder(spec.urls)
                val user = spec.username
                val cred = spec.credential
                if (!user.isNullOrBlank() && !cred.isNullOrBlank()) {
                    builder.setUsername(user).setPassword(cred)
                }
                if (spec.insecureTls) {
                    builder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
                }
                val host = spec.hostname?.trim().orEmpty()
                if (host.isNotEmpty()) builder.setHostname(host)
                builder.createIceServer()
            }
            return PeerConnection.RTCConfiguration(ice).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                iceTransportsType = if (plan.forceRelay) {
                    PeerConnection.IceTransportsType.RELAY
                } else {
                    PeerConnection.IceTransportsType.ALL
                }
                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
                enableImplicitRollback = true
                presumeWritableWhenFullyRelayed = true
                surfaceIceCandidatesOnIceTransportTypeChanged = true
            }
        }

        fun rtcConfig(specs: List<IceServerSpec>): PeerConnection.RTCConfiguration =
            rtcConfig(IceServers.plan(specs))

        private fun audioSourceConstraints() = MediaConstraints().apply {
            optional.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            optional.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            optional.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        private fun audioConstraints(iceRestart: Boolean = false) = offerConstraints(false, iceRestart)

        private fun offerConstraints(video: Boolean, iceRestart: Boolean = false) = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", VideoCallRules.offerToReceiveAudio()))
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    VideoCallRules.offerToReceiveVideo(video),
                ),
            )
            if (iceRestart) {
                mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
            }
        }

        private val noopSdp = object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription) = Unit
            override fun onSetSuccess() = Unit
            override fun onCreateFailure(p0: String) = Unit
            override fun onSetFailure(p0: String) = Unit
        }

        private fun sdpSink(ok: (SessionDescription) -> Unit) = object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) = ok(desc)
            override fun onSetSuccess() = Unit
            override fun onCreateFailure(err: String) {
                Log.w("rope-webrtc", err)
            }
            override fun onSetFailure(err: String) {
                Log.w("rope-webrtc", err)
            }
        }
    }
}
