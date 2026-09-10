package app.rope.android.data

/**
 * Telegram-like autoplay of in-thread video when the bubble is on screen.
 * Regular video starts muted; video notes keep their own sound. Kv only —
 * no LocalStore bump.
 */
object AutoplayRules {
    const val KEY = "video_autoplay"
    const val LABEL = "Автовоспроизведение"
    const val HINT =
        "Видео в чате крутится без звука, когда оно на экране. Кружки — со звуком. Нажмите ▶ чтобы включить звук."

    fun stored(raw: String?): Boolean = raw != "0"

    fun write(enabled: Boolean): String = if (enabled) "1" else "0"

    fun startOnVisible(enabled: Boolean, hasPath: Boolean): Boolean = enabled && hasPath

    fun startMuted(kind: MessageKind): Boolean = kind == MessageKind.VIDEO
}
