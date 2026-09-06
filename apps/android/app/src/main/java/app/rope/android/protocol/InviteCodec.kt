package app.rope.android.protocol

data class InviteLink(
    val version: Int,
    val host: String,
    val port: Int,
    val serverId: String,
    val fingerprint: String,
    val token: String,
    val displayName: String? = null,
)

object InviteCodec {
    fun parse(url: String): InviteLink {
        require(url.startsWith("rope://join")) { "scheme must be rope://join" }
        val query = url.substringAfter('?', "")
        require(query.isNotEmpty()) { "missing query" }
        val map = linkedMapOf<String, String>()
        for (part in query.split('&')) {
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            map[part.substring(0, eq)] = decode(part.substring(eq + 1))
        }
        val version = map["v"]?.toIntOrNull() ?: 1
        require(version == 1) { "unsupported protocol version $version" }
        return InviteLink(
            version = version,
            host = map["host"] ?: error("missing host"),
            port = map["port"]?.toIntOrNull() ?: error("bad port"),
            serverId = map["sid"] ?: error("missing sid"),
            fingerprint = (map["fp"] ?: error("missing fp")).lowercase(),
            token = map["tok"] ?: error("missing tok"),
            displayName = map["name"],
        )
    }

    fun build(link: InviteLink): String {
        val q = mutableListOf(
            "v=${link.version}",
            "host=${encode(link.host)}",
            "port=${link.port}",
            "sid=${encode(link.serverId)}",
            "fp=${encode(link.fingerprint.lowercase())}",
            "tok=${encode(link.token)}",
        )
        if (!link.displayName.isNullOrBlank()) {
            q += "name=${encode(link.displayName)}"
        }
        return "rope://join?${q.joinToString("&")}"
    }

    fun verify(link: InviteLink, serverId: String, fingerprint: String) {
        require(link.serverId == serverId) { "server id mismatch" }
        require(link.fingerprint.equals(fingerprint, ignoreCase = true)) { "fingerprint mismatch" }
    }

    private fun encode(s: String): String = buildString {
        for (b in s.toByteArray()) {
            val c = b.toInt() and 0xff
            when {
                c in 0x41..0x5a || c in 0x61..0x7a || c in 0x30..0x39 || c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code -> append(c.toChar())
                c == ' '.code -> append('+')
                else -> append("%%%02X".format(c))
            }
        }
    }

    private fun decode(s: String): String {
        val out = ArrayList<Byte>()
        var i = 0
        val raw = s.toByteArray()
        while (i < raw.size) {
            when (raw[i].toInt().toChar()) {
                '+' -> {
                    out.add(' '.code.toByte()); i++
                }
                '%' -> {
                    require(i + 2 < raw.size)
                    out.add(s.substring(i + 1, i + 3).toInt(16).toByte())
                    i += 3
                }
                else -> {
                    out.add(raw[i]); i++
                }
            }
        }
        return String(out.toByteArray())
    }
}
