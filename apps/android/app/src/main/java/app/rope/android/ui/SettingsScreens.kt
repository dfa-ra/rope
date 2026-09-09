package app.rope.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.rope.android.BuildConfig
import app.rope.android.UiState
import app.rope.android.data.FontScaleRules
import app.rope.android.data.RevokeRules
import app.rope.android.data.SettingsRules
import app.rope.android.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsPane(
    state: UiState,
    onSetTheme: (ThemeMode) -> Unit,
    onToggleNotifications: () -> Unit,
    onCopy: (String) -> Unit,
    onToggleLinkPreviews: () -> Unit = {},
    onSetFontScale: (String) -> Unit = {},
) {
    val me = state.profile?.displayName.orEmpty()
    val profile = state.profile
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FadeIn(80) {
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InitialsAvatar(
                        title = me.ifBlank { "?" },
                        group = false,
                        online = !state.offline,
                        size = 52.dp,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            me.ifBlank { "Rope" },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (profile != null) {
                                "${SettingsRules.roleLabel(profile.role)} · ${BuildConfig.VERSION_NAME}"
                            } else {
                                "Rope ${BuildConfig.VERSION_NAME}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        FadeIn(120) {
            SectionCard {
                Text("Уведомления", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Без звука", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            SettingsRules.notificationsHint(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.notificationsMuted,
                        onCheckedChange = { checked ->
                            if (checked != state.notificationsMuted) onToggleNotifications()
                        },
                        modifier = Modifier.semantics { contentDescription = "Без звука" },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Предпросмотр ссылок", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            SettingsRules.linkPreviewsHint(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.linkPreviewsEnabled,
                        onCheckedChange = { checked ->
                            if (checked != state.linkPreviewsEnabled) onToggleLinkPreviews()
                        },
                        modifier = Modifier.semantics { contentDescription = "Предпросмотр ссылок" },
                    )
                }
            }
        }
        FadeIn(160) {
            SectionCard {
                Text("Оформление", style = MaterialTheme.typography.titleMedium)
                Text(
                    SettingsRules.appearanceHint(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.theme == ThemeMode.DARK,
                        onClick = { onSetTheme(ThemeMode.DARK) },
                        label = { Text("Тёмная") },
                    )
                    FilterChip(
                        selected = state.theme == ThemeMode.LIGHT,
                        onClick = { onSetTheme(ThemeMode.LIGHT) },
                        label = { Text("Светлая") },
                    )
                }
            }
        }
        FadeIn(180) {
            SectionCard {
                Text("Размер текста", style = MaterialTheme.typography.titleMedium)
                Text(
                    FontScaleRules.hint(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    FontScaleRules.sample(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { contentDescription = "Пример размера текста" },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val current = FontScaleRules.parse(state.fontScale)
                    FontScaleRules.PRESETS.forEach { preset ->
                        FilterChip(
                            selected = current == preset.id,
                            onClick = { onSetFontScale(preset.id) },
                            label = { Text(preset.label) },
                        )
                    }
                }
            }
        }
        if (profile != null) {
            FadeIn(200) {
                val endpoint = SettingsRules.formatEndpoint(profile.host, profile.port, profile.useTls)
                val fp = SettingsRules.formatHexGroups(profile.fingerprint)
                val sid = SettingsRules.formatHexGroups(profile.serverId)
                SectionCard {
                    Text("Сервер", style = MaterialTheme.typography.titleMedium)
                    Text(
                        endpoint,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopy(endpoint) }
                            .semantics { contentDescription = "Адрес сервера" },
                    )
                    if (fp.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("TLS fingerprint", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            fp,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCopy(SettingsRules.copyFingerprintValue(profile.fingerprint))
                                }
                                .semantics { contentDescription = "TLS fingerprint" },
                        )
                    }
                    if (sid.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("server id", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sid, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        SettingsRules.serverPinHint(profile.useTls),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (RevokeRules.canRevoke(profile.role)) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            RevokeRules.settingsHint(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        FadeIn(240) {
            SectionCard {
                Text("О приложении", style = MaterialTheme.typography.titleMedium)
                Text("Rope ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    SettingsRules.aboutBody(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
