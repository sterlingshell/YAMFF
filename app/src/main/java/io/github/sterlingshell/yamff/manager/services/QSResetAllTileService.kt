package io.github.sterlingshell.yamff.manager.services

import android.service.quicksettings.TileService

class QSResetAllTileService: TileService() {
    override fun onClick() {
        YAMFFManagerProxy.resetAllWindow()
    }
}
