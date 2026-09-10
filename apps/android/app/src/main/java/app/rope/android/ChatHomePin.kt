package app.rope.android

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import app.rope.android.data.ChatHomeRules
import app.rope.android.data.Conversation
import app.rope.android.data.GroupChatUx
import app.rope.android.data.SavedMessagesRules

/** Asks the launcher to pin a chat shortcut. API 26+ (minSdk). */
object ChatHomePin {
    fun request(context: Context, chat: Conversation): Boolean {
        if (Build.VERSION.SDK_INT < 26) return false
        val id = ChatHomeRules.sanitizeId(chat.id) ?: return false
        val sm = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!sm.isRequestPinShortcutSupported) return false
        val launch = Intent(context, MainActivity::class.java).apply {
            action = ChatHomeRules.ACTION
            putExtra(ChatHomeRules.EXTRA_CHAT_ID, id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val info = ShortcutInfo.Builder(context, ChatHomeRules.shortcutId(id))
            .setShortLabel(ChatHomeRules.shortLabel(chat.title))
            .setLongLabel(ChatHomeRules.longLabel(chat.title))
            .setIcon(Icon.createWithBitmap(avatar(chat)))
            .setIntent(launch)
            .build()
        return runCatching { sm.requestPinShortcut(info, null) }.getOrDefault(false)
    }

    internal fun avatar(chat: Conversation): Bitmap {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GroupChatUx.senderColorArgb(chat.id, chat.title) or 0xFF000000.toInt()
        }
        val r = size / 2f
        canvas.drawCircle(r, r, r, bg)
        val letter = when {
            SavedMessagesRules.isSaved(chat.id) -> "И"
            chat.isGroup -> "Г"
            else -> chat.title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        }
        val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            textSize = if (letter.length == 1) 44f else 36f
            typeface = Typeface.DEFAULT_BOLD
        }
        val y = r - (fg.descent() + fg.ascent()) / 2f
        canvas.drawText(letter, r, y, fg)
        return bmp
    }
}
