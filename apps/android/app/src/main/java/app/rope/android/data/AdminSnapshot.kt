package app.rope.android.data

import org.json.JSONObject

data class AdminCard(
    val label: String,
    val value: String,
    val hint: String = "",
)

data class AdminSnapshot(
    val cards: List<AdminCard>,
    val raw: String,
) {
    companion object {
        fun from(obj: JSONObject): AdminSnapshot {
            fun n(key: String) = obj.opt(key)?.toString() ?: "—"
            val bytes = obj.optLong("object_bytes")
            val cards = listOf(
                AdminCard("Версия ядра", n("version"), "protocol ${n("protocol_version")}"),
                AdminCard("Участники", n("member_count")),
                AdminCard("Устройства", n("device_count"), "в сети ${n("online_devices")}"),
                AdminCard("Почта", n("mailbox_count"), "зашифрованные конверты"),
                AdminCard("Объекты", n("object_count"), formatBytes(bytes)),
                AdminCard("Группы", n("group_count")),
                AdminCard("Лимит файла", formatBytes(obj.optLong("max_object_bytes"))),
                AdminCard("Listen", n("listen")),
                AdminCard("TURN", turnValue(obj), turnHint(obj)),
            )
            return AdminSnapshot(cards, obj.toString(2))
        }

        fun turnValue(obj: JSONObject): String {
            val running = obj.optBoolean("turn_running")
            val configured = obj.optBoolean("ice_enabled")
            val err = obj.optString("turn_error").trim()
            val allocKnown = obj.has("turn_allocate_ok")
            val alloc = obj.optBoolean("turn_allocate_ok")
            return when {
                running && allocKnown && !alloc -> "allocate нет"
                running -> "работает"
                configured && err.isNotEmpty() -> "не слушает"
                configured -> "настроен"
                else -> "нет"
            }
        }

        fun turnHint(obj: JSONObject): String {
            val err = obj.optString("turn_error").trim()
            val turn = obj.opt("turn_port")?.toString() ?: "3478"
            val turns = obj.opt("turns_port")?.toString().orEmpty()
            val listen = obj.optString("turn_listen").trim()
            val ext = obj.optString("turn_external_ip").trim()
            val relayed = obj.optString("turn_relayed_ip").trim()
            if (err.isNotEmpty() && !obj.optBoolean("turn_running")) {
                return err
            }
            val bits = mutableListOf("stun/turn $turn")
            if (turns.isNotEmpty() && turns != "0") bits += "turns $turns"
            if (listen.isNotEmpty()) bits += "listen $listen"
            if (ext.isNotEmpty()) bits += ext.removePrefix("external-ip=")
            if (relayed.isNotEmpty()) bits += "relay $relayed"
            if (err.isNotEmpty()) bits += err
            return if (bits.isEmpty()) {
                "обновите ядро, чтобы звонки шли через сервер"
            } else {
                bits.joinToString(" · ")
            }
        }

        fun formatBytes(n: Long): String = when {
            n <= 0L -> "0 B"
            n < 1024 -> "$n B"
            n < 1024 * 1024 -> "${n / 1024} КБ"
            else -> "${"%.1f".format(n / (1024.0 * 1024.0))} МБ"
        }
    }
}
