package com.nexalarm.app.util

/**
 * Feature flags for simulating free vs premium tiers.
 * Toggle isPremium to unlock unlimited folders.
 */
object FeatureFlags {
    var isPremium: Boolean = false

    const val FREE_FOLDER_LIMIT = 10

    fun canCreateFolder(currentUserFolderCount: Int): Boolean {
        return isPremium || currentUserFolderCount < FREE_FOLDER_LIMIT
    }
}
