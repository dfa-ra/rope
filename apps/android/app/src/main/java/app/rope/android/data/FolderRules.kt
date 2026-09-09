package app.rope.android.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class FolderMembership { SKIP, INCLUDE, EXCLUDE }

data class CustomFolder(
    val id: String,
    val name: String,
    val sort: Int,
    val include: List<String> = emptyList(),
    val exclude: List<String> = emptyList(),
)

data class FolderSnap(
    val v: Int = FolderRules.VERSION,
    val selected: String = FolderRules.ALL_ID,
    val hiddenBuiltins: List<String> = emptyList(),
    val custom: List<CustomFolder> = emptyList(),
)

data class FolderChip(
    val id: String,
    val title: String,
    val builtin: Boolean,
    val edit: Boolean = false,
)

/**
 * Local Telegram-like chat folders. kv JSON only — no Go, no DeviceBackup, no schema bump.
 */
object FolderRules {
    const val KV_KEY = "chat_folders"
    const val VERSION = 1
    const val ALL_ID = "all"
    const val UNREAD_ID = "unread"
    const val EDIT_ID = "edit"
    const val MAX_CUSTOM = 10
    const val NAME_MAX = 24
    const val ALL_TITLE = "Все"
    const val UNREAD_TITLE = "Непрочитанные"
    const val EDIT_TITLE = "✎"
    const val ADD_TO_FOLDER = "В папку"
    const val TOO_MANY = "Слишком много папок"
    const val DEVICE_ONLY = "Папки только на этом телефоне."
    const val UNREAD_EMPTY = "Нет непрочитанных"
    const val CUSTOM_EMPTY = "Нет чатов в папке"
    const val HIDE = "Скрыть"
    const val RENAME = "Переименовать"
    const val DELETE = "Удалить"
    const val EDIT_CHATS = "Изменить чаты"

    fun defaultSnap(): FolderSnap = FolderSnap()

    fun parse(raw: String?): FolderSnap {
        if (raw.isNullOrBlank()) return defaultSnap()
        return try {
            val o = JSONObject(raw)
            val hidden = jsonStrings(o.optJSONArray("hidden_builtins"))
                .filter { it == UNREAD_ID }
            val custom = parseCustom(o.optJSONArray("custom"))
            sanitize(
                FolderSnap(
                    v = o.optInt("v", VERSION),
                    selected = o.optString("selected").ifBlank { ALL_ID },
                    hiddenBuiltins = hidden,
                    custom = custom,
                ),
            )
        } catch (_: Exception) {
            defaultSnap()
        }
    }

    fun encode(snap: FolderSnap): String {
        val o = JSONObject()
            .put("v", VERSION)
            .put("selected", snap.selected)
        val hidden = JSONArray()
        snap.hiddenBuiltins.forEach { hidden.put(it) }
        o.put("hidden_builtins", hidden)
        val custom = JSONArray()
        snap.custom.sortedBy { it.sort }.forEachIndexed { i, f ->
            custom.put(
                JSONObject()
                    .put("id", f.id)
                    .put("name", f.name)
                    .put("sort", i)
                    .put("include", stringArray(f.include))
                    .put("exclude", stringArray(f.exclude)),
            )
        }
        o.put("custom", custom)
        return o.toString()
    }

    fun sanitize(snap: FolderSnap): FolderSnap {
        val custom = snap.custom
            .mapIndexed { i, f -> f.copy(sort = i, name = normalizeName(f.name) ?: f.name.trim()) }
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .take(MAX_CUSTOM)
        val hidden = snap.hiddenBuiltins.filter { it == UNREAD_ID }.distinct()
        val selected = when {
            snap.selected == ALL_ID -> ALL_ID
            snap.selected == UNREAD_ID && UNREAD_ID !in hidden -> UNREAD_ID
            custom.any { it.id == snap.selected } -> snap.selected
            else -> ALL_ID
        }
        return FolderSnap(VERSION, selected, hidden, custom)
    }

    fun member(chatId: String, folderId: String, snap: FolderSnap, unread: Int): Boolean {
        return when (folderId) {
            ALL_ID -> true
            UNREAD_ID -> unread > 0
            else -> {
                val folder = snap.custom.find { it.id == folderId } ?: return false
                chatId in folder.include && chatId !in folder.exclude
            }
        }
    }

    fun apply(
        base: List<Conversation>,
        selected: String,
        snap: FolderSnap = FolderSnap(selected = selected),
        mode: ChatListMode = ChatListMode.ALL,
    ): List<Conversation> {
        if (mode != ChatListMode.ALL) return base
        val id = sanitize(snap.copy(selected = selected)).selected
        return base.filter { member(it.id, id, snap, it.unread) }
    }

    fun chips(snap: FolderSnap): List<FolderChip> {
        val out = mutableListOf(FolderChip(ALL_ID, ALL_TITLE, builtin = true))
        if (UNREAD_ID !in snap.hiddenBuiltins) {
            out += FolderChip(UNREAD_ID, UNREAD_TITLE, builtin = true)
        }
        out += snap.custom.sortedBy { it.sort }.map { FolderChip(it.id, it.name, builtin = false) }
        out += FolderChip(EDIT_ID, EDIT_TITLE, builtin = false, edit = true)
        return out
    }

    fun chipUnread(conversations: List<Conversation>, folderId: String, snap: FolderSnap): Int =
        conversations.filter { member(it.id, folderId, snap, it.unread) }.sumOf { it.unread.coerceAtLeast(0) }

    fun chipBadgeKind(conversations: List<Conversation>, folderId: String, snap: FolderSnap): UnreadBadgeKind {
        val hits = conversations.filter { member(it.id, folderId, snap, it.unread) && it.unread > 0 }
        if (hits.isEmpty()) return UnreadBadgeKind.NONE
        return if (hits.any { !it.muted }) UnreadBadgeKind.ACCENT else UnreadBadgeKind.MUTED
    }

    fun idleTitle(folderId: String): String? = when {
        folderId == UNREAD_ID -> UNREAD_EMPTY
        folderId != ALL_ID && folderId != EDIT_ID -> CUSTOM_EMPTY
        else -> null
    }

    fun normalizeName(raw: String): String? {
        val n = raw.trim().replace(Regex("\\s+"), " ")
        if (n.isEmpty()) return null
        return n.take(NAME_MAX)
    }

    fun addCustom(snap: FolderSnap, name: String, id: String = UUID.randomUUID().toString()): Pair<FolderSnap, String?> {
        if (snap.custom.size >= MAX_CUSTOM) return snap to TOO_MANY
        val n = normalizeName(name) ?: return snap to "Введите название"
        if (snap.custom.any { it.name.equals(n, ignoreCase = true) }) return snap to "Такое имя уже есть"
        val folder = CustomFolder(id, n, snap.custom.size, emptyList(), emptyList())
        return sanitize(snap.copy(custom = snap.custom + folder, selected = id)) to null
    }

    fun renameCustom(snap: FolderSnap, id: String, name: String): Pair<FolderSnap, String?> {
        val n = normalizeName(name) ?: return snap to "Введите название"
        if (snap.custom.any { it.id != id && it.name.equals(n, ignoreCase = true) }) {
            return snap to "Такое имя уже есть"
        }
        val custom = snap.custom.map { if (it.id == id) it.copy(name = n) else it }
        return sanitize(snap.copy(custom = custom)) to null
    }

    fun deleteCustom(snap: FolderSnap, id: String): FolderSnap =
        sanitize(snap.copy(custom = snap.custom.filter { it.id != id }))

    fun moveCustom(snap: FolderSnap, id: String, delta: Int): FolderSnap {
        val list = snap.custom.sortedBy { it.sort }.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return snap
        val item = list.removeAt(i)
        list.add(j, item)
        return sanitize(snap.copy(custom = list))
    }

    fun hideBuiltin(snap: FolderSnap, id: String): FolderSnap {
        if (id != UNREAD_ID) return snap
        return sanitize(snap.copy(hiddenBuiltins = (snap.hiddenBuiltins + id).distinct()))
    }

    fun showBuiltin(snap: FolderSnap, id: String): FolderSnap {
        if (id != UNREAD_ID) return snap
        return sanitize(snap.copy(hiddenBuiltins = snap.hiddenBuiltins.filter { it != id }))
    }

    fun select(snap: FolderSnap, id: String): FolderSnap = sanitize(snap.copy(selected = id))

    fun membership(folder: CustomFolder, chatId: String): FolderMembership = when {
        chatId in folder.exclude -> FolderMembership.EXCLUDE
        chatId in folder.include -> FolderMembership.INCLUDE
        else -> FolderMembership.SKIP
    }

    fun cycleMembership(folder: CustomFolder, chatId: String): CustomFolder {
        val include = folder.include.toMutableList()
        val exclude = folder.exclude.toMutableList()
        when (membership(folder, chatId)) {
            FolderMembership.SKIP -> {
                if (chatId !in include) include += chatId
                exclude.remove(chatId)
            }
            FolderMembership.INCLUDE -> {
                include.remove(chatId)
                if (chatId !in exclude) exclude += chatId
            }
            FolderMembership.EXCLUDE -> {
                include.remove(chatId)
                exclude.remove(chatId)
            }
        }
        return folder.copy(include = include, exclude = exclude)
    }

    fun cycleChat(snap: FolderSnap, folderId: String, chatId: String): FolderSnap {
        val custom = snap.custom.map { if (it.id == folderId) cycleMembership(it, chatId) else it }
        return sanitize(snap.copy(custom = custom))
    }

    fun setChatFolders(snap: FolderSnap, chatId: String, folderIds: Set<String>): FolderSnap {
        val custom = snap.custom.map { folder ->
            val include = folder.include.toMutableList()
            val exclude = folder.exclude.toMutableList()
            if (folder.id in folderIds) {
                if (chatId !in include) include += chatId
                exclude.remove(chatId)
            } else {
                include.remove(chatId)
            }
            folder.copy(include = include, exclude = exclude)
        }
        return sanitize(snap.copy(custom = custom))
    }

    fun foldersContaining(snap: FolderSnap, chatId: String): Set<String> =
        snap.custom.filter { chatId in it.include && chatId !in it.exclude }.map { it.id }.toSet()

    private fun parseCustom(arr: JSONArray?): List<CustomFolder> {
        if (arr == null) return emptyList()
        val out = mutableListOf<CustomFolder>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").trim()
            val name = normalizeName(o.optString("name")).orEmpty()
            if (id.isEmpty() || name.isEmpty()) continue
            out += CustomFolder(
                id = id,
                name = name,
                sort = o.optInt("sort", out.size),
                include = jsonStrings(o.optJSONArray("include")),
                exclude = jsonStrings(o.optJSONArray("exclude")),
            )
        }
        return out
    }

    private fun jsonStrings(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i).trim()
            if (s.isNotEmpty() && s != "null") out += s
        }
        return out.distinct()
    }

    private fun stringArray(values: List<String>): JSONArray {
        val a = JSONArray()
        values.distinct().forEach { a.put(it) }
        return a
    }
}
