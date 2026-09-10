package app.rope.android.data

data class EmojiCategory(
    val id: String,
    val label: String,
    val icon: String,
    val emojis: List<String>,
)

/** Unicode emoji grid for the composer and the expanded reaction picker. No third-party SDK. */
object EmojiPack {
    val smileys = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃",
        "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙",
        "🥲", "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫",
        "🤔", "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬",
        "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢",
        "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "🥸",
        "😎", "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳",
        "🥺", "😦", "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖",
        "😣", "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬",
        "😈", "👿", "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👽",
        "👾", "🤖", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿",
        "😾",
    )

    val gestures = listOf(
        "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞",
        "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍",
        "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝",
        "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦵", "🦶", "👂", "🦻",
        "👃", "🧠", "👀", "👁️", "👅", "👄", "💋", "🦷", "🦴", "🫂",
    )

    val hearts = listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
        "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "♥️",
        "💌", "💍", "💒", "🌹", "🥀", "🌺", "🌸", "🌼", "🌻", "🌷",
    )

    val animals = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
        "🦁", "🐮", "🐷", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🐔",
        "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺",
        "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜", "🪲",
        "🐢", "🐍", "🦎", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐠",
        "🐟", "🐬", "🐳", "🐋", "🦈", "🐊", "🐅", "🐆", "🦓", "🦍",
        "🐘", "🦛", "🦏", "🐪", "🐫", "🦒", "🦘", "🐃", "🐂", "🐄",
        "🐎", "🐖", "🐏", "🐑", "🦙", "🐐", "🦌", "🐕", "🐈", "🐓",
    )

    val food = listOf(
        "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐",
        "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑",
        "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🧄", "🧅", "🥔",
        "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🥞", "🧇",
        "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕", "🥪", "🌮",
        "🌯", "🥗", "🍝", "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🍤",
        "🍙", "🍚", "🍘", "🍥", "🍦", "🍧", "🍨", "🍩", "🍪", "🎂",
        "🍰", "🧁", "🥧", "🍫", "🍬", "🍭", "🍮", "🍯", "☕", "🍵",
        "🧃", "🥤", "🍶", "🍺", "🍻", "🥂", "🍷", "🥃", "🍸", "🍹",
    )

    val objects = listOf(
        "⌚", "📱", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "📷", "📸", "📹",
        "🎥", "📞", "☎️", "📺", "📻", "⏰", "⌛", "⏳", "💡", "🔦",
        "📡", "🔋", "🔌", "💸", "💵", "💴", "💶", "💷", "💰", "💳",
        "💎", "🔧", "🔨", "🪓", "⛏️", "🔩", "⚙️", "🔫", "💣", "🔪",
        "🛡️", "💊", "💉", "🩹", "🩺", "🔑", "🗝️", "📦", "📫", "📬",
        "📜", "📃", "📄", "📊", "📈", "📉", "📅", "📆", "📌", "📍",
        "✂️", "🖊️", "🖋️", "✒️", "📝", "💼", "📁", "📂", "🔔", "🔕",
        "🎵", "🎶", "🎮", "🎲", "🧩", "🎯", "🏆", "🥇", "🥈", "🥉",
    )

    val symbols = listOf(
        "⭐", "🌟", "✨", "⚡", "🔥", "💥", "💫", "🌈", "☀️", "🌤️",
        "⛅", "🌙", "🌛", "🌜", "✅", "❌", "❓", "❗", "💯", "⚠️",
        "🚫", "♻️", "🔞", "☮️", "✝️", "☪️", "✡️", "☯️", "⚛️", "🆔",
        "▶️", "⏸️", "⏹️", "⏺️", "⏭️", "⏮️", "🔊", "🔇", "💬", "💭",
        "🗯️", "💤", "💢", "💦", "💨", "🕳️", "🎉", "🎊", "🎈", "🎁",
        "🎃", "🎄", "🎗️", "🎟️", "🎫", "🔮", "🧿", "🪄", "🧸", "🎀",
    )

    val categories: List<EmojiCategory> = listOf(
        EmojiCategory("smileys", "Улыбки", "😊", smileys),
        EmojiCategory("gestures", "Жесты", "👋", gestures),
        EmojiCategory("hearts", "Сердца", "❤️", hearts),
        EmojiCategory("animals", "Животные", "🐶", animals),
        EmojiCategory("food", "Еда", "🍕", food),
        EmojiCategory("objects", "Предметы", "💡", objects),
        EmojiCategory("symbols", "Символы", "✨", symbols),
    )

    val all: List<String> = categories.flatMap { it.emojis }.distinct()

    val quickReactions: List<String> = ReactionPayload.EMOJIS

    private val searchAliases = mapOf(
        "улыб" to "smileys",
        "смех" to "smileys",
        "смайл" to "smileys",
        "жест" to "gestures",
        "рук" to "gestures",
        "серд" to "hearts",
        "любов" to "hearts",
        "живот" to "animals",
        "кот" to "animals",
        "еда" to "food",
        "пищ" to "food",
        "предмет" to "objects",
        "вещ" to "objects",
        "символ" to "symbols",
        "знак" to "symbols",
    )

    fun category(id: String): EmojiCategory? = categories.find { it.id == id }

    /**
     * Fail closed on CR/LF/NUL before trim so a newline prefix cannot hit a
     * live category alias. Spaces still trim. Empty after trim still lists all.
     */
    fun search(query: String): List<String> {
        if (query.indexOf('\n') >= 0 || query.indexOf('\r') >= 0 || query.indexOf('\u0000') >= 0) {
            return emptyList()
        }
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        val aliasId = searchAliases.entries.firstOrNull { q.contains(it.key) }?.value
        val fromAlias = aliasId?.let { id -> categories.find { it.id == id }?.emojis }.orEmpty()
        val fromLabel = categories
            .filter { it.id.contains(q) || it.label.lowercase().contains(q) }
            .flatMap { it.emojis }
        val hits = (fromAlias + fromLabel).distinct()
        if (hits.isNotEmpty()) return hits
        return all.filter { it.contains(q) }
    }
}
