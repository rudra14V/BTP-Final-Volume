package com.example.btp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.btp.data.ProtectedAppsRepositoryImpl
import com.example.btp.ui.protectedapps.ProtectedAppsScreen
import com.example.btp.ui.registration.VoiceRegistrationScreen
import com.example.btp.ui.settings.SettingsScreen
import com.example.btp.ui.theme.BTPTheme

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val protectedRepo = remember { ProtectedAppsRepositoryImpl(context) }
    val protectedPackages by protectedRepo.getProtectedPackageNames().collectAsState(initial = emptySet())
    val protectedCount = protectedPackages.size

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.HowToReg, contentDescription = "Voice Registration") },
                    label = { Text("Enrollment") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        if (protectedCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge { Text("$protectedCount") }
                                }
                            ) {
                                Icon(Icons.Rounded.Apps, contentDescription = "Protected Apps")
                            }
                        } else {
                            Icon(Icons.Rounded.Apps, contentDescription = "Protected Apps")
                        }
                    },
                    label = { Text("Apps") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            modifier = Modifier.padding(innerPadding),
            label = "TabTransition"
        ) { targetTab ->
            when (targetTab) {
                0 -> VoiceRegistrationScreen()
                1 -> ProtectedAppsScreen()
                2 -> SettingsScreen(
                    onNavigateToEnrollment = { selectedTab = 0 }
                )
            }
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    BTPTheme {
        MainScreen()
    }
}
