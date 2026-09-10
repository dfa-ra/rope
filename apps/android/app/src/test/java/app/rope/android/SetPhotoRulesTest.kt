package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SetPhotoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetPhotoRulesTest {
    @Test
    fun parseRejectsCrLfNul() {
        assertEquals("Фото профиля", SetPhotoRules.TITLE)
        assertFalse(SetPhotoRules.HINT.contains('\n'))
        assertFalse(SetPhotoRules.HINT.contains('\r'))
        assertEquals("/data/user/0/app/files/avatar/me.jpg", SetPhotoRules.parse("/data/user/0/app/files/avatar/me.jpg"))
        assertNull(SetPhotoRules.parse(null))
        assertNull(SetPhotoRules.parse(""))
        assertNull(SetPhotoRules.parse("  "))
        assertNull(SetPhotoRules.parse("/tmp/me.jpg\n"))
        assertNull(SetPhotoRules.parse("\r/tmp/me.jpg"))
        assertNull(SetPhotoRules.parse("/tmp/me.jpg\u0000"))
        assertTrue(SetPhotoRules.acceptsMime("image/jpeg"))
        assertTrue(SetPhotoRules.acceptsMime("image/png"))
        assertFalse(SetPhotoRules.acceptsMime("video/mp4"))
        assertFalse(SetPhotoRules.acceptsMime("application/octet-stream"))
        assertEquals("Нужно фото", SetPhotoRules.NEED_PHOTO)
        assertEquals(6, LocalStore.VERSION)
    }
}
