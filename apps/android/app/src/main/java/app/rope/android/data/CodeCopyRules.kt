package app.rope.android.data

/**
 * Message menu **Копировать код**. Copies the inner ``` fence or `span` payload.
 * Distinct from copy-span (selected TEXT range) and text-fmt render.
 */
object CodeCopyRules {
    const val LABEL = "Копировать код"
    const val NOTICE = "Скопировано"

    fun canCopy(msg: ChatMessage): Boolean = payload(msg) != null

    fun payload(msg: ChatMessage): String? {
        if (msg.deleted) return null
        if (msg.kind != MessageKind.TEXT && msg.kind != MessageKind.GROUP_TEXT) return null
        return payload(msg.text)
    }

    fun payload(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if ('\u0000' in raw) return null
        fenceInner(raw)?.let { return it }
        return tickInner(raw)
    }

    fun fenceInner(raw: String): String? {
        val open = raw.indexOf("```")
        if (open < 0) return null
        val after = open + 3
        val nl = raw.indexOf('\n', after)
        val from = if (nl >= 0) nl + 1 else after
        val close = raw.indexOf("```", from)
        if (close >= 0) {
            return raw.substring(from, close).trimEnd('\n', '\r').takeIf { it.isNotEmpty() }
        }
        val head = raw.trimStart()
        if (!head.startsWith("```")) return null
        return raw.substring(from).trimEnd().takeIf { it.isNotEmpty() }
    }

    fun tickInner(raw: String): String? {
        var i = 0
        while (i < raw.length) {
            if (raw[i] != '`') {
                i++
                continue
            }
            if (i + 2 < raw.length && raw[i + 1] == '`' && raw[i + 2] == '`') {
                i += 3
                continue
            }
            val end = raw.indexOf('`', i + 1)
            if (end < 0) return null
            val inner = raw.substring(i + 1, end)
            if (inner.isNotEmpty()) return inner
            i = end + 1
        }
        return null
    }
}
