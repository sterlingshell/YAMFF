package io.github.sterlingshell.yamff.manager.services

import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager

class QSNewWindowTileService : TileService() {
    override fun onClick() {
        super.onClick()
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("useAppList", true))
            YAMFFManagerProxy.openAppList()
        else YAMFFManagerProxy.createWindow()
    }
}
