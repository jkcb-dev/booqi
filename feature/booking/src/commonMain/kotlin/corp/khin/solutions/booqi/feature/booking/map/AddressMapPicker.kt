package corp.khin.solutions.booqi.feature.booking.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.domain.model.Address

/**
 * Platform map pin picker for C6 (docs/design/SCREENS.md AddressPicker). Shows a native map
 * (Google Maps on Android, MapKit on iOS) with a fixed center pin; the Customer pans the map and
 * confirms the point under the pin. Reverse geocoding to an [Address.line] happens inside each
 * `actual` — platform SDKs never leak into domain / data / ViewModels.
 *
 * [cameraAddress] is the pin the map should center on (selected search result, else saved
 * address, else the Buenos Aires default). Confirming the pin calls [onAddressPicked] with a
 * full [Address] ready for [corp.khin.solutions.booqi.feature.booking.AddressAction.ResultSelected].
 */
@Composable
expect fun AddressMapPicker(
    cameraAddress: Address?,
    enabled: Boolean,
    onAddressPicked: (Address) -> Unit,
    modifier: Modifier = Modifier,
)
