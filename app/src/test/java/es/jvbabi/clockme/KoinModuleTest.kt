package es.jvbabi.clockme

import android.content.Context
import es.jvbabi.clockme.di.appModule
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.KoinTest
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
class KoinModuleTest : KoinTest {
    @Test
    fun verifyKoinModules() {
        appModule.verify(extraTypes = listOf(Context::class))
    }
}
