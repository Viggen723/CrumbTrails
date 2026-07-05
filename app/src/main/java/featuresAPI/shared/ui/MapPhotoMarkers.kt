package featuresAPI.shared.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
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
            modifier = modifier
        )
    }
}

@Composable
@GoogleMapComposable
private fun PhotoThumbnailMarker(
    pin: MapPhotoPin,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thumbnailIcon by remember(pin.imageSource) { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(pin.imageSource) {
        thumbnailIcon = null
        thumbnailIcon = runCatching {
            val sizePx = (48 * context.resources.displayMetrics.density).roundToInt()
            val request = ImageRequest.Builder(context)
                .data(pin.imageSource)
                // Coil was staying stuck in Loading inside MarkerComposable, so preload the bitmap first.
                .allowHardware(false)
                .size(Size(sizePx, sizePx))
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.drawable.toBitmap()
                BitmapDescriptorFactory.fromBitmap(bitmap.toCircularThumbnail(sizePx))
            } else {
                null
            }
        }.getOrNull()
    }

    // Keep the default pin until the thumbnail bitmap is ready.
    Marker(
        state = MarkerState(position = pin.position),
        anchor = Offset(0.5f, 1.0f),
        icon = thumbnailIcon,
        zIndex = if (thumbnailIcon == null) 0f else 2f
    )
}

private fun Bitmap.toCircularThumbnail(sizePx: Int): Bitmap {
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val borderPx = (sizePx * 0.08f).coerceAtLeast(2f)
    val center = sizePx / 2f
    val imageRadius = center - borderPx

    val shader = BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val scale = maxOf(
        (imageRadius * 2f) / width.toFloat(),
        (imageRadius * 2f) / height.toFloat()
    )
    val matrix = Matrix().apply {
        setScale(scale, scale)
        postTranslate(
            center - width * scale / 2f,
            center - height * scale / 2f
        )
    }
    shader.setLocalMatrix(matrix)

    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }

    // Google Maps wants a BitmapDescriptor, so draw a tiny circular preview ourselves.
    canvas.drawCircle(center, center, imageRadius, imagePaint)
    canvas.drawCircle(center, center, imageRadius + borderPx / 2f, borderPaint)
    return output
}
