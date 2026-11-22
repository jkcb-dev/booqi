package corp.khin.solutions.booqi.domain.model

/**
 * A Provider's location as submitted when completing/editing the profile: the free-text [text]
 * line plus the optional map position [coordinates] of that text. Value object — it is also the
 * parameter object that keeps `CompletarPerfilDeProveedor` under the LongParameterList limit.
 *
 * The coordinates passed are the coordinates stored: `null` clears a previous pin (a changed
 * [text] makes an old pin stale). See [ProviderProfile.coordinates].
 */
data class ProviderLocation(
    val text: String,
    val coordinates: GeoPoint? = null,
)
