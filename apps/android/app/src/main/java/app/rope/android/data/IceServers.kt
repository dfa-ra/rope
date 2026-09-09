package app.rope.android.data

import org.json.JSONArray
import org.json.JSONObject

data class IceServerSpec(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,
    val hostname: String? = null,
) {
    val hasTurn: Boolean
        get() = urls.any { IceServers.isTurnUrl(it) }
}

/** Inputs for PeerConnection.RTCConfiguration — unit-testable without WebRTC. */
data class IceRtcServer(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,
    val hostname: String? = null,
    val insecureTls: Boolean = false,
)

data class IceRtcPlan(
    val servers: List<IceRtcServer>,
    val forceRelay: Boolean,
) {
    val hasTurn: Boolean
        get() = servers.any { spec -> spec.urls.any { IceServers.isTurnUrl(it) } }

    /** Stuck RELAY-only ICE → host/srflx (`IceTransportsType.ALL`). */
    fun allowDirect(): IceRtcPlan = copy(forceRelay = false)
}

object IceServers {
    /** TURN REST creds expire; treat empty or aged cache as must-fetch. */
    const val ICE_CACHE_MAX_AGE_MS = 30_000L

    // Optional /v1/info fields the TURN-server agent can add later (do not edit Go here):
    // - ice_servers[].hostname : TLS SNI when urls use a raw IP
    // - public_ip : second set of turn/turns URLs if DNS for public_host fails on the phone
    fun parse(raw: String?): List<IceServerSpec> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text.equals("null", ignoreCase = true)) return emptyList()
        return try {
            val trimmed = text.trim()
            if (trimmed.startsWith("[")) {
                parseArray(JSONArray(trimmed))
            } else {
                parseArray(iceArray(JSONObject(trimmed)))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * True when GET /v1/info must run: never-fetched, empty payload, or TURN json older
     * than [ICE_CACHE_MAX_AGE_MS]. Call start always fetches; this only decides retries
     * when the GET fails and the local cache might be unusable.
     */
    fun shouldRefresh(
        cachedAtMs: Long,
        nowMs: Long,
        cachedJson: String?,
    ): Boolean {
        if (cachedAtMs <= 0L) return true
        if (parse(cachedJson).isEmpty()) return true
        val ttlMs = parseIceTtlSeconds(cachedJson)?.times(1000L)
        val limit = when {
            ttlMs == null -> ICE_CACHE_MAX_AGE_MS
            ttlMs <= 0L -> 0L
            else -> minOf(ICE_CACHE_MAX_AGE_MS, ttlMs / 2)
        }
        return nowMs - cachedAtMs >= limit
    }

    /** Seconds from `/v1/info` `ice_ttl_seconds` (HMAC lifetime). */
    fun parseIceTtlSeconds(raw: String?): Long? {
        val text = raw?.trim().orEmpty()
        if (!text.startsWith("{")) return null
        return try {
            val obj = JSONObject(text)
            if (!obj.has("ice_ttl_seconds")) return null
            val v = obj.optLong("ice_ttl_seconds", -1L)
            if (v < 0L) null else v
        } catch (_: Exception) {
            null
        }
    }

    fun fromInfo(obj: JSONObject): List<IceServerSpec> = parseArray(iceArray(obj))

    /** Cached /v1/info ICE payload. Keeps top-level `public_ip` / `hostname` when present. */
    fun infoJson(obj: JSONObject): String? {
        val arr = iceArray(obj) ?: return null
        if (arr.length() == 0) return null
        val ip = jsonText(obj, "public_ip") ?: jsonText(obj, "publicIp")
        val host = jsonText(obj, "hostname") ?: jsonText(obj, "public_host") ?: jsonText(obj, "publicHost")
        val ttl = if (obj.has("ice_ttl_seconds")) obj.optLong("ice_ttl_seconds", -1L) else -1L
        if (ip == null && host == null && ttl < 0L) return arr.toString()
        val out = JSONObject().put("ice_servers", arr)
        if (ip != null) out.put("public_ip", ip)
        if (host != null) out.put("hostname", host)
        if (ttl >= 0L) out.put("ice_ttl_seconds", ttl)
        return out.toString()
    }

    fun parsePublicIp(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (!text.startsWith("{")) return null
        return try {
            val obj = JSONObject(text)
            jsonText(obj, "public_ip") ?: jsonText(obj, "publicIp")
        } catch (_: Exception) {
            null
        }
    }

    /** Top-level hostname / public_host, else first ice_servers[].hostname. */
    fun parseHostname(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text.equals("null", ignoreCase = true)) return null
        return try {
            when {
                text.startsWith("{") -> {
                    val obj = JSONObject(text)
                    jsonText(obj, "hostname")
                        ?: jsonText(obj, "public_host")
                        ?: jsonText(obj, "publicHost")
                        ?: parseArray(iceArray(obj)).firstNotNullOfOrNull { JsonIds.optional(it.hostname) }
                }
                text.startsWith("[") -> parse(text).firstNotNullOfOrNull { JsonIds.optional(it.hostname) }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun iceArray(obj: JSONObject?): JSONArray? {
        if (obj == null) return null
        val raw = when {
            obj.has("ice_servers") -> obj.opt("ice_servers")
            obj.has("iceServers") -> obj.opt("iceServers")
            else -> null
        }
        return asArray(raw)
    }

    fun parseArray(arr: JSONArray?): List<IceServerSpec> {
        if (arr == null) return emptyList()
        val out = mutableListOf<IceServerSpec>()
        for (i in 0 until arr.length()) {
            val item = arr.opt(i) ?: continue
            if (item === JSONObject.NULL) continue
            if (item is String) {
                val url = item.trim()
                if (allowedUrl(url)) {
                    out += IceServerSpec(listOf(url))
                }
                continue
            }
            if (item !is JSONObject) continue
            val urls = urlsOf(item)
            if (urls.isEmpty()) continue
            out += IceServerSpec(
                urls = urls,
                username = jsonText(item, "username"),
                credential = jsonText(item, "credential"),
                hostname = jsonText(item, "hostname"),
            )
        }
        return out
    }

    fun resolve(serverProvided: List<IceServerSpec>, hintHost: String? = null): List<IceServerSpec> {
        val cleaned = serverProvided.map { spec ->
            spec.copy(
                urls = spec.urls.map { it.trim() }.filter { allowedUrl(it) },
                username = JsonIds.optional(spec.username),
                credential = JsonIds.optional(spec.credential),
                hostname = JsonIds.optional(spec.hostname),
            )
        }.filter { it.urls.isNotEmpty() }
        if (cleaned.isNotEmpty()) return cleaned
        // IceAtRest unwrap miss is empty JSON. Prefer the VPS host; never
        // phone Google/Cloudflare STUN (IP leak + often blocked in Russia).
        stunHint(hintHost)?.let { return listOf(it) }
        return emptyList()
    }

    fun plan(
        serverProvided: List<IceServerSpec>,
        hintHost: String? = null,
        publicIp: String? = null,
    ): IceRtcPlan {
        val resolved = resolve(serverProvided, hintHost)
        val expanded = expandHosts(resolved, hintHost, publicIp).map { spec ->
            spec.copy(urls = withExtraTcp(spec.urls))
        }
        val servers = expanded.map { spec ->
            val turns = spec.urls.any { isTurnsUrl(it) }
            IceRtcServer(
                urls = spec.urls,
                username = spec.username,
                credential = spec.credential,
                hostname = JsonIds.optional(spec.hostname) ?: tlsHostname(spec, hintHost),
                insecureTls = turns,
            )
        }
        return IceRtcPlan(servers = servers, forceRelay = servers.any { spec -> spec.urls.any { isTurnUrl(it) } })
    }

    fun expandHosts(specs: List<IceServerSpec>, vararg altHosts: String?): List<IceServerSpec> {
        return specs.map { spec ->
            val hosts = (altHosts.toList() + spec.hostname)
                .mapNotNull { JsonIds.optional(it) }
                .filter { !isUnusableIceHost(it) }
                .distinct()
            if (hosts.isEmpty()) return@map spec
            val extra = mutableListOf<String>()
            for (host in hosts) {
                for (url in spec.urls) {
                    val current = urlHost(url) ?: continue
                    if (current.equals(host, ignoreCase = true)) continue
                    val alt = replaceUrlHost(url, host)
                    if (alt != null && alt !in spec.urls && alt !in extra) extra += alt
                }
            }
            if (extra.isEmpty()) spec else spec.copy(urls = spec.urls + extra)
        }
    }

    /** Prefer existing UDP; also gather TURN/TURNS over TCP if allocate-UDP fails. */
    fun withExtraTcp(urls: List<String>): List<String> {
        val out = urls.toMutableList()
        for (url in urls) {
            val tcp = tcpVariant(url) ?: continue
            if (tcp !in out) out += tcp
        }
        return out
    }

    fun tcpVariant(url: String): String? {
        if (!isTurnUrl(url)) return null
        val raw = url.trim()
        if (raw.lowercase().contains("transport=tcp")) return null
        val base = raw.substringBefore('?')
        val query = raw.substringAfter('?', "")
        val kept = query.split('&').filter { part ->
            part.isNotEmpty() && !part.startsWith("transport=", ignoreCase = true)
        }
        val next = (kept + "transport=tcp").joinToString("&")
        return "$base?$next"
    }

    fun tlsHostname(spec: IceServerSpec, hintHost: String? = null): String? {
        val fromJsonHint = JsonIds.optional(hintHost)
        val urlHost = spec.urls.firstNotNullOfOrNull { urlHost(it) }
        val ipUrl = spec.urls.any { host -> urlHost(host)?.let { looksLikeIp(it) } == true }
        return when {
            ipUrl && fromJsonHint != null && !looksLikeIp(fromJsonHint) -> fromJsonHint
            urlHost != null && !looksLikeIp(urlHost) -> urlHost
            fromJsonHint != null && !looksLikeIp(fromJsonHint) -> fromJsonHint
            else -> urlHost
        }
    }

    fun missingTurn(resolved: List<IceServerSpec>): Boolean = resolved.none { it.hasTurn }

    /** RFC1918 / loopback / link-local / CGNAT — same policy as server ice.go. */
    fun isUnusableIceHost(host: String): Boolean {
        val h = host.trim().removePrefix("[").removeSuffix("]")
        if (h.isEmpty() || h.equals("localhost", ignoreCase = true) || h == "::1") return true
        if (h == "127.0.0.1" || h.startsWith("127.")) return true
        if (h.startsWith("169.254.")) return true
        if (h.startsWith("10.")) return true
        if (h.startsWith("192.168.")) return true
        if (h.startsWith("172.")) {
            val second = h.substringAfter('.').substringBefore('.').toIntOrNull() ?: return false
            if (second in 16..31) return true
        }
        if (h.startsWith("100.")) {
            val second = h.substringAfter('.').substringBefore('.').toIntOrNull() ?: return false
            if (second in 64..127) return true
        }
        return false
    }

    fun stunHint(host: String?): IceServerSpec? {
        val raw = JsonIds.optional(host) ?: return null
        if (isUnusableIceHost(raw)) return null
        val h = if (raw.contains(":") && !raw.startsWith("[")) "[$raw]" else raw
        return IceServerSpec(listOf("stun:$h:3478"))
    }

    fun fallbackStun(): List<IceServerSpec> =
        CallMedia.STUN_URLS.map { IceServerSpec(listOf(it)) }

    fun usesPublicStunFallback(resolved: List<IceServerSpec>): Boolean {
        val urls = resolved.flatMap { it.urls }
        return urls.isNotEmpty() && urls.all { it in CallMedia.STUN_URLS }
    }

    fun isTurnUrl(url: String): Boolean {
        val u = url.trim().lowercase()
        return u.startsWith("turn:") || u.startsWith("turns:")
    }

    fun isTurnsUrl(url: String): Boolean = url.trim().lowercase().startsWith("turns:")

    fun urlHost(url: String): String? {
        val rest = url.trim().substringAfter(':', "").removePrefix("//").substringBefore('?')
        if (rest.isEmpty() || rest.equals("null", ignoreCase = true)) return null
        if (rest.startsWith("[")) {
            val inside = rest.substringAfter('[').substringBefore(']')
            return JsonIds.optional(inside)
        }
        val host = rest.substringBeforeLast(':')
        return JsonIds.optional(host)
    }

    fun replaceUrlHost(url: String, newHost: String): String? {
        val raw = url.trim()
        val scheme = raw.substringBefore(':', "")
        if (scheme.isEmpty() || scheme.equals(raw, ignoreCase = true)) return null
        val after = raw.substringAfter(':')
        val query = if (after.contains('?')) "?" + after.substringAfter('?') else ""
        val hostport = after.removePrefix("//").substringBefore('?')
        val port = if (hostport.startsWith("[")) {
            hostport.substringAfter(']', "").removePrefix(":")
        } else if (hostport.contains(':')) {
            hostport.substringAfterLast(':')
        } else {
            ""
        }
        val h = if (newHost.contains(":") && !newHost.startsWith("[")) "[$newHost]" else newHost
        val hp = if (port.isNotEmpty()) "$h:$port" else h
        return "$scheme:$hp$query"
    }

    fun looksLikeIp(host: String): Boolean {
        val h = host.trim().removePrefix("[").removeSuffix("]")
        if (h.contains(':')) return h.all { it.isDigit() || it == ':' || it == '.' }
        val parts = h.split('.')
        return parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
    }

    fun jsonText(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val raw = obj.opt(key) ?: return null
        if (raw === JSONObject.NULL) return null
        return JsonIds.optional(raw.toString())
    }

    private fun asArray(raw: Any?): JSONArray? = when {
        raw == null || raw === JSONObject.NULL -> null
        raw is JSONArray -> raw
        raw is JSONObject -> JSONArray().put(raw)
        raw is String -> {
            val text = raw.trim()
            if (text.isEmpty() || text.equals("null", ignoreCase = true)) null
            else try {
                if (text.startsWith("[")) JSONArray(text)
                else if (text.startsWith("{")) JSONArray().put(JSONObject(text))
                else null
            } catch (_: Exception) {
                null
            }
        }
        else -> null
    }

    private fun urlsOf(obj: JSONObject): List<String> {
        val raw = when {
            obj.has("urls") -> obj.opt("urls")
            obj.has("url") -> obj.opt("url")
            else -> null
        }
        val values = when (raw) {
            null, JSONObject.NULL -> emptyList()
            is JSONArray -> buildList {
                for (i in 0 until raw.length()) {
                    val item = raw.opt(i) ?: continue
                    if (item === JSONObject.NULL) continue
                    add(item.toString())
                }
            }
            is String -> listOf(raw)
            else -> emptyList()
        }
        return values.map { it.trim() }.filter { allowedUrl(it) }
    }

    /** PeerConnection only accepts STUN/TURN. Drop file/http/data/javascript and anything else. */
    fun allowedUrl(url: String): Boolean {
        val raw = url.trim()
        if (raw.isEmpty() || raw.equals("null", ignoreCase = true)) return false
        val scheme = raw.substringBefore(':', missingDelimiterValue = "").lowercase()
        return scheme == "stun" || scheme == "turn" || scheme == "turns"
    }
}
