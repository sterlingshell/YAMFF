package io.github.sterlingshell.yamff.xposed.services

import io.github.sterlingshell.yamff.xposed.IOpenCountListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

object YAMFFWindowManager {
    private val windowList = CopyOnWriteArrayList<Int>()
    private val taskToDisplayMap = ConcurrentHashMap<Int, Int>()
    
    // BUG Fix: Use windowList.size directly for atomic counting
    val openWindowCount: Int get() = windowList.size
        
    private val iOpenCountListenerSet = CopyOnWriteArraySet<IOpenCountListener>()

    fun addWindow(displayId: Int) {
        windowList.add(0, displayId)
        notifyListeners()
    }

    fun removeWindow(displayId: Int) {
        if (windowList.remove(displayId)) {
            val toRemove = taskToDisplayMap.filter { it.value == displayId }.keys
            toRemove.forEach { taskToDisplayMap.remove(it) }
            notifyListeners()
        }
    }

    fun associateTaskWithDisplay(taskId: Int, displayId: Int) {
        taskToDisplayMap[taskId] = displayId
    }

    fun isTaskInWindow(taskId: Int): Boolean {
        return taskToDisplayMap.containsKey(taskId)
    }

    fun isTop(displayId: Int): Boolean {
        return windowList.firstOrNull() == displayId
    }

    fun moveToTop(displayId: Int) {
        if (windowList.remove(displayId)) {
            windowList.add(0, displayId)
        }
    }

    fun getWindowList(): List<Int> = windowList

    fun registerOpenCountListener(listener: IOpenCountListener) {
        iOpenCountListenerSet.add(listener)
        runCatching { listener.onUpdate(openWindowCount) }
    }

    fun unregisterOpenCountListener(listener: IOpenCountListener?) {
        iOpenCountListenerSet.remove(listener)
    }

    private fun notifyListeners() {
        iOpenCountListenerSet.forEach { listener ->
            runCatching {
                listener.onUpdate(openWindowCount)
            }.onFailure {
                // Keep the listener in set, let register/unregister handle it or 
                // handle it gracefully. The previous logic was also removing on failure.
                // However, with CopyOnWriteArraySet, iteration is on a snapshot.
            }
        }
    }
}
