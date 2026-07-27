package io.github.sterlingshell.yamff.common.ext

import kotlinx.coroutines.*

object AppScope : CoroutineScope {
    override val coroutineContext = SupervisorJob() + Dispatchers.Main
}

fun runMain(block: suspend CoroutineScope.() -> Unit) =
    AppScope.launch(block = block)

fun runIO(block: suspend CoroutineScope.() -> Unit) =
    AppScope.launch(Dispatchers.IO, block = block)
