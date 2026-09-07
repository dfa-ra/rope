package app.rope.android.media

import android.content.Context
import android.util.Log
import app.rope.android.data.CallMedia
import app.rope.android.data.CallSignal
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class WebRtcSession(
    context: Context,
    private val onLocalSignal: (CallSignal) -> Unit,
    private val onMedia: (String) -> Unit,
) {
    private val app = context.applicationContext
    private val factory: PeerConnectionFactory
    private var pc: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var callee = false
    private val pendingIce = mutableListOf<IceCandidate>()
    private var remoteSet = false
    private var haveRemoteSdp = false

    private val observer = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            onLocalSignal(
                CallSignal(
                    CallSignal.ICE,
                    candidate = candidate.sdp,
                    sdpMid = candidate.sdpMid.orEmpty(),
                    sdpMLineIndex = candidate.sdpMLineIndex,
                ),
            )
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            onMedia(CallMedia.label(state.name, state == PeerConnection.IceConnectionState.FAILED))
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) = Unit
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) = Unit
        override fun onAddStream(p0: MediaStream) = Unit
        override fun onRemoveStream(p0: MediaStream) = Unit
        override fun onDataChannel(p0: DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(p0: org.webrtc.RtpReceiver, p1: Array<out MediaStream>) = Unit
    }

    init {
        ensureInit(app)
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        pc = factory.createPeerConnection(rtcConfig(), observer)
        val source = factory.createAudioSource(MediaConstraints())
        audioSource = source
        val track = factory.createAudioTrack("rope-audio", source)
        track.setEnabled(true)
        audioTrack = track
        pc?.addTrack(track, listOf("rope"))
    }

    fun createOffer() {
        callee = false
        pc?.createOffer(sdpSink { desc ->
            pc?.setLocalDescription(noopSdp, desc)
            onLocalSignal(CallSignal(CallSignal.OFFER, sdp = desc.description))
        }, audioConstraints())
    }

    fun prepareCallee() {
        callee = true
    }

    fun handleRemote(signal: CallSignal) {
        when (signal.kind) {
            CallSignal.OFFER -> {
                if (haveRemoteSdp) return
                haveRemoteSdp = true
                val desc = SessionDescription(SessionDescription.Type.OFFER, signal.sdp)
                pc?.setRemoteDescription(object : SdpObserver by noopSdp {
                    override fun onSetSuccess() {
                        remoteSet = true
                        flushIce()
                        if (callee) {
                            pc?.createAnswer(sdpSink { answer ->
                                pc?.setLocalDescription(noopSdp, answer)
                                onLocalSignal(CallSignal(CallSignal.ANSWER, sdp = answer.description))
                            }, audioConstraints())
                        }
                    }
                }, desc)
            }
            CallSignal.ANSWER -> {
                if (haveRemoteSdp) return
                haveRemoteSdp = true
                val desc = SessionDescription(SessionDescription.Type.ANSWER, signal.sdp)
                pc?.setRemoteDescription(object : SdpObserver by noopSdp {
                    override fun onSetSuccess() {
                        remoteSet = true
                        flushIce()
                    }
                }, desc)
            }
            CallSignal.ICE -> {
                if (signal.candidate.isBlank()) return
                val ice = IceCandidate(signal.sdpMid, signal.sdpMLineIndex, signal.candidate)
                if (!remoteSet) pendingIce += ice else pc?.addIceCandidate(ice)
            }
        }
    }

    fun close() {
        try {
            audioTrack?.setEnabled(false)
            audioTrack?.dispose()
            audioSource?.dispose()
            pc?.close()
            pc?.dispose()
            factory.dispose()
        } catch (e: Exception) {
            Log.w("rope-webrtc", e)
        }
        audioTrack = null
        audioSource = null
        pc = null
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

        private fun rtcConfig(): PeerConnection.RTCConfiguration {
            val ice = CallMedia.STUN_URLS.map { PeerConnection.IceServer.builder(it).createIceServer() }
            return PeerConnection.RTCConfiguration(ice).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }
        }

        private fun audioConstraints() = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
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
