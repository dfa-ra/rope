package app.rope.android.data

/**
 * Invite pane **Новая ссылка**. Mints a new one-time QR token.
 * Distinct from clipboard copy and share-sheet. Not envelope crypto.
 */
object InviteRotateRules {
    const val LABEL = "Новая ссылка"
    const val HINT = "Старая ссылка живёт до TTL. Это не копирование и не шаринг."

    fun enabled(busy: Boolean): Boolean = !busy
}
