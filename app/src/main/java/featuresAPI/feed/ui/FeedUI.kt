package featuresAPI.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routetracker.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import featuresAPI.feed.data.SharedRoutePhoto
import featuresAPI.feed.data.SharedRoutePost
import featuresAPI.feed.viewModel.FeedActionStatus
import featuresAPI.feed.viewModel.FeedViewModel
import featuresAPI.shared.ui.MapPhotoPin
import featuresAPI.shared.ui.PhotoThumbnailMarkers
import featuresAPI.shared.ui.fallbackRoutePosition
import java.text.DateFormat
import java.util.Date

private enum class FeedFilter(val label: String) {
    AllPosts("All posts"),
    MyPosts("My posts"),
    OtherUsers("Other users")
}

@Composable
fun FeedUI(
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val actionStatus by viewModel.actionStatus.collectAsState()
    val currentUserId = viewModel.currentUserId
    var selectedPost by remember { mutableStateOf<SharedRoutePost?>(null) }
    var postPendingCaptionEdit by remember { mutableStateOf<SharedRoutePost?>(null) }
    var editedCaption by remember { mutableStateOf("") }
    var postPendingDelete by remember { mutableStateOf<SharedRoutePost?>(null) }
    var selectedFilter by remember { mutableStateOf(FeedFilter.AllPosts) }
    val filteredPosts = remember(posts, selectedFilter, currentUserId) {
        posts.filter { post ->
            when (selectedFilter) {
                FeedFilter.AllPosts -> true
                FeedFilter.MyPosts -> post.userId == currentUserId
                FeedFilter.OtherUsers -> post.userId != currentUserId
            }
        }
    }

    postPendingCaptionEdit?.let { post ->
        AlertDialog(
            onDismissRequest = { postPendingCaptionEdit = null },
            title = { Text("Edit caption") },
            text = {
                OutlinedTextField(
                    value = editedCaption,
                    onValueChange = { editedCaption = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Caption") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.editCaption(post, editedCaption)
                        postPendingCaptionEdit = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { postPendingCaptionEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    postPendingDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { postPendingDelete = null },
            title = { Text("Delete this post?") },
            text = { Text("This removes the shared route from your Feed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePost(post)
                        postPendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { postPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (posts.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No shared routes yet.",
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeedFilterDropdown(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No posts found for this filter.",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPosts, key = { it.postId }) { post ->
                        FeedCards(
                            post = post,
                            showPostOptions = post.userId == currentUserId,
                            onRoutePreviewClick = { selectedPost = post },
                            onEditCaption = {
                                postPendingCaptionEdit = post
                                editedCaption = post.caption
                            },
                            onDeletePost = { postPendingDelete = post }
                        )
                    }
                }
            }
        }
    }

    if (actionStatus is FeedActionStatus.Error) {
        Text(
            text = (actionStatus as FeedActionStatus.Error).message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
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
private fun FeedFilterDropdown(
    selectedFilter: FeedFilter,
    onFilterSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text("Filter: ${selectedFilter.label}")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FeedFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        expanded = false
                        onFilterSelected(filter)
                    }
                )
            }
        }
    }
}

@Composable
fun FeedCards(
    post: SharedRoutePost,
    showPostOptions: Boolean = true,
    onRoutePreviewClick: () -> Unit = {},
    onEditCaption: () -> Unit = {},
    onDeletePost: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorLabel(),
                        style = MaterialTheme.typography.titleSmall
                    )
                    post.authorHandleLabel()?.let { handle ->
                        Text(
                            text = handle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(post.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showPostOptions) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Post options"
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit caption") },
                                    onClick = {
                                        menuExpanded = false
                                        onEditCaption()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete post") },
                                    onClick = {
                                        menuExpanded = false
                                        onDeletePost()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Text(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp),
                text = post.caption.ifBlank { "No caption" },
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp),
                text = "Route: ${post.tripName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

    val singlePointRouteMarkerState = rememberMarkerState(position = routePoints.first())
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
    val mapPoints = remember(routePoints, photoPins) {
        routePoints + photoPins.map { it.position }
    }
    val cameraPositionState = remember(routeString) {
        val bounds = mapPoints.toLatLngBounds()
        CameraPositionState(
            position = CameraPosition.fromLatLngZoom(
                bounds?.center ?: routePoints[routePoints.size / 2],
                14f
            )
        )
    }
    LaunchedEffect(cameraPositionState, mapPoints) {
        val bounds = mapPoints.toLatLngBounds()
        if (bounds != null && mapPoints.size > 1) {
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 64))
        }
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
            } else if (photoPins.isEmpty()) {
                Marker(state = singlePointRouteMarkerState)
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
            var selectedPhotoPin by remember { mutableStateOf<MapPhotoPin?>(null) }
            val singlePointRouteMarkerState = rememberMarkerState(position = routePoints.first())
            val mapPoints = remember(routePoints, photoPins) {
                routePoints + photoPins.map { it.position }
            }
            val cameraPositionState = remember(postId, routePoints) {
                val bounds = mapPoints.toLatLngBounds()
                CameraPositionState(
                    position = CameraPosition.fromLatLngZoom(
                        bounds?.center ?: routePoints[routePoints.size / 2],
                        15f
                    )
                )
            }
            LaunchedEffect(cameraPositionState, mapPoints) {
                val bounds = mapPoints.toLatLngBounds()
                if (bounds != null && mapPoints.size > 1) {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 96))
                }
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
                } else if (photoPins.isEmpty()) {
                    Marker(state = singlePointRouteMarkerState)
                }

                PhotoThumbnailMarkers(
                    pins = photoPins,
                    onPhotoClick = { selectedPhotoPin = it }
                )
            }

            selectedPhotoPin?.let { pin ->
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = pin.imageSource,
                        contentDescription = "Route photo",
                        contentScale = ContentScale.Fit,
                        placeholder = rememberVectorPainter(Icons.Filled.Refresh),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )

                    IconButton(
                        onClick = { selectedPhotoPin = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close photo",
                            tint = Color.White
                        )
                    }
                }
            }

            if (selectedPhotoPin == null) {
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

private fun SharedRoutePost.authorLabel(): String {
    return when {
        authorName.isNotBlank() -> authorName
        authorUsername.isNotBlank() -> "@$authorUsername"
        else -> "Unknown user"
    }
}

private fun SharedRoutePost.authorHandleLabel(): String? {
    return if (authorName.isNotBlank() && authorUsername.isNotBlank()) {
        "@$authorUsername"
    } else {
        null
    }
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

private fun List<LatLng>.toLatLngBounds(): LatLngBounds? {
    if (isEmpty()) return null
    return LatLngBounds.builder().apply {
        forEach { include(it) }
    }.build()
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
