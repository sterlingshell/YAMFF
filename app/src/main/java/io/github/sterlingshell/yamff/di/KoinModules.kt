package io.github.sterlingshell.yamff.di

import io.github.sterlingshell.yamff.data.bridge.ConfigBridge
import io.github.sterlingshell.yamff.data.bridge.ExtensionsBridge
import io.github.sterlingshell.yamff.manager.ui.home.HomeViewModel
import io.github.sterlingshell.yamff.manager.ui.features.settings.SettingsViewModel
import io.github.sterlingshell.yamff.manager.ui.extensions.ExtensionsViewModel
import io.github.sterlingshell.yamff.xposed.core.ConfigManager
import io.github.sterlingshell.yamff.xposed.core.ExtensionRegistry
import io.github.sterlingshell.yamff.xposed.core.FreeformManager
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coreModule = module {
    single { ExtensionRegistry() } 
    single { ConfigManager() }
    single { FreeformManager }
}

val appModule = module {
    single { ConfigBridge() }
    single { ExtensionsBridge(get()) }
    
    viewModel { HomeViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { ExtensionsViewModel(get()) }
}
