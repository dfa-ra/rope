package app.rope.android

import app.rope.android.data.AttachContactRules
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachContactRulesTest {
    private fun device(
        id: String,
        name: String = id.take(4),
        member: String = "m-$id",
    ) = DirectoryDevice(id, member, name, ByteArray(0), "", false)

    @Test
    fun labelsAndVersion() {
        assertEquals("Контакт", AttachContactRules.LABEL)
        assertEquals("Контакт", AttachContactRules.TITLE)
        assertEquals("Нет контактов", AttachContactRules.EMPTY)
        assertEquals("Когда появятся люди, их можно будет отправить сюда.", AttachContactRules.EMPTY_BODY)
        assertEquals("Нельзя отправить этот контакт.", AttachContactRules.REJECT)
        assertEquals("контакт", AttachContactRules.FALLBACK_NAME)
        assertFalse(AttachContactRules.EMPTY.contains('\n'))
        assertFalse(AttachContactRules.REJECT.contains('\n'))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun pickPeersNotSelfOrSaved() {
        assertTrue(AttachContactRules.canPick("peer-1", "me"))
        assertTrue(AttachContactRules.canPick("  peer-1  ", "me"))
        assertFalse(AttachContactRules.canPick(null, "me"))
        assertFalse(AttachContactRules.canPick("", "me"))
        assertFalse(AttachContactRules.canPick("  ", "me"))
        assertFalse(AttachContactRules.canPick("me", "me"))
        assertFalse(AttachContactRules.canPick("ME", "me"))
        assertFalse(AttachContactRules.canPick(SavedMessagesRules.ID, "me"))
        assertTrue(AttachContactRules.canPick("peer-1", null))
        assertNull(AttachContactRules.cleanId("bad\nid"))
        assertNull(AttachContactRules.cleanId("bad\rid"))
        assertNull(AttachContactRules.cleanId("bad\u0000id"))
        assertFalse(AttachContactRules.canPick("bad\nid", "me"))
        assertNull(AttachContactRules.cleanId("x".repeat(AttachContactRules.MAX_ID + 1)))
    }

    @Test
    fun sendIntoOpenThreadIncludingSaved() {
        assertTrue(AttachContactRules.canSendInto("peer-2", null))
        assertTrue(AttachContactRules.canSendInto(SavedMessagesRules.ID, null))
        assertTrue(AttachContactRules.canSendInto(null, "g-uuid"))
        assertTrue(AttachContactRules.canSendInto("peer-1", "g-uuid"))
        assertFalse(AttachContactRules.canSendInto(null, null))
        assertFalse(AttachContactRules.canSendInto("", ""))
        assertFalse(AttachContactRules.canSendInto("bad\nid", null))
        assertFalse(AttachContactRules.canSendInto(null, "g\nid"))
    }

    @Test
    fun candidatesSkipSelfAndSavedThenSort() {
        val self = device("me", "Я")
        val bob = device("bob", "Боб")
        val anna = device("anna", "Анна")
        val saved = device(SavedMessagesRules.ID, "Избранное")
        val crlf = device("x\ny", "Нет")
        val out = AttachContactRules.candidates(listOf(self, bob, anna, saved, crlf), "me")
        assertEquals(listOf("anna", "bob"), out.map { it.deviceId })
        val picked = AttachContactRules.pick(listOf(self, bob, anna), "bob", "me")
        assertNotNull(picked)
        assertEquals("bob", picked!!.deviceId)
        assertNull(AttachContactRules.pick(listOf(self, bob), "me", "me"))
        assertNull(AttachContactRules.pick(listOf(bob), SavedMessagesRules.ID, "me"))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun bodyIsNameThenIdNoCr() {
        assertEquals("Анна\ndev-1", AttachContactRules.body("  Анна  ", "dev-1"))
        assertEquals("контакт\ndev-1", AttachContactRules.body("  ", "dev-1"))
        assertEquals("контакт\ndev-1", AttachContactRules.body(null, "dev-1"))
        assertEquals("Анна", AttachContactRules.displayName(" Анна "))
        assertEquals("контакт", AttachContactRules.displayName(""))
        assertEquals("A B", AttachContactRules.displayName("A\nB"))
        assertNull(AttachContactRules.body("Анна", "bad\nid"))
        assertNull(AttachContactRules.body("Анна", SavedMessagesRules.ID))
        val body = AttachContactRules.body("Анна", "dev-1")!!
        assertFalse(body.contains('\r'))
        assertFalse(body.contains('\u0000'))
    }
}
