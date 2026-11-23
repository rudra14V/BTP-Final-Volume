package com.example.btp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

object LockManager {

    private const val PREFS_NAME = "lock_manager_protected_apps_store"
    private const val KEY_PACKAGES = "protected_packages_set"

    // Set of package names that are currently marked as protected
    private val protectedPackages = ConcurrentHashMap.newKeySet<String>()

    // Map of packageName -> timestamp (millis) when it was unlocked
    private val unlockedApps = ConcurrentHashMap<String, Long>()

    // Track the package currently undergoing voice verification to avoid launch loops
    @Volatile
    private var currentlyVerifyingPackage: String? = null

    @Volatile
    private var verifyingTimestamp: Long = 0L

    // Track failed verification attempt counts per package
    private val failedAttemptsMap = ConcurrentHashMap<String, Int>()

    // Track lockout cooldown expiration timestamp per package
    private val lockoutCooldownMap = ConcurrentHashMap<String, Long>()

    private val _unlockedAppsFlow = MutableStateFlow<Set<String>>(emptySet())
    val unlockedAppsFlow: StateFlow<Set<String>> = _unlockedAppsFlow.asStateFlow()

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedSet = prefs.getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
            if (savedSet.isNotEmpty()) {
                protectedPackages.clear()
                protectedPackages.addAll(savedSet)
            }
        } catch (_: Exception) {}
    }

    fun setProtectedPackages(context: Context?, packages: Set<String>) {
        protectedPackages.clear()
        protectedPackages.addAll(packages)
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putStringSet(KEY_PACKAGES, packages).apply()
            } catch (_: Exception) {}
        }
    }

    fun protectApp(context: Context?, packageName: String) {
        protectedPackages.add(packageName)
        relockApp(packageName) // Force immediate lock when protected
        if (context != null) {
            setProtectedPackages(context, protectedPackages)
        }
    }

    fun unprotectApp(context: Context?, packageName: String) {
        protectedPackages.remove(packageName)
        relockApp(packageName)
        if (context != null) {
            setProtectedPackages(context, protectedPackages)
        }
    }

    fun clearAllProtectedApps(context: Context?) {
        protectedPackages.clear()
        unlockedApps.clear()
        if (context != null) {
            setProtectedPackages(context, emptySet())
        }
        updateFlow()
    }

    fun isProtected(packageName: String): Boolean {
        return protectedPackages.contains(packageName)
    }

    fun isProtected(context: Context?, packageName: String): Boolean {
        if (protectedPackages.contains(packageName)) return true
        if (context != null) {
            init(context)
            if (protectedPackages.contains(packageName)) return true
        }
        return false
    }

    fun isAppUnlocked(packageName: String, gracePeriodSeconds: Int): Boolean {
        val lastUnlocked = unlockedApps[packageName] ?: return false
        val now = System.currentTimeMillis()

        // When user selects "None (Every time)", keep the app unlocked for 30 seconds
        // while active so that internal activity/window changes do not constantly re-trigger the lock.
        val effectiveGracePeriod = if (gracePeriodSeconds <= 0) 30 else gracePeriodSeconds

        val elapsedSeconds = (now - lastUnlocked) / 1000L
        val isUnlocked = elapsedSeconds < effectiveGracePeriod

        if (!isUnlocked) {
            unlockedApps.remove(packageName)
            updateFlow()
        }
        return isUnlocked
    }

    fun grantAccess(packageName: String) {
        unlockedApps[packageName] = System.currentTimeMillis()
        failedAttemptsMap.remove(packageName)
        lockoutCooldownMap.remove(packageName)
        // Keep currentlyVerifying set with fresh timestamp for cooldown window
        verifyingTimestamp = System.currentTimeMillis()
        updateFlow()
    }

    fun relockApp(packageName: String) {
        unlockedApps.remove(packageName)
        updateFlow()
    }

    fun setCurrentlyVerifying(packageName: String?) {
        currentlyVerifyingPackage = packageName
        verifyingTimestamp = if (packageName != null) System.currentTimeMillis() else 0L
    }

    fun getCurrentlyVerifying(): String? {
        val current = currentlyVerifyingPackage ?: return null
        val now = System.currentTimeMillis()
        if (now - verifyingTimestamp > 3_000L) {
            currentlyVerifyingPackage = null
            verifyingTimestamp = 0L
            return null
        }
        return current
    }

    fun registerFailedAttempt(packageName: String, maxFailedAttempts: Int, cooldownSeconds: Int): Int {
        val currentCount = (failedAttemptsMap[packageName] ?: 0) + 1
        failedAttemptsMap[packageName] = currentCount

        if (currentCount >= maxFailedAttempts) {
            val cooldownUntil = System.currentTimeMillis() + (cooldownSeconds * 1000L)
            lockoutCooldownMap[packageName] = cooldownUntil
        }
        return currentCount
    }

    fun getFailedAttempts(packageName: String): Int = failedAttemptsMap[packageName] ?: 0

    fun getRemainingCooldownSeconds(packageName: String): Int {
        val cooldownUntil = lockoutCooldownMap[packageName] ?: return 0
        val remainingMillis = cooldownUntil - System.currentTimeMillis()
        return if (remainingMillis > 0) (remainingMillis / 1000L).toInt() + 1 else 0
    }

    fun isLockedOut(packageName: String): Boolean {
        return getRemainingCooldownSeconds(packageName) > 0
    }

    fun resetLockout(packageName: String) {
        failedAttemptsMap.remove(packageName)
        lockoutCooldownMap.remove(packageName)
    }

    private fun updateFlow() {
        _unlockedAppsFlow.value = unlockedApps.keys.toSet()
    }
}
