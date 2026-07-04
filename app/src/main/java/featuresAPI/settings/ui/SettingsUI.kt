package featuresAPI.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import featuresAPI.authentication.viewModel.AuthenticationViewModel

@Composable
fun SettingsUI(
    modifier: Modifier = Modifier,
    authenticationViewModel: AuthenticationViewModel)
{
    Column(modifier = Modifier.fillMaxSize())
    {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = { authenticationViewModel.logOut() }))// Not forcing the user back to the login screen yet
        {
            Text(
                text = "Log out",
                style = MaterialTheme.typography.bodyMedium
            )

            Icon(modifier = Modifier.align(Alignment.CenterVertically),
                imageVector = Icons.Filled.ArrowBackIosNew,
                contentDescription = null,
                )
        }
    }
}

@Preview
@Composable
fun SettingsUI()
{

}