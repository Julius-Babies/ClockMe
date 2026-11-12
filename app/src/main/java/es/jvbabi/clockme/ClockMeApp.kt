package es.jvbabi.clockme

import android.app.Application
import es.jvbabi.clockme.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ClockMeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ClockMeApp)
            modules(appModule)
        }
    }
}
