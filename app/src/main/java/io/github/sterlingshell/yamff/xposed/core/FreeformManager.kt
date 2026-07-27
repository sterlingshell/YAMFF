package io.github.sterlingshell.yamff.xposed.core

import io.github.sterlingshell.yamff.xposed.IOpenCountListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

object FreeformManager {
    private val windowList = CopyOnWriteArrayList<Int>()
    private val taskToDisplayMap = ConcurrentHashMap<Int, Int>()
    
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
            }
        }
    }
}
