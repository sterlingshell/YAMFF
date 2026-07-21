package io.github.duzhaokun123.yamf.manager.services

import android.service.quicksettings.TileService

class QSResetAllTileService: TileService() {
    override fun onClick() {
        YAMFManagerProxy.resetAllWindow()
    }
}
