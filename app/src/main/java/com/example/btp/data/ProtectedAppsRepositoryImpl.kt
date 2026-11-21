package com.example.btp.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProtectedAppsRepositoryImpl(
    private val context: Context
) : ProtectedAppsRepository {

    companion object {
        val KEY_PROTECTED_PACKAGES = stringSetPreferencesKey("protected_package_names")
    }

    private var cachedInstalledApps: List<AppInfo>? = null

    override fun getProtectedPackageNames(): Flow<Set<String>> {
        return context.voiceDataStore.data.map { prefs ->
            val set = prefs[KEY_PROTECTED_PACKAGES] ?: emptySet()
            LockManager.setProtectedPackages(context, set)
            set
        }
    }

    override fun getInstalledApps(searchQuery: String): Flow<List<AppInfo>> {
        return getProtectedPackageNames().map { protectedSet ->
            val allApps = withContext(Dispatchers.IO) {
                cachedInstalledApps ?: loadInstalledApps().also { cachedInstalledApps = it }
            }
            val filtered = if (searchQuery.isBlank()) {
                allApps
            } else {
                allApps.filter { app ->
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                            app.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
            filtered.map { app ->
                app.copy(isProtected = protectedSet.contains(app.packageName))
            }
        }
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val selfPackage = context.packageName

        // Find home launcher package to avoid locking launcher
        val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val defaultHomePackage = try {
            packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }

        // Method 1: Get launcher intent activities
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = try {
            packageManager.queryIntentActivities(mainIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val launchablePackages = resolveInfoList.mapNotNull { it.activityInfo?.packageName }.toMutableSet()

        // Method 2: Fallback to getInstalledApplications
        if (launchablePackages.isEmpty()) {
            val installedApps = try {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            } catch (_: Exception) {
                emptyList()
            }
            for (app in installedApps) {
                if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                    launchablePackages.add(app.packageName)
                }
            }
        }

        return launchablePackages.mapNotNull { pkgName ->
            if (pkgName == selfPackage || pkgName == defaultHomePackage) return@mapNotNull null

            val appInfo = try {
                packageManager.getApplicationInfo(pkgName, 0)
            } catch (_: Exception) {
                return@mapNotNull null
            }

            val appLabel = packageManager.getApplicationLabel(appInfo).toString()
            val appIcon = try {
                packageManager.getApplicationIcon(appInfo)
            } catch (_: Exception) {
                null
            }

            AppInfo(
                packageName = pkgName,
                appName = if (appLabel.isNotBlank()) appLabel else pkgName,
                icon = appIcon,
                isProtected = false
            )
        }.sortedBy { it.appName.lowercase() }
    }

    override suspend fun toggleAppProtection(packageName: String, isProtected: Boolean) {
        if (isProtected) {
            LockManager.protectApp(context, packageName)
        } else {
            LockManager.unprotectApp(context, packageName)
        }
        context.voiceDataStore.edit { prefs ->
            val currentSet = prefs[KEY_PROTECTED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (isProtected) {
                currentSet.add(packageName)
            } else {
                currentSet.remove(packageName)
            }
            prefs[KEY_PROTECTED_PACKAGES] = currentSet
        }
    }

    override suspend fun selectAllApps(packageNames: List<String>) {
        packageNames.forEach { LockManager.protectApp(context, it) }
        context.voiceDataStore.edit { prefs ->
            val currentSet = prefs[KEY_PROTECTED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            currentSet.addAll(packageNames)
            prefs[KEY_PROTECTED_PACKAGES] = currentSet
        }
    }

    override suspend fun clearAllProtections() {
        LockManager.clearAllProtectedApps(context)
        context.voiceDataStore.edit { prefs ->
            prefs[KEY_PROTECTED_PACKAGES] = emptySet()
        }
    }
}
