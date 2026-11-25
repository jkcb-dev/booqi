package corp.khin.solutions.booqi.feature.booking.map

import corp.khin.solutions.booqi.domain.model.Address

/** Shared helpers for the Android / iOS map actuals when reverse geocoding fails or is empty. */
internal object MapPinFallback {
    const val RESOLVING_LABEL = "Resolviendo dirección..."
    const val CONFIRM_LABEL = "Usar este punto del mapa"
    const val GEOCODE_FAILED = "No pudimos leer la dirección de este punto. Probá de nuevo o buscá por texto."

    fun addressOrFallback(line: String?, latitude: Double, longitude: Double): Address {
        val trimmed = line?.trim().orEmpty()
        return Address(
            line = trimmed.ifEmpty { "Ubicación $latitude, $longitude" },
            latitude = latitude,
            longitude = longitude,
        )
    }
}
