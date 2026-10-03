package corp.khin.solutions.booqi.feature.provider

/**
 * TEMPORARY: there is no auth/session concept built yet (Identity bounded context is "Not built
 * yet" per docs/DOMAIN.md), so every `feature:provider` ViewModel that needs "the current
 * Provider" uses this one constant — [ProviderProfileViewModel] as the user id, the service
 * ViewModels as `Service.providerId` — so what one screen creates is what the next screen lists.
 * Replace with the signed-in Provider's real id once Identity exists (Architect's wiring can
 * already override it: the service ViewModels take `providerId` as a constructor parameter).
 */
internal const val TEMPORARY_PROVIDER_ID = "user-placeholder-temp"
