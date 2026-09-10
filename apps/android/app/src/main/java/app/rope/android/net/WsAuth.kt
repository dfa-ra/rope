package app.rope.android.net

/**
 * Phase A WSS auth: same three fields as the query, sent on [HEADER].
 * Transcript is still `rope-ws-v1\n<unix>` — not REST `Authorization: Rope`.
 * Phase B (later) drops the query; this slice must keep `sig=` on the URL.
 */
object WsAuth {
    const val HEADER = "X-Rope-Ws-Auth"

    /**
     * Header value is device_id.ts.sig. Empty if a part has whitespace or
     * control so OkHttp cannot split the header. Query sig= still sent.
     */
    fun value(deviceId: String, ts: String, sig: String): String {
        if (!wirePart(deviceId) || !wirePart(ts) || !wirePart(sig)) return ""
        return "$deviceId.$ts.$sig"
    }

    fun wirePart(s: String): Boolean {
        if (s.isEmpty()) return false
        return s.none { it.isWhitespace() || it.isISOControl() }
    }
}
