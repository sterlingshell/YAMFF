package io.github.sterlingshell.yamff.manager

import android.app.Application
import com.google.android.material.color.DynamicColors

object AppHolder {
    lateinit var app: Application
        private set

    internal fun init(app: Application) {
        this.app = app
    }
}

class YAMFFApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AppHolder.init(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
