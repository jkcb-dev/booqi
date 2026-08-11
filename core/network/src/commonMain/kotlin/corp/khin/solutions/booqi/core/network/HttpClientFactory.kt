package corp.khin.solutions.booqi.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A single shared [HttpClient] configuration. Ktor resolves the platform engine automatically
 * (OkHttp on Android, Darwin on iOS) from whichever single engine artifact is on that target's
 * classpath — no expect/actual needed here.
 *
 * Real endpoints/DTOs are added by the Shared Domain & Data role once the backend API is defined;
 * this factory is the one place client-wide concerns (serialization, logging, timeouts) live.
 */
fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        )
    }
    install(Logging) {
        level = LogLevel.INFO
    }
}
