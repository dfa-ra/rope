package app.rope.android.data

/**
 * Settings toggle so the composer and search fields do not train the IME.
 * Not FCM, not envelope crypto.
 */
object IncognitoKbRules {
    fun enabledFromKv(raw: String?): Boolean = raw == "1"

    fun autoCorrect(incognito: Boolean): Boolean = !incognito

    fun hint(): String =
        "Подсказки клавиатуры не учатся на поле ввода. Не FCM."
}
