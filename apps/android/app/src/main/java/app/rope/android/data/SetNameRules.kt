package app.rope.android.data

import app.rope.android.LoginRules

/**
 * Settings **Имя** saves the local display name on this device.
 * Distinct from contact nicks and Go PATCH `/v1/me`. LocalStore stays v6.
 */
object SetNameRules {
    const val LABEL = "Имя"
    const val SAVE = "Сохранить"
    const val HINT = "На этом телефоне. Не ник контакта."

    fun canSave(raw: String, current: String): Boolean {
        if (!LoginRules.isValid(raw)) return false
        return LoginRules.normalize(raw) != LoginRules.normalize(current)
    }
}
