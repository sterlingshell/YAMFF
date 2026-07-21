package io.github.duzhaokun123.yamf.manager.services

import android.service.quicksettings.TileService

class QSEnterTileService: TileService() {
    override fun onClick() {
        super.onClick()
        YAMFManagerProxy.currentToWindow()
    }
}
