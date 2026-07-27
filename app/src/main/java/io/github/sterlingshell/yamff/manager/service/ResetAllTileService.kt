package io.github.sterlingshell.yamff.manager.service

import android.service.quicksettings.TileService

class ResetAllTileService: TileService() {
    override fun onClick() {
        IpcProxy.resetAllWindow()
    }
}
