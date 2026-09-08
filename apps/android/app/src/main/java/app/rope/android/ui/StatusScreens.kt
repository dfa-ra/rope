package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.rope.android.BuildConfig
import app.rope.android.RopeShapes
import app.rope.android.UiState
import app.rope.android.data.AdminSnapshot
import app.rope.android.data.CallLink
import app.rope.android.data.RoleRules
import app.rope.android.data.ThemeMode

@Composable
fun StatusPane(
    state: UiState,
    onUpdateApp: () -> Unit,
    onUpgradeCore: (String, String) -> Unit,
    onHome: () -> Unit = {},
) {
    var sshPassword by remember { mutableStateOf("") }
    var sshKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    val host = state.profile?.host.orEmpty()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Сервер и обновления", style = MaterialTheme.typography.titleLarge)
        FadeIn(0) {
            Text(
                "Приложение ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            if (state.theme == ThemeMode.DARK) {
                "Тема: тёмная · иконка солнца в шапке включает светлую"
            } else {
                "Тема: светлая · иконка луны в шапке включает тёмную"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val role = state.profile?.role
        val owner = RoleRules.canShowAdminCards(role)
        val cards = if (owner) state.admin?.cards.orEmpty() else emptyList()
        if (state.profile != null) {
            val iceLine = CallLink.infoStatusLine(state.profile.iceServersJson)
            val iceBad = iceLine.contains("нет")
            FadeIn(130) {
                SectionCard {
                    Text("ice_servers", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        iceLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (iceBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (cards.isEmpty()) {
            FadeIn(140) {
                SectionCard {
                    if (state.statusText.isBlank() && owner) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            RopeLogoMark(size = 56.dp, animate = true, loop = true)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Text(
                        state.statusText.ifBlank {
                            if (owner) "Статус сервера ещё не загружен." else "Вы гость. Приложение обновляется здесь, без прав owner."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!owner && host.isNotBlank()) {
                        Text(
                            "Подключение: $host:${state.profile?.port ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            cards.forEachIndexed { index, card ->
                FadeIn(0) {
                    SectionCard {
                        val turnBad = card.label == "TURN" && (card.value == "нет" || card.value == "не слушает")
                        Text(card.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            card.value,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (turnBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                        if (card.hint.isNotBlank()) {
                            Text(
                                card.hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (turnBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        GlowButton(
            when {
                state.pendingApkPath != null -> "Повторить установку APK"
                state.appUpdateAvailable && state.latestAppVersion.isNotBlank() ->
                    "Обновить приложение ${state.latestAppVersion}"
                else -> "Обновить приложение"
            },
            onUpdateApp,
            Modifier.fillMaxWidth(),
            enabled = !state.busy,
        )
        Text(
            state.updateText.ifBlank {
                "Скачает APK с GitHub и поставит поверх. Это может любой участник, owner для этого не нужен."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.pendingApkPath != null) {
            Text(
                "APK уже скачан. Разрешите установку из этого приложения и подтвердите системное окно.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (RoleRules.canUpgradeCore(role)) {
            Text("Обновить ядро на VPS", style = MaterialTheme.typography.titleMedium)
            Text(
                if (host.isNotBlank()) {
                    "Сервер $host. Бинарник заменится на месте, чаты и owner останутся. Не открывает экран «Создать сервер»."
                } else {
                    "Нужен SSH к тому же VPS, где уже стоит Rope."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                sshPassword,
                { sshPassword = it },
                label = { Text("SSH-пароль") },
                singleLine = true,
                shape = RoundedCornerShape(RopeShapes.field),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { showKey = !showKey }) {
                Text(if (showKey) "Скрыть SSH-ключ" else "Вставить SSH-ключ вместо пароля")
            }
            if (showKey) {
                OutlinedTextField(
                    sshKey,
                    { sshKey = it },
                    label = { Text("SSH ключ") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
            }
            GlowButton(
                "Обновить ядро",
                { onUpgradeCore(sshPassword, sshKey) },
                Modifier.fillMaxWidth(),
                enabled = !state.busy && (sshPassword.isNotBlank() || sshKey.isNotBlank()),
            )
        }
    }
}

fun adminCardCount(snapshot: AdminSnapshot?): Int = snapshot?.cards?.size ?: 0
