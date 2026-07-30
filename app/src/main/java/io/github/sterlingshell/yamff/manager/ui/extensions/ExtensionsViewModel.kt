package io.github.sterlingshell.yamff.manager.ui.extensions

import androidx.lifecycle.ViewModel
import io.github.sterlingshell.yamff.data.bridge.ExtensionsBridge
import io.github.sterlingshell.yamff.xposed.sys.ExtensionMetadata
import kotlinx.coroutines.flow.StateFlow

class ExtensionsViewModel(
    private val extensionsBridge: ExtensionsBridge
) : ViewModel() {
    
    val extensions: StateFlow<List<ExtensionMetadata>> = extensionsBridge.extensions

    fun refresh() = extensionsBridge.refresh()

    fun toggleAuthorization(packageName: String, authorized: Boolean) {
        extensionsBridge.toggleAuthorization(packageName, authorized)
    }
}
