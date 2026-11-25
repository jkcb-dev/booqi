package corp.khin.solutions.booqi.feature.booking.map

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
import androidx.compose.ui.viewinterop.UIKitView
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSizing
import corp.khin.solutions.booqi.domain.model.Address
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.launch
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLPlacemark
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AddressMapPicker(
    cameraAddress: Address?,
    enabled: Boolean,
    onAddressPicked: (Address) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var isResolving by remember { mutableStateOf(false) }
    var resolveError by remember { mutableStateOf<String?>(null) }
    val mapView = remember {
        MKMapView().apply {
            zoomEnabled = true
            scrollEnabled = true
            pitchEnabled = false
            rotateEnabled = false
        }
    }

    LaunchedEffect(cameraAddress?.latitude, cameraAddress?.longitude) {
        val lat = cameraAddress?.latitude ?: DefaultMapLocation.LATITUDE
        val lng = cameraAddress?.longitude ?: DefaultMapLocation.LONGITUDE
        val region = MKCoordinateRegionMakeWithDistance(
            CLLocationCoordinate2DMake(lat, lng),
            REGION_METERS,
            REGION_METERS,
        )
        mapView.setRegion(region, animated = true)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(BooqiSizing.mapPickerHeight)) {
            UIKitView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { /* camera via LaunchedEffect */ },
            )
            MapCenterPin(modifier = Modifier.align(Alignment.Center))
        }
        MapPinConfirmSection(
            enabled = enabled,
            isResolving = isResolving,
            resolveError = resolveError,
            onConfirm = {
                if (!enabled || isResolving) return@MapPinConfirmSection
                isResolving = true
                resolveError = null
                val center = mapView.centerCoordinate.useContents { latitude to longitude }
                scope.launch {
                    val address = reverseGeocode(center.first, center.second)
                    isResolving = false
                    if (address == null) resolveError = MapPinFallback.GEOCODE_FAILED
                    else onAddressPicked(address)
                }
            },
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun reverseGeocode(latitude: Double, longitude: Double): Address? =
    suspendCoroutine { continuation ->
        val location = CLLocation(latitude = latitude, longitude = longitude)
        CLGeocoder().reverseGeocodeLocation(location) { placemarks, error ->
            dispatch_async(dispatch_get_main_queue()) {
                val placemark = placemarks?.firstOrNull() as? CLPlacemark
                if (error != null || placemark == null) {
                    continuation.resume(
                        MapPinFallback.addressOrFallback(null, latitude, longitude),
                    )
                    return@dispatch_async
                }
                val line = listOfNotNull(
                    placemark.name,
                    placemark.locality,
                    placemark.administrativeArea,
                    placemark.country,
                ).distinct().joinToString(", ").ifEmpty { null }
                continuation.resume(
                    MapPinFallback.addressOrFallback(line, latitude, longitude),
                )
            }
        }
    }

private const val REGION_METERS = 1_200.0
