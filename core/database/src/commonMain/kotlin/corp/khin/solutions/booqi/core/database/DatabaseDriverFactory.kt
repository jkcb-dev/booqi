package corp.khin.solutions.booqi.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * Platform-agnostic capability to open a [SqlDriver] for a given SQLDelight-generated schema.
 *
 * Deliberately schema-agnostic: the concrete `.sq` files and generated `Database` class belong to
 * whichever `data`-layer datasource actually owns that table (a `core:database`-wide schema would
 * violate the module's job, which is only "how do we get a driver on this platform").
 *
 * Bound to its platform implementation via Koin (`androidModule` provides the `Context`-backed
 * impl, `iosModule` provides the plain one) rather than expect/actual, since the Android driver
 * needs a `Context` and the iOS one doesn't — DI already solves that asymmetry without forcing a
 * shared constructor shape.
 */
interface DatabaseDriverFactory {
    fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, databaseName: String): SqlDriver
}
