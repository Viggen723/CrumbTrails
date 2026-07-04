package featuresAPI.feed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import featuresAPI.feed.data.SharedRoutePost
import featuresAPI.feed.viewModel.FeedViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun FeedUI(
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()

    if (posts.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No shared routes yet.",
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(posts, key = { it.postId }) { post ->
                FeedCards(post = post)
            }
        }
    }
}

@Composable
fun FeedCards(
    post: SharedRoutePost,
    modifier: Modifier = Modifier,
) {

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.tripName,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(post.createdAt)),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Text(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp),
                text = post.caption.ifBlank { "No caption" },
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                text = "${post.pointCount} points recorded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // later we can use routeString to draw a mini map preview
            // TODO: Show route preview map from routeString.
            // TODO: Show uploaded photos from photoUrls.
        }
    }
}

@Preview
@Composable
fun FeedCardsPreview()
{
    FeedCards(
        post = SharedRoutePost(
            postId = "preview",
            tripName = "Morning Route",
            caption = "Easy loop before work.",
            createdAt = System.currentTimeMillis(),
            pointCount = 128
        )
    )
}
