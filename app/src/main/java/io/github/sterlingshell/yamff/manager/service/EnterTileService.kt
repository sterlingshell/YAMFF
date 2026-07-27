package io.github.sterlingshell.yamff.manager.service

import android.service.quicksettings.TileService

class EnterTileService: TileService() {
    override fun onClick() {
        super.onClick()
        IpcProxy.currentToWindow()
    }
}
