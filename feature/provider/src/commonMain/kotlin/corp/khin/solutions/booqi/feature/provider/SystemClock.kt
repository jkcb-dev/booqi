package corp.khin.solutions.booqi.feature.provider

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * The wall clock behind [DateBlockingViewModel]'s default `clock`.
 *
 * Why not `Clock.System`: this module's Compose Material3 (1.11.0-alpha07) drags kotlinx-datetime
 * 0.7.1 onto the iOS classpath while the rest of the repo is declared at 0.6.2. In 0.7.x
 * `kotlinx.datetime.Clock` is a typealias of `kotlin.time.Clock`, and a nested object
 * (`Clock.System`) cannot be reached through a typealias, so `Clock.System` does not compile for
 * `iosSimulatorArm64` here. `kotlin.time.Clock.System` exists on both versions' toolchain and its
 * epoch milliseconds convert to the `kotlinx.datetime.Instant` the 0.6.2 `Clock` contract wants.
 * Drop this once the datetime version is aligned repo-wide.
 */
internal object SystemClock : Clock {
    override fun now(): Instant =
        Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
}
