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
}
