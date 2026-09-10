package app.rope.android

import app.rope.android.data.CallMedia
import app.rope.android.data.IceServerSpec
import app.rope.android.data.IceServers
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IceServersTest {
    @Test
    fun optStringNullLiteralIsNotACredential() {
        val obj = JSONObject()
            .put("urls", JSONArray().put("turn:vps:3478"))
            .put("username", JSONObject.NULL)
            .put("credential", JSONObject.NULL)
        val parsed = IceServers.parseArray(JSONArray().put(obj))
        assertEquals(1, parsed.size)
        assertNull(parsed[0].username)
        assertNull(parsed[0].credential)

        val viaOptString = IceServers.parse(
            """[{"urls":["turns:vps:443"],"username":"null","credential":"null"}]""",
        )
        assertNull(viaOptString[0].username)
        assertNull(viaOptString[0].credential)
        assertTrue(viaOptString[0].hasTurn)
    }

    @Test
    fun fromInfoIgnoresMissingAndNullIceServers() {
        assertEquals(emptyList<IceServerSpec>(), IceServers.fromInfo(JSONObject().put("server_id", "x")))
        assertEquals(
            emptyList<IceServerSpec>(),
            IceServers.fromInfo(JSONObject().put("ice_servers", JSONObject.NULL)),
        )
        assertNull(IceServers.infoJson(JSONObject().put("server_id", "x")))
        assertNull(IceServers.infoJson(JSONObject().put("ice_servers", JSONObject.NULL)))
        assertEquals(emptyList<IceServerSpec>(), IceServers.parse("null"))
        assertEquals(emptyList<IceServerSpec>(), IceServers.parse(null))
    }

    @Test
    fun fromInfoReadsArrayStringAndSingularUrl() {
        val array = IceServers.fromInfo(
            JSONObject(
                """{"ice_servers":[
                  {"urls":["stun:203.0.113.9:3478"]},
                  {"urls":["turns:203.0.113.9:443?transport=tcp","turn:203.0.113.9:3478?transport=udp"],
                   "username":"1700000000:rope","credential":"abc+/=","hostname":"rope.example"}
                ]}""",
            ),
        )
        assertEquals(2, array.size)
        assertEquals("1700000000:rope", array[1].username)
        assertEquals("abc+/=", array[1].credential)
        assertEquals("rope.example", array[1].hostname)
        assertTrue(array[1].hasTurn)

        val asString = IceServers.fromInfo(
            JSONObject().put("ice_servers", """[{"url":"stun:vps:3478"}]"""),
        )
        assertEquals(listOf("stun:vps:3478"), asString[0].urls)

        val camel = IceServers.fromInfo(
            JSONObject("""{"iceServers":[{"urls":"turn:vps:3478","username":"u","credential":"c"}]}"""),
        )
        assertEquals(listOf("turn:vps:3478"), camel[0].urls)
        assertEquals("u", camel[0].username)
    }

    @Test
    fun resolveKeepsVpsTurnAndDropsGoogleWhenHostKnown() {
        val vps = IceServers.parse(
            """[{"urls":["stun:203.0.113.9:3478","turn:203.0.113.9:3478"],"username":"u","credential":"c"}]""",
        )
        val resolved = IceServers.resolve(vps)
        assertFalse(IceServers.usesPublicStunFallback(resolved))
        assertTrue(resolved.none { spec -> spec.urls.any { it.contains("google") || it.contains("cloudflare") } })
        assertFalse(IceServers.missingTurn(resolved))

        val hinted = IceServers.resolve(emptyList(), "203.0.113.9")
        assertEquals(listOf("stun:203.0.113.9:3478"), hinted.flatMap { it.urls })
        assertFalse(IceServers.usesPublicStunFallback(hinted))

        val fallback = IceServers.resolve(emptyList())
        assertTrue(IceServers.usesPublicStunFallback(fallback))
        assertEquals(CallMedia.STUN_URLS, fallback.flatMap { it.urls })
    }

    @Test
    fun planForcesRelayAndMarksTurnsInsecure() {
        val specs = IceServers.parse(
            """[{"urls":["turns:vps.example:443?transport=tcp","turn:vps.example:3478"],
                "username":"u","credential":"c"}]""",
        )
        val plan = IceServers.plan(specs, "203.0.113.9")
        assertTrue(plan.forceRelay)
        assertTrue(plan.hasTurn)
        assertTrue(plan.servers[0].insecureTls)
        assertEquals("vps.example", plan.servers[0].hostname)
        assertEquals("u", plan.servers[0].username)
        assertTrue(plan.servers[0].urls.any { it.startsWith("turns:203.0.113.9:") })
        assertTrue(plan.servers[0].urls.any { it.startsWith("turn:203.0.113.9:") })
    }

    @Test
    fun planDoesNotForceRelayOnStunOnly() {
        val plan = IceServers.plan(IceServers.parse("""[{"urls":["stun:vps:3478"]}]"""))
        assertFalse(plan.forceRelay)
        assertFalse(plan.servers[0].insecureTls)
        assertNull(plan.servers[0].username)
    }

    @Test
    fun urlHostAndReplaceKeepTransportQuery() {
        assertEquals("vps.example", IceServers.urlHost("turns:vps.example:443?transport=tcp"))
        assertEquals("203.0.113.9", IceServers.urlHost("turn:203.0.113.9:3478?transport=udp"))
        assertEquals("2001:db8::1", IceServers.urlHost("stun:[2001:db8::1]:3478"))
        assertEquals(
            "turns:203.0.113.9:443?transport=tcp",
            IceServers.replaceUrlHost("turns:vps.example:443?transport=tcp", "203.0.113.9"),
        )
        assertTrue(IceServers.looksLikeIp("203.0.113.9"))
        assertFalse(IceServers.looksLikeIp("vps.example"))
        assertTrue(IceServers.isTurnUrl("turns:host:443"))
        assertTrue(IceServers.isTurnsUrl("turns:host:443"))
        assertFalse(IceServers.isTurnsUrl("turn:host:3478"))
    }

    @Test
    fun infoJsonKeepsPublicIpAndPlanAddsIpUrls() {
        val obj = JSONObject(
            """{"ice_servers":[
              {"urls":["turns:vps.example:443?transport=tcp","turn:vps.example:3478"],
               "username":"u","credential":"c","hostname":"vps.example"}
            ],"public_ip":"203.0.113.9"}""",
        )
        val raw = IceServers.infoJson(obj)
        assertEquals("203.0.113.9", IceServers.parsePublicIp(raw))
        assertEquals("vps.example", IceServers.fromInfo(obj)[0].hostname)
        val plan = IceServers.plan(IceServers.parse(raw), "vps.example", IceServers.parsePublicIp(raw))
        assertTrue(plan.forceRelay)
        assertEquals("vps.example", plan.servers[0].hostname)
        assertTrue(plan.servers[0].urls.any { it.startsWith("turns:203.0.113.9:") })
        assertTrue(plan.servers[0].urls.any { it.startsWith("turn:203.0.113.9:") })
    }

    @Test
    fun parseHostnameAndPublicIpFromInfoCache() {
        val obj = JSONObject(
            """{"ice_servers":[
              {"urls":["turns:203.0.113.9:443","turn:203.0.113.9:3478"],
               "username":"u","credential":"c","hostname":"vps.example"}
            ],"public_ip":"203.0.113.9","hostname":"vps.example"}""",
        )
        val raw = IceServers.infoJson(obj)
        assertEquals("203.0.113.9", IceServers.parsePublicIp(raw))
        assertEquals("vps.example", IceServers.parseHostname(raw))
        val fromArray = IceServers.parseHostname(
            """[{"urls":["turn:203.0.113.9:3478"],"hostname":"rope.example"}]""",
        )
        assertEquals("rope.example", fromArray)
        assertNull(IceServers.parseHostname(null))
        assertNull(IceServers.parsePublicIp("[]"))
    }

    @Test
    fun planAddsHostnameUrlsAndTcpWhenIceUrlsAreRawIp() {
        val specs = IceServers.parse(
            """[{"urls":["turns:203.0.113.9:443?transport=tcp","turn:203.0.113.9:3478?transport=udp"],
                "username":"u","credential":"c","hostname":"vps.example"}]""",
        )
        val plan = IceServers.plan(specs, hintHost = "203.0.113.9", publicIp = "203.0.113.9")
        assertTrue(plan.forceRelay)
        assertEquals("vps.example", plan.servers[0].hostname)
        assertTrue(plan.servers[0].urls.any { it.startsWith("turns:vps.example:") })
        assertTrue(plan.servers[0].urls.any { it.startsWith("turn:vps.example:") })
        assertTrue(plan.servers[0].urls.any { it.startsWith("turn:203.0.113.9:") && it.contains("transport=tcp") })
        assertEquals(
            "turn:203.0.113.9:3478?transport=tcp",
            IceServers.tcpVariant("turn:203.0.113.9:3478?transport=udp"),
        )
        assertNull(IceServers.tcpVariant("turns:vps.example:443?transport=tcp"))
    }

    @Test
    fun infoJsonKeepsArrayForCache() {
        val obj = JSONObject(
            """{"ice_servers":[{"urls":["turn:vps:3478"],"username":"u","credential":"c"}]}""",
        )
        val raw = IceServers.infoJson(obj)
        assertNotNull(raw)
        val parsed = IceServers.parse(raw)
        assertEquals("u", parsed[0].username)
        assertEquals(listOf("turn:vps:3478"), parsed[0].urls)
    }

    @Test
    fun shouldRefreshWhenEmptyStaleOrNeverFetched() {
        val turn = """[{"urls":["turn:vps:3478"],"username":"u","credential":"c"}]"""
        val t0 = 1_000_000L
        assertTrue(IceServers.shouldRefresh(0, t0, turn))
        assertTrue(IceServers.shouldRefresh(t0, t0, ""))
        assertTrue(IceServers.shouldRefresh(t0, t0, null))
        assertTrue(IceServers.shouldRefresh(t0, t0, "[]"))
        assertFalse(IceServers.shouldRefresh(t0, t0 + 1_000, turn))
        assertFalse(IceServers.shouldRefresh(t0, t0 + IceServers.ICE_CACHE_MAX_AGE_MS - 1, turn))
        assertTrue(IceServers.shouldRefresh(t0, t0 + IceServers.ICE_CACHE_MAX_AGE_MS, turn))
        assertTrue(IceServers.shouldRefresh(t0, t0 + 60_000, turn))
    }

    @Test
    fun planForceRelayFallsBackToAllViaAllowDirect() {
        val specs = IceServers.parse(
            """[{"urls":["turn:vps.example:3478"],"username":"u","credential":"c"}]""",
        )
        val plan = IceServers.plan(specs)
        assertTrue(plan.forceRelay)
        assertTrue(plan.hasTurn)
        val direct = plan.allowDirect()
        assertFalse(direct.forceRelay)
        assertTrue(direct.hasTurn)
        assertEquals(plan.servers, direct.servers)
        val stunOnly = IceServers.plan(IceServers.parse("""[{"urls":["stun:vps:3478"]}]"""))
        assertFalse(stunOnly.forceRelay)
        assertFalse(stunOnly.allowDirect().forceRelay)
    }

    @Test
    fun expandHostsSkipsPrivateAndCgnat() {
        val specs = IceServers.parse("""[{"urls":["turn:vps.example:3478"]}]""")
        val expanded = IceServers.expandHosts(specs, "10.0.0.8", "192.168.1.1", "100.64.1.2", "vps.example")
        val urls = expanded.flatMap { it.urls }
        assertTrue(urls.none { it.contains("10.0.0.8") })
        assertTrue(urls.none { it.contains("192.168.1.1") })
        assertTrue(urls.none { it.contains("100.64.1.2") })
        assertNull(IceServers.stunHint("10.8.0.1"))
        assertTrue(IceServers.isUnusableIceHost("172.16.4.1"))
        assertFalse(IceServers.isUnusableIceHost("203.0.113.9"))
    }

    @Test
    fun infoJsonKeepsIceTtlAndShouldRefreshUsesIt() {
        val info = JSONObject()
            .put("ice_servers", JSONArray().put(JSONObject().put("urls", JSONArray().put("turn:vps:3478"))))
            .put("ice_ttl_seconds", 10)
        val cached = IceServers.infoJson(info)
        assertNotNull(cached)
        assertEquals(10L, IceServers.parseIceTtlSeconds(cached))
        val t0 = 1_000_000L
        // Half of 10s = 5s, which is below the 30s default cache age.
        assertTrue(IceServers.shouldRefresh(t0, t0 + 5_000, cached))
        assertFalse(IceServers.shouldRefresh(t0, t0 + 4_000, cached))
    }

    @Test
    fun parseArrayDropsNonStunTurnSchemes() {
        val mixed = JSONArray()
            .put("stun:vps:3478")
            .put("turn:vps:3478")
            .put("turns:vps:443?transport=tcp")
            .put("file:///etc/passwd")
            .put("http://evil.example/ice")
            .put("https://evil.example/ice")
            .put("javascript:alert(1)")
            .put("data:text/plain,x")
            .put("ws://vps/ice")
        val parsed = IceServers.parseArray(mixed)
        val urls = parsed.flatMap { it.urls }
        assertEquals(listOf("stun:vps:3478", "turn:vps:3478", "turns:vps:443?transport=tcp"), urls)
        assertFalse(IceServers.allowedUrl("file:foo"))
        assertFalse(IceServers.allowedUrl("http://stun:3478"))
        assertTrue(IceServers.allowedUrl("STUN:vps:3478"))
        assertTrue(IceServers.allowedUrl("TURNS:vps:443"))
    }

    @Test
    fun parseObjectDropsForbiddenUrlsAndKeepsTurn() {
        val obj = JSONObject()
            .put(
                "urls",
                JSONArray()
                    .put("turn:vps:3478")
                    .put("file:///tmp/x")
                    .put("http://203.0.113.9:3478"),
            )
            .put("username", "u")
            .put("credential", "c")
        val parsed = IceServers.parseArray(JSONArray().put(obj))
        assertEquals(1, parsed.size)
        assertEquals(listOf("turn:vps:3478"), parsed[0].urls)
        assertEquals("u", parsed[0].username)
        val onlyBad = IceServers.parseArray(
            JSONArray().put(JSONObject().put("urls", JSONArray().put("data:text/plain,x"))),
        )
        assertTrue(onlyBad.isEmpty())
    }

    @Test
    fun resolveStripsForbiddenUrlsBeforeFallback() {
        val dirty = listOf(
            IceServerSpec(listOf("file:///x", "turn:vps:3478"), username = "u", credential = "c"),
        )
        val resolved = IceServers.resolve(dirty)
        assertEquals(listOf("turn:vps:3478"), resolved.flatMap { it.urls })
        assertFalse(IceServers.missingTurn(resolved))
    }

    @Test
    fun allowedUrlRejectsInteriorWhitespaceAndControl() {
        assertFalse(IceServers.allowedUrl("turn:vps:3478\nhttp://evil"))
        assertFalse(IceServers.allowedUrl("stun:vps:3478\r\nX: y"))
        assertFalse(IceServers.allowedUrl("turn:vps:3478 http://evil"))
        assertFalse(IceServers.allowedUrl("turn:vps:3478\u0000evil"))
        assertTrue(IceServers.allowedUrl("turns:vps.example:443?transport=tcp"))
        val mixed = JSONArray()
            .put("turn:vps:3478")
            .put("turn:vps:3478\nhttp://evil")
            .put("stun:vps:3478\r\nX: y")
        val urls = IceServers.parseArray(mixed).flatMap { it.urls }
        assertEquals(listOf("turn:vps:3478"), urls)
    }
}
