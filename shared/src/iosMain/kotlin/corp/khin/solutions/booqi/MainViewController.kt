package corp.khin.solutions.booqi

import androidx.compose.ui.window.ComposeUIViewController
import corp.khin.solutions.booqi.di.initKoin

// MainViewController() is only ever invoked once per process on iOS (no Activity-style
// recreation to guard against), so a plain flag is enough here — no need for Koin's own
// platform-specific "is a Koin instance already running" check.
private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    App()
}
