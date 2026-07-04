package featuresAPI.feed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routetracker.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import featuresAPI.feed.data.SharedRoutePhoto
import featuresAPI.feed.data.SharedRoutePost
import featuresAPI.feed.viewModel.FeedViewModel
import featuresAPI.shared.ui.MapPhotoPin
import featuresAPI.shared.ui.PhotoThumbnailMarkers
import featuresAPI.shared.ui.fallbackRoutePosition
import java.text.DateFormat
import java.util.Date

@Composable
fun FeedUI(
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    var selectedPost by remember { mutableStateOf<SharedRoutePost?>(null) }

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
                FeedCards(
                    post = post,
                    onRoutePreviewClick = { selectedPost = post }
                )
            }
        }
    }

    selectedPost?.let { post ->
        val routePoints = remember(post.routeString) { decodeRoutePoints(post.routeString) }
        if (routePoints.isNotEmpty()) {
            FeedRouteMapDialog(
                postId = post.postId,
                routePoints = routePoints,
                photoPins = post.toMapPhotoPins(routePoints),
                onDismiss = { selectedPost = null }
            )
        } else {
            selectedPost = null
        }
    }
}

@Composable
fun FeedCards(
    post: SharedRoutePost,
    onRoutePreviewClick: () -> Unit = {},
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
                photos = post.photos,
                photoUrls = post.photoUrls,
                onClick = onRoutePreviewClick,
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
    photos: List<SharedRoutePhoto>,
    photoUrls: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routePoints = remember(routeString) { decodeRoutePoints(routeString) }

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
    val photoPins = remember(routePoints, photos, photoUrls) {
        buildFeedPhotoPins(
            routePoints = routePoints,
            photos = photos,
            photoUrls = photoUrls
        )
    }

    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // tiny map preview for the shared route
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings
        ) {
            if (routePoints.size > 1) {
                Polyline(
                    points = routePoints,
                    color = colorResource(id = R.color.route_path),
                    width = 10f
                )
            } else {
                Marker(state = MarkerState(position = routePoints.first()))
            }

            PhotoThumbnailMarkers(pins = photoPins)
        }

        // GoogleMap eats clicks, so this overlay catches the tap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun FeedRouteMapDialog(
    postId: String,
    routePoints: List<LatLng>,
    photoPins: List<MapPhotoPin>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val cameraPositionState = remember(postId, routePoints) {
                val centerPoint = routePoints[routePoints.size / 2]
                CameraPositionState(
                    position = CameraPosition.fromLatLngZoom(centerPoint, 15f)
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                if (routePoints.size > 1) {
                    Polyline(
                        points = routePoints,
                        color = colorResource(id = R.color.route_path),
                        width = 15f
                    )
                } else {
                    Marker(state = MarkerState(position = routePoints.first()))
                }

                // full map uses simple pins for now because async thumbnail markers can crash in Dialog
                photoPins.forEach { pin ->
                    Marker(state = MarkerState(position = pin.position))
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }
    }
}

private fun decodeRoutePoints(routeString: String): List<LatLng> {
    // decode the saved route so Feed can draw it
    return runCatching {
        if (routeString.isBlank()) emptyList() else PolyUtil.decode(routeString)
    }.getOrDefault(emptyList())
}

private fun SharedRoutePost.toMapPhotoPins(routePoints: List<LatLng>): List<MapPhotoPin> {
    return buildFeedPhotoPins(
        routePoints = routePoints,
        photos = photos,
        photoUrls = photoUrls
    )
}

private fun buildFeedPhotoPins(
    routePoints: List<LatLng>,
    photos: List<SharedRoutePhoto>,
    photoUrls: List<String>
): List<MapPhotoPin> {
    val photoSources = photos.ifEmpty {
        photoUrls.map { url -> SharedRoutePhoto(url = url) }
    }.filter { it.url.isNotBlank() }

    return photoSources.mapIndexedNotNull { index, photo ->
        val latitude = photo.latitude
        val longitude = photo.longitude
        val position = if (latitude != null && longitude != null) {
            LatLng(latitude, longitude)
        } else {
            fallbackRoutePosition(routePoints, index, photoSources.size)
        }
        position?.let {
            MapPhotoPin(
                imageSource = photo.url,
                position = it
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
