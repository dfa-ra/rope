package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.GroupPhotoRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupPhotoRulesTest {
    @Test
    fun parseRejectsCrLfNul() {
        assertEquals("Фото группы", GroupPhotoRules.TITLE)
        assertFalse(GroupPhotoRules.TITLE.contains("Фото профиля"))
        assertFalse(GroupPhotoRules.HINT.contains('\n'))
        assertFalse(GroupPhotoRules.HINT.contains('\r'))
        assertEquals("/data/user/0/app/files/group-avatar/g1.jpg", GroupPhotoRules.parsePath("/data/user/0/app/files/group-avatar/g1.jpg"))
        assertNull(GroupPhotoRules.parsePath(null))
        assertNull(GroupPhotoRules.parsePath(""))
        assertNull(GroupPhotoRules.parsePath("  "))
        assertNull(GroupPhotoRules.parsePath("/tmp/g.jpg\n"))
        assertNull(GroupPhotoRules.parsePath("\r/tmp/g.jpg"))
        assertNull(GroupPhotoRules.parsePath("/tmp/g.jpg\u0000"))
        assertTrue(GroupPhotoRules.acceptsMime("image/jpeg"))
        assertTrue(GroupPhotoRules.acceptsMime("image/png"))
        assertFalse(GroupPhotoRules.acceptsMime("video/mp4"))
        assertFalse(GroupPhotoRules.acceptsMime("application/octet-stream"))
        assertEquals("Нужно фото", GroupPhotoRules.NEED_PHOTO)
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun keyAndLookupAndCanSet() {
        assertEquals("abc", GroupPhotoRules.key("abc"))
        assertEquals("abc", GroupPhotoRules.key(ChatIds.group("abc")))
        assertNull(GroupPhotoRules.key("abc\n"))
        assertNull(GroupPhotoRules.key("g:abc\r"))
        assertNull(GroupPhotoRules.key(null))
        val photos = mapOf("abc" to "/data/g.jpg")
        assertEquals("/data/g.jpg", GroupPhotoRules.lookup(photos, "abc"))
        assertEquals("/data/g.jpg", GroupPhotoRules.lookup(photos, ChatIds.group("abc")))
        assertNull(GroupPhotoRules.lookup(photos, "other"))
        assertTrue(GroupPhotoRules.canSet(true, "org", "org", "guest"))
        assertFalse(GroupPhotoRules.canSet(true, "mem", "org", "guest"))
        assertTrue(GroupPhotoRules.canSet(true, "mem", "org", "owner"))
        val encoded = GroupPhotoRules.encode(mapOf("abc" to "/data/g.jpg", "bad\n" to "/x"))
        val parsed = GroupPhotoRules.parseMap(encoded)
        assertEquals("/data/g.jpg", parsed["abc"])
        assertFalse(parsed.containsKey("bad\n"))
        assertTrue(GroupPhotoRules.parseMap("not-json").isEmpty())
        assertEquals(6, LocalStore.VERSION)
    }
}
