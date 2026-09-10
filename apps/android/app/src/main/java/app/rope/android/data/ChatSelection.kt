package app.rope.android.data

/**
 * Telegram-like selection chrome. Title is «Выбрано N»; «Выбрать все»
 * fills the live (search-visible) thread and skips deleted rows. Tapping
 * again while everything live is selected is «Снять всё».
 */
object ChatSelection {
    const val SELECT_ALL = "Выбрать все"
    const val CLEAR_ALL = "Снять всё"

    fun title(count: Int): String = "Выбрано $count"

    fun canSelect(msg: ChatMessage): Boolean = !msg.deleted

    fun selectableIds(messages: List<ChatMessage>): Set<String> =
        messages.filter { canSelect(it) }.map { it.id }.toSet()

    fun showsAction(messages: List<ChatMessage>): Boolean =
        selectableIds(messages).isNotEmpty()

    fun allSelected(selectedIds: Set<String>, messages: List<ChatMessage>): Boolean {
        val ids = selectableIds(messages)
        return ids.isNotEmpty() && selectedIds.containsAll(ids)
    }

    fun actionLabel(selectedIds: Set<String>, messages: List<ChatMessage>): String =
        if (allSelected(selectedIds, messages)) CLEAR_ALL else SELECT_ALL

    fun nextIds(selectedIds: Set<String>, messages: List<ChatMessage>): Set<String> {
        val ids = selectableIds(messages)
        return if (ids.isNotEmpty() && selectedIds.containsAll(ids)) emptySet() else ids
    }
}
