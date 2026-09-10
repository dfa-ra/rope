package app.rope.android.data

/**
 * Telegram «Скрыть отправителя»: forward as a copy with no `ff`.
 * Distinct from auto-hiding «Переслано от» on your own originals.
 */
object ForwardCopyRules {
    const val CHIP = "Скрыть отправителя"

    fun stampName(src: ChatMessage, fallback: String, hideSender: Boolean): String? =
        if (hideSender) null else ForwardRules.originName(src, fallback)

    fun packsFf(from: String?): Boolean = !JsonIds.optional(from).isNullOrBlank()
}
