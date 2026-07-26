package io.github.sterlingshell.yamff.manager.services

import android.service.quicksettings.TileService

class QSEnterTileService: TileService() {
    override fun onClick() {
        super.onClick()
        YAMFFManagerProxy.currentToWindow()
    }
}
