package app.rope.android

import app.rope.android.data.EmojiBubbleRules
import app.rope.android.data.EmojiPack
import app.rope.android.data.MessageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiBubbleRulesTest {
    @Test
    fun oneToThreeEmojiScaleAndFourStayNormal() {
        assertEquals(1, EmojiBubbleRules.count("👍"))
        assertEquals(2, EmojiBubbleRules.count("👍😂"))
        assertEquals(3, EmojiBubbleRules.count("👍😂❤️"))
        assertNull(EmojiBubbleRules.count("👍😂❤️🎉"))
        assertEquals(EmojiBubbleRules.SP_ONE, EmojiBubbleRules.fontSp(1))
        assertEquals(EmojiBubbleRules.SP_TWO, EmojiBubbleRules.fontSp(2))
        assertEquals(EmojiBubbleRules.SP_THREE, EmojiBubbleRules.fontSp(3))
    }

    @Test
    fun trimsButRejectsLettersSpacesAndMixed() {
        assertEquals(1, EmojiBubbleRules.count("  ❤️  "))
        assertNull(EmojiBubbleRules.count("👍 ок"))
        assertNull(EmojiBubbleRules.count("привет"))
        assertNull(EmojiBubbleRules.count("👍!"))
        assertNull(EmojiBubbleRules.count(""))
        assertNull(EmojiBubbleRules.count("   "))
        assertNull(EmojiBubbleRules.count("1"))
        assertNull(EmojiBubbleRules.count("@Аня"))
    }

    @Test
    fun zwjSkinToneAndFlagsCountAsOne() {
        assertEquals(1, EmojiBubbleRules.count("👋🏻"))
        assertEquals(1, EmojiBubbleRules.count("❤️"))
        assertEquals(1, EmojiBubbleRules.count("👨‍👩‍👧"))
        assertEquals(2, EmojiBubbleRules.count("👋🏻👍"))
    }

    @Test
    fun packEmojisAreSingleClustersAndApplyOnlyToTextKinds() {
        EmojiPack.all.forEach { emoji ->
            assertEquals(emoji, 1, EmojiBubbleRules.count(emoji))
        }
        assertTrue(EmojiBubbleRules.applies(MessageKind.TEXT, "🎉"))
        assertTrue(EmojiBubbleRules.applies(MessageKind.GROUP_TEXT, "🎉"))
        assertFalse(EmojiBubbleRules.applies(MessageKind.IMAGE, "🎉"))
        assertFalse(EmojiBubbleRules.applies(MessageKind.VOICE, "🎉"))
        assertFalse(EmojiBubbleRules.applies(MessageKind.TEXT, "привет"))
    }
}
