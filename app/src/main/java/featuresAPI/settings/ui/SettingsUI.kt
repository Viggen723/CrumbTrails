package featuresAPI.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import featuresAPI.authentication.viewModel.AuthenticationViewModel

@Composable
fun SettingsUI(
    modifier: Modifier = Modifier,
    authenticationViewModel: AuthenticationViewModel)
{
    Column(modifier = Modifier.fillMaxSize())
    {
        Row(modifier = Modifier.fillMaxWidth())
        {
            Text(
                // Not forcing the user back to the login screen yet
                modifier = Modifier.clickable(onClick = { authenticationViewModel.logOut() }),
                text = "Log out",
                style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview
@Composable
fun SettingsUI()
{

}