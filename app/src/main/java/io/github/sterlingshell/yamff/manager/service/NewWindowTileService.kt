package io.github.sterlingshell.yamff.manager.service

import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager

class NewWindowTileService : TileService() {
    override fun onClick() {
        super.onClick()
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("useAppList", true))
            IpcProxy.openAppList()
        else IpcProxy.createWindow()
    }
}
