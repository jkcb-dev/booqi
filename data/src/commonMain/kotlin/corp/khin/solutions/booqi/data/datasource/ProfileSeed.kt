package corp.khin.solutions.booqi.data.datasource

/** What [SampleData] needs to describe one sample ProviderProfile; [userId] is its owner. */
internal class ProfileSeed(
    val id: String,
    val name: String,
    val location: String,
    val coordinates: Pair<Double, Double>,
    val rating: Pair<Double, Int>,
    val userId: String = sampleOwnerOf(id),
)
