package io.github.sterlingshell.yamff.xposed.core

import io.github.sterlingshell.yamff.xposed.IOpenCountListener
import io.github.sterlingshell.yamff.xposed.window.Window
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

object FreeformManager {
    private val windowList = CopyOnWriteArrayList<Int>()
    private val taskToDisplayMap = ConcurrentHashMap<Int, Int>()
    
    // Map of displayId to a cleanup function
    private val windowCleanupMap = ConcurrentHashMap<Int, () -> Unit>()
    
    // Map of displayId to the Window instance
    private val activeWindows = ConcurrentHashMap<Int, Window>()
    
    // Map of displayId to the primary package name running in it
    private val displayToPackageMap = ConcurrentHashMap<Int, String>()
    
    // Set of tasks that are or were in freeform
    private val formerTasks = ConcurrentHashMap.newKeySet<Int>()
    
    val openWindowCount: Int get() = windowList.size
        
    private val iOpenCountListenerSet = CopyOnWriteArraySet<IOpenCountListener>()

    fun addWindow(displayId: Int, window: Window, cleanup: () -> Unit) {
        windowList.add(0, displayId)
        activeWindows[displayId] = window
        windowCleanupMap[displayId] = cleanup
        notifyListeners()
    }

    fun removeWindow(displayId: Int) {
        if (windowList.remove(displayId)) {
            activeWindows.remove(displayId)
            windowCleanupMap.remove(displayId)
            displayToPackageMap.remove(displayId)
            // Note: we DO NOT remove from formerTasks here, so we can still optimize snapshots
            val toRemove = taskToDisplayMap.filter { it.value == displayId }.keys
            toRemove.forEach { taskToDisplayMap.remove(it) }
            notifyListeners()
        }
    }
    
    fun getWindow(displayId: Int): Window? = activeWindows[displayId]

    fun getDisplayIdForTask(taskId: Int): Int? = taskToDisplayMap[taskId]

    fun associateTaskWithDisplay(taskId: Int, displayId: Int) {
        taskToDisplayMap[taskId] = displayId
        formerTasks.add(taskId)
    }

    fun isTaskFormerFreeform(taskId: Int): Boolean = formerTasks.contains(taskId)
    
    fun associatePackageWithDisplay(packageName: String, displayId: Int) {
        displayToPackageMap[displayId] = packageName
    }

    fun isTaskInWindow(taskId: Int): Boolean {
        return taskToDisplayMap.containsKey(taskId)
    }

    fun moveToTop(displayId: Int) {
        if (windowList.remove(displayId)) {
            windowList.add(0, displayId)
        }
    }

    fun closeAllWindows() {
        // Use a copy of keys to avoid concurrent modification if cleanup calls removeWindow
        val ids = windowCleanupMap.keys().toList()
        ids.forEach { id ->
            windowCleanupMap[id]?.invoke()
        }
    }
    
    fun closeWindowByPackage(packageName: String) {
        displayToPackageMap.filter { it.value == packageName }.keys.forEach { id ->
            windowCleanupMap[id]?.invoke()
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
