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
                AdminCard(
                    "TURN",
                    if (obj.optBoolean("ice_enabled")) "на этом VPS" else "нет",
                    if (obj.optBoolean("ice_enabled")) {
                        "stun/turn ${n("turn_port")} · turns ${n("turns_port")}"
                    } else {
                        "обновите ядро, чтобы звонки шли через сервер"
                    },
                ),
            )
            return AdminSnapshot(cards, obj.toString(2))
        }

        fun formatBytes(n: Long): String = when {
            n <= 0L -> "0 B"
            n < 1024 -> "$n B"
            n < 1024 * 1024 -> "${n / 1024} КБ"
            else -> "${"%.1f".format(n / (1024.0 * 1024.0))} МБ"
        }
    }
}
