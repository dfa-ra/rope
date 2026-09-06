package app.rope.android

import android.app.Application

class RopeApp : Application() {
    lateinit var repo: RopeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = RopeRepository(this)
    }
}
