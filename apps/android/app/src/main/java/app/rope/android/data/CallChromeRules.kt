package app.rope.android.data

/**
 * Telegram-style in-call chrome: round icon-only controls. Labels exist for
 * TalkBack (`a11y`) only — never as wrapping text under mute/speaker/camera/hangup.
 */
enum class CallControlKind {
    MUTE,
    SPEAKER,
    CAMERA,
    FLIP,
    HANGUP,
    ACCEPT,
    REJECT,
}

data class CallControlSpec(
    val kind: CallControlKind,
    val a11y: String,
    /** Visible caption under the icon. Always null — icon-only chrome. */
    val visibleCaption: String? = null,
)

object CallChromeRules {
    fun ringingControls(): List<CallControlSpec> = listOf(
        CallControlSpec(CallControlKind.REJECT, a11y = "Отклонить"),
        CallControlSpec(CallControlKind.ACCEPT, a11y = "Ответить"),
    )

    fun showFlip(camMuted: Boolean, rtcReady: Boolean): Boolean = !camMuted && rtcReady

    fun inCallControls(
        video: Boolean,
        micMuted: Boolean = false,
        speakerOn: Boolean = false,
        camMuted: Boolean = false,
        showFlip: Boolean = video && !camMuted,
    ): List<CallControlSpec> {
        val mute = CallControlSpec(CallControlKind.MUTE, a11y = muteA11y(micMuted))
        val hangup = CallControlSpec(CallControlKind.HANGUP, a11y = "Завершить")
        return if (video) {
            buildList {
                add(mute)
                add(hangup)
                add(CallControlSpec(CallControlKind.CAMERA, a11y = cameraA11y(camMuted)))
                if (showFlip) add(CallControlSpec(CallControlKind.FLIP, a11y = "Сменить камеру"))
            }
        } else {
            listOf(
                mute,
                hangup,
                CallControlSpec(CallControlKind.SPEAKER, a11y = speakerA11y(speakerOn)),
            )
        }
    }

    fun muteA11y(muted: Boolean): String = if (muted) "Микрофон выкл" else "Микрофон"

    fun speakerA11y(on: Boolean): String = if (on) "Громкая связь вкл" else "Громкая связь"

    fun cameraA11y(muted: Boolean): String = if (muted) "Камера выкл" else "Камера"

    fun showVisibleCaption(kind: CallControlKind): Boolean = when (kind) {
        CallControlKind.MUTE,
        CallControlKind.SPEAKER,
        CallControlKind.CAMERA,
        CallControlKind.FLIP,
        CallControlKind.HANGUP,
        CallControlKind.ACCEPT,
        CallControlKind.REJECT -> false
    }

    fun iconOnly(spec: CallControlSpec): Boolean =
        spec.visibleCaption.isNullOrBlank() && !showVisibleCaption(spec.kind)
}
