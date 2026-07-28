package io.github.sterlingshell.yamff.xposed.sys

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.github.sterlingshell.yamff.common.Constants

data class ExtensionMetadata(
    val packageName: String,
    val label: String,
    val settingsComponent: String?,
    val isAuthorized: Boolean
)

object ExtensionDiscovery {
    fun discover(context: Context, authorizedPackages: Set<String>): List<ExtensionMetadata> {
        val pm = context.packageManager
        val intent = Intent(Constants.ACTION_FREEFORM_EXTENSION)
        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        }

        return resolveInfos.map { info ->
            val ai = info.activityInfo
            ExtensionMetadata(
                packageName = ai.packageName,
                label = ai.loadLabel(pm).toString(),
                settingsComponent = ai.name,
                isAuthorized = authorizedPackages.contains(ai.packageName)
            )
        }
    }
}
