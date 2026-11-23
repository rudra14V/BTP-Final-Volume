package com.example.btp.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.btp.MainActivity
import com.example.btp.R
import com.example.btp.data.LockManager
import com.example.btp.data.ProtectedAppsRepositoryImpl
import com.example.btp.data.SecuritySettings
import com.example.btp.data.SettingsRepositoryImpl
import com.example.btp.ui.lock.VoiceLockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppLockService"
        private const val CHANNEL_ID = "voice_lock_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var defaultHomePackage: String? = null

    @Volatile
    private var cachedSettings: SecuritySettings = SecuritySettings()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AppLockAccessibilityService connected and active")
        LockManager.init(applicationContext)
        resolveDefaultHomePackage()
        startServiceInForeground()

        val protectedRepo = ProtectedAppsRepositoryImpl(applicationContext)
        val settingsRepo = SettingsRepositoryImpl(applicationContext)

        // Observe DataStore for protected packages and settings
        serviceScope.launch {
            protectedRepo.getProtectedPackageNames().collect { set ->
                LockManager.setProtectedPackages(applicationContext, set)
                Log.d(TAG, "Sync protected packages: count=${set.size}")
            }
        }

        serviceScope.launch {
            settingsRepo.getSettings().collect { settings ->
                cachedSettings = settings
                Log.d(TAG, "Sync settings: gracePeriod=${settings.gracePeriodSeconds}")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Only intercept window state changes (app/activity launches)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName.isBlank()) return

        checkAndLockPackage(pkgName)
    }

    override fun onInterrupt() {
        Log.w(TAG, "AppLockAccessibilityService interrupted")
    }

    private fun checkAndLockPackage(pkgName: String) {
        // Skip BTP app itself and VoiceLockActivity
        if (pkgName == packageName) return

        // Skip launcher / home app
        if (defaultHomePackage == null) resolveDefaultHomePackage()
        if (pkgName == defaultHomePackage) {
            LockManager.setCurrentlyVerifying(null)
            return
        }

        // Fast check using LockManager in-memory / SharedPreferences set
        if (!LockManager.isProtected(applicationContext, pkgName)) {
            return
        }

        // Check if unlocked within grace period
        if (LockManager.isAppUnlocked(pkgName, cachedSettings.gracePeriodSeconds)) {
            return
        }

        // Prevent duplicate activity launch loops
        if (LockManager.getCurrentlyVerifying() == pkgName) {
            return
        }

        Log.d(TAG, "Locking protected app: $pkgName")
        LockManager.setCurrentlyVerifying(pkgName)

        val lockIntent = Intent(applicationContext, VoiceLockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(VoiceLockActivity.EXTRA_PACKAGE_NAME, pkgName)
        }
        try {
            startActivity(lockIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch VoiceLockActivity for $pkgName", e)
            LockManager.setCurrentlyVerifying(null)
        }
    }

    private fun startServiceInForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Voice Lock Active Protection",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps voice app lock protection active in background"
                }
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Lock Protection Active")
                .setContentText("Monitoring protected app launches")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground notification", e)
        }
    }

    private fun resolveDefaultHomePackage() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            defaultHomePackage = resolveInfo?.activityInfo?.packageName
            Log.d(TAG, "Resolved default launcher: $defaultHomePackage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve default home package", e)
        }
    }
}
