package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rope.android.BrandLinks
import app.rope.android.BuildConfig
import app.rope.android.NavRules
import app.rope.android.Screen
import app.rope.android.UiState
import app.rope.android.data.RoleRules

@Composable
fun HomePane(
    state: UiState,
    onGo: (Screen) -> Unit,
    onStatus: () -> Unit,
    onInvite: () -> Unit,
) {
    val uri = LocalUriHandler.current
    val me = state.profile?.displayName.orEmpty()
    val people = NavRules.peopleOf(state.devices, state.profile?.deviceId)
    val groups = NavRules.groupsOf(state.conversations)
    val owner = RoleRules.isOwner(state.profile?.role)
    Box(Modifier.fillMaxSize()) {
        BrandBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FadeIn(40) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RopeKnot(size = 148.dp, animate = true)
                    Spacer(Modifier.height(12.dp))
                    Text("self-hosted · E2EE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Rope", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Свой мессенджер на своём сервере",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        if (me.isBlank()) {
                            "Приватный self-hosted E2EE: ключи на телефоне, релей видит только шифротекст."
                        } else {
                            "Привет, $me. Ключи на этом телефоне. Сервер не читает переписку."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            FadeIn(160) {
                GlowButton("К чатам", { onGo(Screen.Chats) }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            FadeIn(220) {
                QuietButton("К статусу сервера", onStatus, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            FadeIn(280) {
                QuietButton(
                    "Открыть веб-лендинг",
                    { runCatching { uri.openUri(BrandLinks.GITHUB) } },
                    Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            FadeIn(340) {
                QuietButton(
                    "Скачать / о приложении",
                    { runCatching { uri.openUri(BrandLinks.RELEASES) } },
                    Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(22.dp))
            FadeIn(400) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("Чаты", state.conversations.size.toString(), Modifier.weight(1f)) { onGo(Screen.Chats) }
                    MiniStat("Группы", groups.size.toString(), Modifier.weight(1f)) { onGo(Screen.Groups) }
                    MiniStat("Люди", people.size.toString(), Modifier.weight(1f)) { onGo(Screen.People) }
                }
            }
            Spacer(Modifier.height(12.dp))
            FadeIn(460) {
                SectionCard(onClick = { onGo(Screen.Calls) }) {
                    Text("Звонки", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Входящие и исходящие — из личного чата. История на этой вкладке.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FadeIn(520) {
                SectionCard(onClick = onInvite) {
                    Text("Пригласить по QR", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Одноразовая ссылка. Гость входит без прав owner.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FadeIn(580) {
                SectionCard(onClick = { onGo(Screen.People) }) {
                    Text("Люди на сервере", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (people.isEmpty()) {
                            "Пока никого. Покажите QR — человек появится здесь."
                        } else {
                            people.take(4).joinToString(" · ") { it.displayName.ifBlank { it.deviceId.take(6) } }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Приложение ${BuildConfig.VERSION_NAME}" + if (owner) " · owner" else " · гость",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SectionCard(modifier = modifier, onClick = onClick) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
