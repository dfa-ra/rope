package app.rope.android.data

/**
 * Telegram-like «В контакты» on a 1:1 profile. Opens the system insert
 * sheet; Rope does not write the address book itself. LocalStore stays v6.
 */
object AddContactRules {
    const val LABEL = "В контакты"
    const val MIME = "vnd.android.cursor.dir/raw_contact"
    const val EXTRA_NAME = "name"

    fun show(saved: Boolean, name: String?): Boolean =
        !saved && !name?.trim().isNullOrEmpty()

    fun name(displayName: String?): String = displayName?.trim().orEmpty()
}
