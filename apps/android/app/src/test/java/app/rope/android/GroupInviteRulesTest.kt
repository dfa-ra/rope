package app.rope.android

import app.rope.android.data.GroupInviteRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupInviteRulesTest {
    @Test
    fun ownerOnlyAndVersion() {
        assertEquals("Пригласить", GroupInviteRules.LABEL)
        assertTrue(GroupInviteRules.canShow("owner"))
        assertTrue(GroupInviteRules.canShow("OWNER"))
        assertFalse(GroupInviteRules.canShow("member"))
        assertFalse(GroupInviteRules.canShow(null))
        assertFalse(GroupInviteRules.LABEL.contains('\n'))
        assertFalse(GroupInviteRules.LABEL.contains("FCM", ignoreCase = true))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }
}
