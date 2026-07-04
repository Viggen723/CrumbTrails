package featuresAPI.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import kotlin.math.roundToInt

data class MapPhotoPin(
    val imageSource: Any,
    val position: LatLng
)

fun fallbackRoutePosition(
    routePoints: List<LatLng>,
    index: Int,
    totalCount: Int
): LatLng? {
    if (routePoints.isEmpty() || totalCount <= 0) return null
    if (routePoints.size == 1) return routePoints.first()

    // no GPS? place the photo along the route so it still shows on the map
    val fraction = (index + 1).toDouble() / (totalCount + 1).toDouble()
    val routeIndex = (fraction * routePoints.lastIndex)
        .roundToInt()
        .coerceIn(0, routePoints.lastIndex)
    return routePoints[routeIndex]
}

@Composable
@GoogleMapComposable
fun PhotoThumbnailMarkers(
    pins: List<MapPhotoPin>,
    modifier: Modifier = Modifier
) {
    pins.forEachIndexed { index, pin ->
        PhotoThumbnailMarker(
            pin = pin,
            key = "photo-pin-$index-${pin.position.latitude}-${pin.position.longitude}",
            modifier = modifier
        )
    }
}

@Composable
@GoogleMapComposable
private fun PhotoThumbnailMarker(
    pin: MapPhotoPin,
    key: String,
    modifier: Modifier = Modifier
) {
    val painter = rememberAsyncImagePainter(model = pin.imageSource)
    val state = painter.state

    if (state !is AsyncImagePainter.State.Success) {
        Marker(state = MarkerState(position = pin.position))
        return
    }

    MarkerComposable(
        key,
        state.result.dataSource,
        state = MarkerState(position = pin.position),
        anchor = Offset(0.5f, 1.0f),
        zIndex = 2f
    ) {
        Box(
            modifier = modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color.White, CircleShape)
        ) {
            Image(
                painter = painter,
                contentDescription = "Route photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )
        }
    }
}
