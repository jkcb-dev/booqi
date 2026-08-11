package corp.khin.solutions.booqi.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

/**
 * Android's one-line Koin bootstrap. Keeps Koin itself an implementation detail of `shared` —
 * `androidApp` (Platform Integration's module) calls this and never needs to know Koin exists.
 */
fun initKoinAndroid(context: Context) {
    if (GlobalContext.getOrNull() == null) {
        initKoin(appDeclaration = { androidContext(context) })
    }
}
