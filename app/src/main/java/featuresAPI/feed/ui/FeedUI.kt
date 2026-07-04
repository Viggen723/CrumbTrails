package featuresAPI.feed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routetracker.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
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

            FeedRoutePreview(
                routeString = post.routeString,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            )

            // TODO: Show uploaded photos from photoUrls.
        }
    }
}

@Composable
private fun FeedRoutePreview(
    routeString: String,
    modifier: Modifier = Modifier
) {
    val routePoints = remember(routeString) {
        // decode the saved route so Feed can draw it
        runCatching {
            if (routeString.isBlank()) emptyList() else PolyUtil.decode(routeString)
        }.getOrDefault(emptyList())
    }

    if (routePoints.isEmpty()) return

    val cameraPositionState = remember(routeString) {
        val centerPoint = routePoints[routePoints.size / 2]
        CameraPositionState(
            position = CameraPosition.fromLatLngZoom(centerPoint, 14f)
        )
    }
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            tiltGesturesEnabled = false,
            rotationGesturesEnabled = false
        )
    }

    // tiny map preview for the shared route
    GoogleMap(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp)),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings
    ) {
        if (routePoints.size > 1) {
            Polyline(
                points = routePoints,
                color = colorResource(id = R.color.route_path),
                width = 10f
            )
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
