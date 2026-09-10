package app.rope.android

import app.rope.android.data.DirectoryDevice
import app.rope.android.data.LocalStore
import app.rope.android.data.PeopleOnlineRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleOnlineRulesTest {
    private val anna = DirectoryDevice("a", "m1", "Аня", ByteArray(0), "", online = true)
    private val bob = DirectoryDevice("b", "m2", "Боб", ByteArray(0), "", online = false)
    private val people = listOf(anna, bob)

    @Test
    fun onlineOnlyDropsOffline() {
        assertEquals(people, PeopleOnlineRules.visible(people, onlineOnly = false))
        assertEquals(listOf(anna), PeopleOnlineRules.visible(people, onlineOnly = true))
        assertTrue(PeopleOnlineRules.visible(emptyList(), onlineOnly = true).isEmpty())
        assertEquals("В сети", PeopleOnlineRules.CHIP)
        assertEquals("Никого нет в сети", PeopleOnlineRules.EMPTY_TITLE)
        assertEquals("Кто выйдет в сеть — появится здесь.", PeopleOnlineRules.EMPTY_BODY)
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
