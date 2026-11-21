package com.example.btp.data

import kotlinx.coroutines.flow.Flow

interface ProtectedAppsRepository {
    /**
     * Flow emitting the set of currently protected application package names.
     */
    fun getProtectedPackageNames(): Flow<Set<String>>

    /**
     * Flow emitting the list of installed applications with their protection state,
     * filtered by search query.
     */
    fun getInstalledApps(searchQuery: String = ""): Flow<List<AppInfo>>

    /**
     * Toggles protection state for a specific package name.
     */
    suspend fun toggleAppProtection(packageName: String, isProtected: Boolean)

    /**
     * Protects all given package names.
     */
    suspend fun selectAllApps(packageNames: List<String>)

    /**
     * Clears protection for all applications.
     */
    suspend fun clearAllProtections()
}
