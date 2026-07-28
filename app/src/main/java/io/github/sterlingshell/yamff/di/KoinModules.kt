package io.github.sterlingshell.yamff.di

import io.github.sterlingshell.yamff.xposed.core.ConfigManager
import io.github.sterlingshell.yamff.xposed.core.ExtensionRegistry
import io.github.sterlingshell.yamff.xposed.core.FreeformManager
import org.koin.dsl.module

val coreModule = module {
    single { ExtensionRegistry() } 
    single { ConfigManager() }
    single { FreeformManager }
}

val appModule = module {
    // Manager App specific dependencies
}
