package github.wensimin.message

import android.app.Application

class Application : Application() {
    init {
        context = this
    }

    companion object {
        lateinit var context: Application
            private set
    }
}