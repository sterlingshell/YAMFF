package io.github.sterlingshell.yamff.manager

import android.app.Application
import com.google.android.material.color.DynamicColors
import io.github.sterlingshell.yamff.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

object AppHolder {
    lateinit var app: Application
        private set

    internal fun init(app: Application) {
        this.app = app
    }
}

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        AppHolder.init(this)
        DynamicColors.applyToActivitiesIfAvailable(this)

        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }
}
