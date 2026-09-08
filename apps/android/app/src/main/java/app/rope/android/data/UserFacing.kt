package app.rope.android.data

/** Short Russian copy for chrome. Never surface stack traces or WebRTC internals. */
object UserFacing {
    const val GENERIC = "Что-то пошло не так"
    const val NO_PATH = "Нет пути — звонок через чат"
    const val NO_NETWORK = "Нет сети"
    const val FILE_TOO_BIG = "Файл больше 25 МБ"
    const val LOGIN = "Придумайте логин: 2–24 символа, буквы/цифры/_ . -"

    fun of(error: Throwable?): String = of(error?.message ?: error?.toString())

    fun of(raw: String?): String {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty()) return GENERIC
        if (t == LOGIN || t == FILE_TOO_BIG || t == NO_NETWORK || t == NO_PATH || t == GENERIC) return t
        if (looksLikeStackDump(t)) return GENERIC
        val lower = t.lowercase()
        return when {
            "25 мб" in lower || "25mb" in lower || "файл больше" in lower -> FILE_TOO_BIG
            "webrtc" in lower || "ice " in lower || lower.startsWith("ice") ||
                "turn" in lower || "нет пути" in lower || "sdp" in lower -> NO_PATH
            "unknownhost" in lower || "connectexception" in lower || "sockettimeout" in lower ||
                "failed to connect" in lower || "unable to resolve" in lower ||
                "нет сети" in lower || "not connected" in lower || "no server" in lower ||
                "offline" in lower || "network" in lower -> NO_NETWORK
            "fingerprint" in lower || "mismatch" in lower -> "Сервер не совпал"
            looksLikeExceptionName(t) -> GENERIC
            isHumanCopy(t) -> t
            t.length > 80 -> GENERIC
            else -> t
        }
    }

    private fun isHumanCopy(t: String): Boolean {
        if (t.length > 80) return false
        if (looksLikeStackDump(t) || looksLikeExceptionName(t)) return false
        val cyrillic = t.any { it in '\u0400'..'\u04FF' }
        return cyrillic && !t.contains('\n')
    }

    private fun looksLikeStackDump(t: String): Boolean =
        t.contains('\n') && (t.contains("at ") || t.contains("Exception") || t.contains("\tat "))

    private fun looksLikeExceptionName(t: String): Boolean {
        if (t.contains("Exception:") || t.contains("Error:")) return true
        if (t.contains("java.") || t.contains("kotlin.") || t.contains("org.webrtc")) return true
        return false
    }
}
