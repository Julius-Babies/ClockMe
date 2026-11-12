package es.jvbabi.clockme

import es.jvbabi.clockme.di.appModule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class KoinModuleTest : KoinTest {
    @Test
    fun verifyKoinModules() {
        appModule.verify()
    }
}
