package app.rope.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaRecorder
import android.util.Log
import app.rope.android.data.CallMedia
import app.rope.android.data.CallSignal
import app.rope.android.data.IceRtcPlan
import app.rope.android.data.IceServerSpec
import app.rope.android.data.IceServers
import app.rope.android.net.PinnedClient
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.BuiltinAudioDecoderFactoryFactory
import org.webrtc.BuiltinAudioEncoderFactoryFactory
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
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
import org.webrtc.audio.JavaAudioDeviceModule

class WebRtcSession(
    context: Context,
    iceServers: List<IceServerSpec> = emptyList(),
    private val pinnedFingerprint: String = "",
    hintHost: String? = null,
    private val polite: Boolean = false,
    private val onLocalSignal: (CallSignal) -> Unit,
    private val onIce: (state: String, viaRelay: Boolean) -> Unit,
) {
    private val app = context.applicationContext
    private val plan = IceServers.plan(iceServers, hintHost)
    private val audioDevice: JavaAudioDeviceModule
    private val factory: PeerConnectionFactory
    private var pc: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var callee = polite
    private val pendingIce = mutableListOf<IceCandidate>()
    private var remoteSet = false
    private var localSet = false
    private var makingOffer = false
    private var pendingRemote: CallSignal? = null
    private var viaRelay = false
    private var closed = false

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
            onIce(state.name, viaRelay)
        }

        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
            if (closed) return
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
            receiver.track()?.setEnabled(true)
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            transceiver.receiver.track()?.setEnabled(true)
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) = Unit
        override fun onAddStream(p0: MediaStream) = Unit
        override fun onRemoveStream(p0: MediaStream) = Unit
        override fun onDataChannel(p0: DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit
    }

    init {
        CallAudio.apply(app, true)
        ensureInit(app)
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
            .createPeerConnectionFactory()
        val deps = PeerConnectionDependencies.builder(observer).apply {
            // TURNS uses the VPS self-signed cert. Default WebRTC TLS rejects it.
            setSSLCertificateVerifier(SSLCertificateVerifier { der ->
                val got = runCatching { PinnedClient.fingerprintHex(der) }.getOrDefault("")
                if (pinnedFingerprint.isNotBlank() && !got.equals(pinnedFingerprint, ignoreCase = true)) {
                    Log.i("rope-webrtc", "tls cert $got pin=$pinnedFingerprint — allow TURNS/DTLS")
                }
                true
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
        Log.i(
            "rope-webrtc",
            "pc ice=${plan.servers.flatMap { it.urls }} relay=${plan.forceRelay} creds=${plan.servers.any { !it.username.isNullOrBlank() }}",
        )
    }

    fun createOffer(iceRestart: Boolean = false) {
        if (closed) return
        if (callee && !iceRestart) {
            Log.i("rope-webrtc", "skip offer: this side is callee")
            return
        }
        val state = pc?.signalingState()
        if (!iceRestart && state != null && state != PeerConnection.SignalingState.STABLE) {
            Log.w("rope-webrtc", "skip offer: signaling=$state")
            return
        }
        callee = false
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
                    onIce("FAILED", viaRelay)
                }
            }, desc)
        }, audioConstraints(iceRestart))
    }

    fun prepareCallee() {
        callee = true
    }

    fun restartIce() {
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
                val glare = makingOffer || pc?.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER
                if (glare && !polite) {
                    Log.i("rope-webrtc", "glare: ignore remote offer (impolite)")
                    return
                }
                applyRemoteSdp(signal)
            }
            CallSignal.ANSWER -> {
                if (!localSet) {
                    pendingRemote = signal
                    return
                }
                applyRemoteSdp(signal)
            }
            CallSignal.ICE -> {
                if (signal.candidate.isBlank()) return
                if (CallMedia.isRelayCandidate(signal.candidate)) viaRelay = true
                val mid = signal.sdpMid.trim().ifEmpty { "0" }
                val ice = IceCandidate(mid, signal.sdpMLineIndex.coerceAtLeast(0), signal.candidate)
                if (!remoteSet) pendingIce += ice else pc?.addIceCandidate(ice)
            }
        }
    }

    fun close() {
        closed = true
        try {
            audioTrack?.setEnabled(false)
            audioTrack?.dispose()
            audioSource?.dispose()
            pc?.close()
            pc?.dispose()
            factory.dispose()
            audioDevice.release()
        } catch (e: Exception) {
            Log.w("rope-webrtc", e)
        }
        audioTrack = null
        audioSource = null
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
                                onIce("FAILED", viaRelay)
                            }
                        }, answer)
                    }, audioConstraints())
                }
            }
            override fun onSetFailure(err: String) {
                Log.w("rope-webrtc", "setRemote ${signal.kind}: $err")
                if (signal.kind == CallSignal.OFFER) onIce("FAILED", viaRelay)
            }
        }, desc)
    }

    private fun flushIce() {
        pendingIce.forEach { pc?.addIceCandidate(it) }
        pendingIce.clear()
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

        private fun audioConstraints(iceRestart: Boolean = false) = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
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
