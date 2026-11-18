package es.jvbabi.clockme

import android.app.Application
import es.jvbabi.clockme.core.activity.CurrentActivityHolder
import es.jvbabi.clockme.di.appModule
import es.jvbabi.clockme.sync.CalendarEventSyncObserver
import es.jvbabi.clockme.sync.CalendarEventSyncScheduler
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class CirclesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@CirclesApp)
            workManagerFactory()
            modules(appModule)
        }
        registerActivityLifecycleCallbacks(get<CurrentActivityHolder>())
        get<CalendarEventSyncObserver>().start()
        get<CalendarEventSyncScheduler>().schedule()
    }
}
