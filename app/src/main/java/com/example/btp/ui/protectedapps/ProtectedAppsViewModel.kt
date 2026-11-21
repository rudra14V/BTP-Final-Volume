package com.example.btp.ui.protectedapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.btp.data.AppInfo
import com.example.btp.data.ProtectedAppsRepository
import com.example.btp.data.ProtectedAppsRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProtectedAppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val protectedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false
)

class ProtectedAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProtectedAppsRepository = ProtectedAppsRepositoryImpl(application)
    private val searchQueryFlow = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProtectedAppsUiState> = searchQueryFlow
        .flatMapLatest { query ->
            combine(
                repository.getInstalledApps(query),
                repository.getProtectedPackageNames()
            ) { apps, protectedSet ->
                ProtectedAppsUiState(
                    apps = apps,
                    searchQuery = query,
                    protectedCount = protectedSet.size,
                    totalCount = apps.size,
                    isLoading = false
                )
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProtectedAppsUiState(isLoading = true)
        )

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun toggleAppProtection(packageName: String, isProtected: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleAppProtection(packageName, isProtected)
        }
    }

    fun selectAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val visiblePackageNames = uiState.value.apps.map { it.packageName }
            repository.selectAllApps(visiblePackageNames)
        }
    }

    fun clearAllProtections() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllProtections()
        }
    }
}
