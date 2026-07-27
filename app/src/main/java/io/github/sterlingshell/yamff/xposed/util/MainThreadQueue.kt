package io.github.sterlingshell.yamff.xposed.util

import io.github.sterlingshell.yamff.common.ext.runMain
import java.util.concurrent.ConcurrentLinkedQueue

object MainThreadQueue {
    private val queue = ConcurrentLinkedQueue<suspend () -> Unit>()
    private var isProcessing = false

    fun add(block: suspend () -> Unit) {
        queue.add(block)
        process()
    }

    private fun process() {
        if (isProcessing) return
        isProcessing = true
        runMain {
            while (queue.isNotEmpty()) {
                queue.poll()?.invoke()
            }
            isProcessing = false
        }
    }
}
