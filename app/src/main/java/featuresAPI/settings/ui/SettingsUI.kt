package featuresAPI.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import featuresAPI.settings.viewModel.SettingsStatus
import featuresAPI.settings.viewModel.SettingsViewModel

@Composable
fun SettingsUI(
    modifier: Modifier = Modifier,
    authenticationViewModel: AuthenticationViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
    onLoggedOut: () -> Unit = {}
) {
    val profile by settingsViewModel.profile.collectAsState()
    val status by settingsViewModel.status.collectAsState()
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
        onUsernameChange = { username = it },
        onDisplayNameChange = { displayName = it },
        onSave = { settingsViewModel.saveProfile(username, displayName) },
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
    onUsernameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSave: () -> Unit,
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

        TextButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log out")
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
        onUsernameChange = {},
        onDisplayNameChange = {},
        onSave = {},
        onLogout = {}
    )
}
