package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.rope.android.BuildConfig
import app.rope.android.UiState
import app.rope.android.data.AdminSnapshot

@Composable
fun StatusPane(
    state: UiState,
    onUpdateApp: () -> Unit,
    onUpgradeCore: (String, String) -> Unit,
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
        Text(
            "Приложение ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
        )
        val cards = state.admin?.cards.orEmpty()
        if (cards.isEmpty()) {
            Text(
                state.statusText.ifBlank { "Статус сервера ещё не загружен." },
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            cards.forEach { card ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(card.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(card.value, style = MaterialTheme.typography.titleLarge)
                        if (card.hint.isNotBlank()) {
                            Text(card.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Button(
            onClick = onUpdateApp,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (state.pendingApkPath != null) "Повторить установку APK" else "Обновить приложение")
        }
        Text(
            state.updateText.ifBlank {
                "Скачает APK и поставит поверх, без удаления. Если система откажет из‑за старой подписи — ключ уже лежит в Загрузках как rope-device.backup."
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
        Button(
            onClick = { onUpgradeCore(sshPassword, sshKey) },
            enabled = !state.busy && (sshPassword.isNotBlank() || sshKey.isNotBlank()),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Обновить ядро")
        }
    }
}

fun adminCardCount(snapshot: AdminSnapshot?): Int = snapshot?.cards?.size ?: 0
