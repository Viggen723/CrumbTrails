package featuresAPI.feed.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckboxDefaults.colors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import featuresAPI.feed.viewModel.FeedViewModel

@Composable
fun FeedUI(
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = FeedViewModel()) {

    Text("Test")

    LazyColumn(modifier = Modifier) {
        // The Feed cards will be loaded into here
    }

}

// The Onclick function is very similar to the existing Session card Dialog so let's use that!
@Composable
fun FeedCards(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,) {

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier =
                Modifier
                    .weight(1f)
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text(
                        text = "Profile name",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        modifier = Modifier,
                        text = "Date Route occurred",
                        style = MaterialTheme.typography.titleSmall
                    )
            }

            Text(modifier = Modifier.padding(16.dp),
                text =  "The route that the user took will be described here!!")
        }
    }
}

@Preview
@Composable
fun FeedCardsPreview()
{
    FeedCards(Modifier, { })
}