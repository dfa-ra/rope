package app.rope.android

import android.app.Application
import app.rope.android.provision.CryptoInit

class RopeApp : Application() {
    lateinit var repo: RopeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        CryptoInit.ensureModernBc()
        repo = RopeRepository(this)
    }
}
