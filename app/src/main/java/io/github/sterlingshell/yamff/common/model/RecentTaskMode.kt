package io.github.sterlingshell.yamff.common.model

enum class RecentTaskMode {
    /**
     * No intervention; let the system handle task visibility and snapshots.
     */
    NATIVE,
    /**
     * Completely remove small window tasks from the Recents list.
     */
    HIDDEN,
    /**
     * Show tasks with customized snapshots (including window frames) and redirect clicks.
     */
    DECORATED
}
