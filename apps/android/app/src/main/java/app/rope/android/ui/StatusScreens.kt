package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.rope.android.UiState
import app.rope.android.data.AdminSnapshot

@Composable
fun StatusPane(
    state: UiState,
    onUpdateApp: () -> Unit,
    onUpgrade: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Сервер и здоровье", style = MaterialTheme.typography.titleLarge)
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
        Button(onClick = onUpdateApp, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Обновить приложение")
        }
        Text(
            state.updateText.ifBlank { "Скачает новый APK и предложит установить поверх. Удалять Rope не нужно." },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Обновить ядро на VPS")
        }
        Text(
            "Скачает новый rope-server. Чаты и owner останутся. Если зайти как owner больше нельзя — Создать сервер → Дополнительно → Стереть старое.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun adminCardCount(snapshot: AdminSnapshot?): Int = snapshot?.cards?.size ?: 0
