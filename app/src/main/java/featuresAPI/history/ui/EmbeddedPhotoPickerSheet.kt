package com.example.routetracker.featuresAPI.history.ui

import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.photopicker.compose.EmbeddedPhotoPicker
import androidx.photopicker.compose.ExperimentalPhotoPickerComposeApi
import androidx.photopicker.compose.rememberEmbeddedPhotoPickerState

/**
 * Validates if the device can use the modern embedded photo picker.
 */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun isEmbeddedPhotoPickerSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 15
}

/**
 * Elevated Sheet UI hosted at the Screen-root level to manage photo selections.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPhotoPickerComposeApi::class)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@Composable
fun EmbeddedPhotoPickerSheet(
    onUriGranted: (List<Uri>) -> Unit,
    onUriRevoked: (List<Uri>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pickerState = rememberEmbeddedPhotoPickerState(
        onUriPermissionGranted = onUriGranted,
        onUriPermissionRevoked = onUriRevoked,
        onSelectionComplete = onDismiss
    )

    val configuration = LocalConfiguration.current
    val sheetHeight = remember(configuration) { (configuration.screenHeightDp * 0.55).dp }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
        ) {
            EmbeddedPhotoPicker(state = pickerState)
        }
    }
}