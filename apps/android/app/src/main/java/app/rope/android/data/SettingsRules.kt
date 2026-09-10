package app.rope.android.data

/**
 * Copy and formatting for the Settings screen.
 * Notifications stay on the live WSS; this is not FCM.
 */
object SettingsRules {
    /**
     * Fail closed on CR/LF/NUL before trim so a newline prefix cannot become
     * a live Settings fingerprint or server-id hex. Spaces still trim.
     * Empty after trim still drops. Not PinnedClient TLS compare.
     */
    fun pinHex(raw: String): String? {
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        return raw.trim().takeIf { it.isNotEmpty() }
    }

    fun formatHexGroups(hex: String): String {
        val text = pinHex(hex) ?: return ""
        val clean = text.lowercase().filter { it in '0'..'9' || it in 'a'..'f' }
        if (clean.isEmpty()) return text
        return clean.chunked(4).joinToString(" ")
    }

    fun formatEndpoint(host: String, port: Int, useTls: Boolean): String {
        val scheme = if (useTls) "https" else "http"
        return "$scheme://$host:$port"
    }

    fun roleLabel(role: String?): String = when {
        RoleRules.isOwner(role) -> "owner"
        role.isNullOrBlank() -> "гость"
        else -> role.lowercase()
    }

    fun notificationsHint(): String =
        "Уведомления с вашего VPS по живому соединению. Google FCM нет. Отдельный чат — без звука в списке. Входящий звонок всё равно звонит."

    fun linkPreviewsHint(): String =
        "Карточка собирается на этом телефоне. Сервер ссылку не видит. Сайт видит ваш IP, как в браузере."

    fun appearanceHint(): String =
        "Тёмная или светлая. Цвета логотипа не меняются."

    fun serverPinHint(useTls: Boolean): String =
        if (useTls) {
            "Отпечаток TLS привязан к этому телефону. Сервер не видит переписку."
        } else {
            "Debug HTTP. В релизе нужен TLS и отпечаток."
        }

    fun aboutBody(): String =
        "Приватный self-hosted мессенджер. Ключи на телефоне, релей видит только шифротекст."

    fun copyFingerprintValue(hex: String): String {
        val text = pinHex(hex) ?: return ""
        return text.lowercase().filter { it in '0'..'9' || it in 'a'..'f' }
    }
}
