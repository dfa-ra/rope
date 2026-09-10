package app.rope.android.data

/**
 * After the forward picker, open the dest chat with a composer hint so the
 * user can add a comment. Picker no longer sends on tap. Not multi-forward.
 */
object ForwardCommentRules {
    const val TITLE = "Переслать"
    const val DISMISS = "Отменить пересылку"

    fun picking(inChat: Boolean, forwarding: ChatMessage?): Boolean =
        forwarding != null && !inChat

    fun commenting(inChat: Boolean, forwarding: ChatMessage?): Boolean =
        forwarding != null && inChat

    fun hint(preview: String): ComposerHintCopy = ComposerHintCopy(
        kind = ComposerHintKind.FORWARD,
        title = TITLE,
        body = ComposerHintRules.clipBody(preview),
        dismissContentDescription = DISMISS,
    )

    fun sendComment(comment: String): Boolean = comment.trim().isNotEmpty()
}
