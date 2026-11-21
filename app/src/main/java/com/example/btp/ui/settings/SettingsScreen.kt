package com.example.btp.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btp.data.SecuritySettings
import com.example.btp.data.ThresholdValidationStatus
import com.example.btp.ui.theme.BTPTheme
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onNavigateToEnrollment: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkAccessibilityStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SettingsContent(
        state = state,
        onSimilarityThresholdChange = { viewModel.updateSimilarityThreshold(it) },
        onGracePeriodChange = { viewModel.updateGracePeriod(it) },
        onMaxFailedAttemptsChange = { viewModel.updateMaxFailedAttempts(it) },
        onCooldownDurationChange = { viewModel.updateCooldownDuration(it) },
        onEnrollmentSamplesCountChange = { viewModel.updateEnrollmentSamplesCount(it) },
        onResetDefaults = { viewModel.resetToDefaults() },
        onClearVoiceProfile = { viewModel.clearVoiceProfile() },
        onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
        onOpenOverlaySettings = { viewModel.openOverlaySettings() },
        onNavigateToEnrollment = onNavigateToEnrollment,
        modifier = modifier
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onSimilarityThresholdChange: (Float) -> Unit,
    onGracePeriodChange: (Int) -> Unit,
    onMaxFailedAttemptsChange: (Int) -> Unit,
    onCooldownDurationChange: (Int) -> Unit,
    onEnrollmentSamplesCountChange: (Int) -> Unit,
    onResetDefaults: () -> Unit,
    onClearVoiceProfile: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onNavigateToEnrollment: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearProfileDialog by remember { mutableStateOf(false) }

    if (showClearProfileDialog) {
        AlertDialog(
            onDismissRequest = { showClearProfileDialog = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Voice Profile?") },
            text = { Text("This will permanently delete your encrypted voice embeddings. You will need to re-enroll to use Voice Lock.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearVoiceProfile()
                        showClearProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Screen Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Security & Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Configure voice recognition thresholds and lock parameters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Accessibility Service Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isAccessibilityEnabled) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AccessibilityNew,
                                contentDescription = null,
                                tint = if (state.isAccessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Accessibility Service",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Status pill
                        Surface(
                            shape = CircleShape,
                            color = if (state.isAccessibilityEnabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isAccessibilityEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = if (state.isAccessibilityEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isAccessibilityEnabled) "Active" else "Disabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isAccessibilityEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (state.isAccessibilityEnabled) {
                            "VoiceLock accessibility service is active and monitoring protected app launches."
                        } else {
                            "Accessibility Service is required to detect protected app launches and present the voice unlock overlay."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!state.isAccessibilityEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenAccessibilitySettings,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable in Accessibility Settings")
                        }
                    }
                }
            }

            // 2. Display Over Other Apps Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isOverlayEnabled) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = null,
                                tint = if (state.isOverlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Display Over Other Apps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Status pill
                        Surface(
                            shape = CircleShape,
                            color = if (state.isOverlayEnabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isOverlayEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = if (state.isOverlayEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isOverlayEnabled) "Granted" else "Missing",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isOverlayEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (state.isOverlayEnabled) {
                            "Display over other apps permission is granted. Voice Lock window can present over protected applications."
                        } else {
                            "Permission to display over other apps is required so the voice authentication window can block unauthorized access."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!state.isOverlayEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenOverlaySettings,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Overlay Permission")
                        }
                    }
                }
            }

            // 3. Voice Profile Summary & Enrollment Config Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Voice Profile Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (state.isVoiceRegistered) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (state.isVoiceRegistered) "Registered" else "Not Registered",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isVoiceRegistered) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (state.isVoiceRegistered) {
                            "Your voice profile embeddings are encrypted using AES-256-GCM and safely stored in device local storage."
                        } else {
                            "No enrolled voice profile found. Please enroll your voice before enabling app locks."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enrollment Samples Config Selector (2, 3, or 5 recordings)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice Enrollment Samples",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Number of passphrase recordings required when enrolling voice profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecuritySettings.ENROLLMENT_SAMPLES_OPTIONS.forEach { option ->
                                val selected = state.settings.enrollmentSamplesCount == option
                                FilterChip(
                                    selected = selected,
                                    onClick = { onEnrollmentSamplesCountChange(option) },
                                    label = { Text("$option recordings") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToEnrollment,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isVoiceRegistered) "Re-enroll" else "Enroll Now")
                        }

                        if (state.isVoiceRegistered) {
                            OutlinedButton(
                                onClick = { showClearProfileDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Profile")
                            }
                        }
                    }
                }
            }

            // 4. Security Thresholds Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Matching & Lock Thresholds",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // A. Similarity Threshold Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Similarity Threshold",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "%.2f (%d%%)".format(
                                    state.settings.similarityThreshold,
                                    (state.settings.similarityThreshold * 100).roundToInt()
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = state.settings.similarityThreshold,
                            onValueChange = onSimilarityThresholdChange,
                            valueRange = SecuritySettings.MIN_SIMILARITY_THRESHOLD..SecuritySettings.MAX_SIMILARITY_THRESHOLD,
                            steps = 44, // 0.01 increment steps
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Threshold Validation Status Pill
                        val statusColor: Color = when (state.thresholdValidationStatus) {
                            ThresholdValidationStatus.UNSAFE_LOW -> MaterialTheme.colorScheme.error
                            ThresholdValidationStatus.MODERATE -> MaterialTheme.colorScheme.secondary
                            ThresholdValidationStatus.RECOMMENDED -> MaterialTheme.colorScheme.primary
                            ThresholdValidationStatus.STRICT -> MaterialTheme.colorScheme.tertiary
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (state.thresholdValidationStatus == ThresholdValidationStatus.UNSAFE_LOW) Icons.Rounded.Warning else Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = state.thresholdValidationStatus.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    Text(
                                        text = state.thresholdValidationStatus.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // B. Grace Period Selector
                    Column {
                        Text(
                            text = "Unlock Grace Period",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Time window after unlocking during which re-opening protected apps won't re-trigger voice prompt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecuritySettings.GRACE_PERIOD_OPTIONS.forEach { option ->
                                val selected = state.settings.gracePeriodSeconds == option
                                FilterChip(
                                    selected = selected,
                                    onClick = { onGracePeriodChange(option) },
                                    label = { Text(if (option == 0) "None (Every time)" else "${option}s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // C. Max Failed Attempts
                    Column {
                        Text(
                            text = "Max Failed Attempts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Maximum wrong voice verification attempts permitted before temporary lockout.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecuritySettings.MAX_FAILED_ATTEMPTS_OPTIONS.forEach { option ->
                                val selected = state.settings.maxFailedAttempts == option
                                FilterChip(
                                    selected = selected,
                                    onClick = { onMaxFailedAttemptsChange(option) },
                                    label = { Text("$option attempts") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // D. Cooldown / Lockout Duration
                    Column {
                        Text(
                            text = "Lockout Cooldown Duration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Duration user must wait before trying again after exceeding max failed attempts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecuritySettings.COOLDOWN_OPTIONS.forEach { option ->
                                val selected = state.settings.cooldownDurationSeconds == option
                                FilterChip(
                                    selected = selected,
                                    onClick = { onCooldownDurationChange(option) },
                                    label = { Text("${option}s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Reset Defaults Button
                    OutlinedButton(
                        onClick = onResetDefaults,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Security Settings to Defaults")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    BTPTheme {
        SettingsContent(
            state = SettingsUiState(
                settings = SecuritySettings(),
                isVoiceRegistered = true,
                isAccessibilityEnabled = false,
                isOverlayEnabled = false
            ),
            onSimilarityThresholdChange = {},
            onGracePeriodChange = {},
            onMaxFailedAttemptsChange = {},
            onCooldownDurationChange = {},
            onEnrollmentSamplesCountChange = {},
            onResetDefaults = {},
            onClearVoiceProfile = {},
            onOpenAccessibilitySettings = {},
            onOpenOverlaySettings = {},
            onNavigateToEnrollment = {}
        )
    }
}
