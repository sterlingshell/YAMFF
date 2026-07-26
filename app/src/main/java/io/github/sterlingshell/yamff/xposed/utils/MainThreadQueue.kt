package io.github.sterlingshell.yamff.xposed.utils

import android.util.Log
import io.github.sterlingshell.yamff.common.extensions.runMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

object MainThreadQueue {
    private val channel = Channel<suspend CoroutineScope.() -> Unit>(Channel.UNLIMITED)

    init {
        runMain {
            for (task in channel) {
                runCatching {
                    task()
                }.onFailure { e ->
                    Log.e("MainThreadQueue", "Error executing task in MainThreadQueue", e)
                }
            }
        }
    }

    fun add(run: suspend CoroutineScope.() -> Unit) {
        channel.trySend(run)
    }
}
