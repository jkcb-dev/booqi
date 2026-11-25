package corp.khin.solutions.booqi.feature.booking.map

import android.location.Geocoder
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSizing
import corp.khin.solutions.booqi.domain.model.Address
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun AddressMapPicker(
    cameraAddress: Address?,
    enabled: Boolean,
    onAddressPicked: (Address) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isResolving by remember { mutableStateOf(false) }
    var resolveError by remember { mutableStateOf<String?>(null) }
    val start = cameraAddress?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(DefaultMapLocation.LATITUDE, DefaultMapLocation.LONGITUDE)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, DefaultMapLocation.ZOOM)
    }

    LaunchedEffect(cameraAddress?.latitude, cameraAddress?.longitude) {
        val target = cameraAddress ?: return@LaunchedEffect
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(target.latitude, target.longitude),
                DefaultMapLocation.ZOOM,
            ),
        )
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(BooqiSizing.mapPickerHeight)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false,
                ),
            )
            MapCenterPin(modifier = Modifier.align(Alignment.Center))
        }
        MapPinConfirmSection(
            enabled = enabled,
            isResolving = isResolving,
            resolveError = resolveError,
            onConfirm = {
                if (!enabled || isResolving) return@MapPinConfirmSection
                val target = cameraPositionState.position.target
                isResolving = true
                resolveError = null
                scope.launch {
                    val address = reverseGeocode(context, target.latitude, target.longitude)
                    isResolving = false
                    if (address == null) resolveError = MapPinFallback.GEOCODE_FAILED
                    else onAddressPicked(address)
                }
            },
        )
    }
}

private suspend fun reverseGeocode(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
): Address? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) {
        return@withContext MapPinFallback.addressOrFallback(null, latitude, longitude)
    }
    runCatching {
        @Suppress("DEPRECATION")
        val results = Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)
        MapPinFallback.addressOrFallback(results?.firstOrNull()?.getAddressLine(0), latitude, longitude)
    }.getOrElse {
        MapPinFallback.addressOrFallback(null, latitude, longitude)
    }
}
