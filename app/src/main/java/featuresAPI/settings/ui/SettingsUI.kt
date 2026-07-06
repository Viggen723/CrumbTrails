package featuresAPI.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import featuresAPI.authentication.viewModel.AuthenticationViewModel
import featuresAPI.settings.data.TrackingPreferences
import featuresAPI.settings.viewModel.SettingsStatus
import featuresAPI.settings.viewModel.SettingsViewModel

private val TRACKING_INTERVAL_OPTIONS = listOf(
    3_000L to "3 seconds",
    5_000L to "5 seconds",
    10_000L to "10 seconds",
    30_000L to "30 seconds"
)

private val TRACKING_DISTANCE_OPTIONS = listOf(
    1f to "1 meter",
    5f to "5 meters",
    10f to "10 meters",
    25f to "25 meters"
)

@Composable
fun SettingsUI(
    modifier: Modifier = Modifier,
    authenticationViewModel: AuthenticationViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
    onLoggedOut: () -> Unit = {}
) {
    val profile by settingsViewModel.profile.collectAsState()
    val status by settingsViewModel.status.collectAsState()
    val trackingPreferences by settingsViewModel.trackingPreferences.collectAsState()
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        username = profile.username
        displayName = profile.displayName
    }

    SettingsContent(
        modifier = modifier,
        email = settingsViewModel.userEmail,
        firebaseDisplayName = settingsViewModel.firebaseDisplayName,
        status = status,
        username = username,
        displayName = displayName,
        trackingPreferences = trackingPreferences,
        onUsernameChange = { username = it },
        onDisplayNameChange = { displayName = it },
        onSave = { settingsViewModel.saveProfile(username, displayName) },
        onUpdateIntervalSelected = settingsViewModel::saveTrackingUpdateInterval,
        onMinDistanceSelected = settingsViewModel::saveTrackingMinDistance,
        onLogout = {
            authenticationViewModel.logOut()
            onLoggedOut()
        }
    )
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    email: String,
    firebaseDisplayName: String,
    status: SettingsStatus,
    username: String,
    displayName: String,
    trackingPreferences: TrackingPreferences,
    onUsernameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onUpdateIntervalSelected: (Long) -> Unit,
    onMinDistanceSelected: (Float) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = email.ifBlank { "Signed in" },
            style = MaterialTheme.typography.bodyMedium
        )

        if (firebaseDisplayName.isNotBlank()) {
            Text(
                text = firebaseDisplayName,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            singleLine = true
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Display name") },
            singleLine = true
        )

        Button(
            onClick = onSave,
            enabled = status !is SettingsStatus.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (status is SettingsStatus.Loading) "Saving..." else "Save profile")
        }

        val statusText = when (status) {
            SettingsStatus.Idle -> null
            SettingsStatus.Loading -> null
            SettingsStatus.Saved -> "Profile saved."
            is SettingsStatus.Error -> status.message
        }
        statusText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (status is SettingsStatus.Error) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Text(
            text = "Tracking Settings",
            style = MaterialTheme.typography.titleMedium
        )

        TrackingSettingsCard(
            trackingPreferences = trackingPreferences,
            onUpdateIntervalSelected = onUpdateIntervalSelected,
            onMinDistanceSelected = onMinDistanceSelected
        )

        TextButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Log out")
        }
    }
}

@Composable
private fun TrackingSettingsCard(
    trackingPreferences: TrackingPreferences,
    onUpdateIntervalSelected: (Long) -> Unit,
    onMinDistanceSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // These dropdown rows save immediately and apply next session.
            TrackingPreferenceDropdown(
                label = "Tracking update interval",
                selectedLabel = TRACKING_INTERVAL_OPTIONS
                    .firstOrNull { it.first == trackingPreferences.updateIntervalMillis }
                    ?.second
                    ?: "${trackingPreferences.updateIntervalMillis / 1000} seconds",
                options = TRACKING_INTERVAL_OPTIONS,
                onOptionSelected = onUpdateIntervalSelected
            )

            HorizontalDivider()

            TrackingPreferenceDropdown(
                label = "Minimum distance",
                selectedLabel = TRACKING_DISTANCE_OPTIONS
                    .firstOrNull { it.first == trackingPreferences.minDistanceMeters }
                    ?.second
                    ?: "${trackingPreferences.minDistanceMeters.toInt()} meters",
                options = TRACKING_DISTANCE_OPTIONS,
                onOptionSelected = onMinDistanceSelected
            )

            Text(
                text = "Changes apply the next time tracking starts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun <T> TrackingPreferenceDropdown(
    label: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Box {
            Row(
                modifier = Modifier.clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            expanded = false
                            onOptionSelected(option.first)
                        }
                    )
                }
            }
        }
    }
}
@Preview
@Composable
fun SettingsUIPreview() {
    SettingsContent(
        email = "ayra@example.com",
        firebaseDisplayName = "",
        status = SettingsStatus.Idle,
        username = "ayra",
        displayName = "Ayra",
        trackingPreferences = TrackingPreferences(),
        onUsernameChange = {},
        onDisplayNameChange = {},
        onSave = {},
        onUpdateIntervalSelected = {},
        onMinDistanceSelected = {},
        onLogout = {}
    )
}
