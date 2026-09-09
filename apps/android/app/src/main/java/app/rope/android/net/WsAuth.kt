package app.rope.android.net

/**
 * Phase A WSS auth: same three fields as the query, sent on [HEADER].
 * Transcript is still `rope-ws-v1\n<unix>` — not REST `Authorization: Rope`.
 * Phase B (later) drops the query; this slice must keep `sig=` on the URL.
 */
object WsAuth {
    const val HEADER = "X-Rope-Ws-Auth"

    fun value(deviceId: String, ts: String, sig: String): String = "$deviceId.$ts.$sig"
}
