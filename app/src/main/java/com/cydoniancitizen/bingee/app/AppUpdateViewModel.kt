package com.cydoniancitizen.bingee.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.bingee.BuildConfig
import com.cydoniancitizen.bingee.domain.repository.AppUpdateRepository
import com.cydoniancitizen.bingee.domain.repository.AppUpdateResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
internal class AppUpdateViewModel @Inject constructor(private val repository: AppUpdateRepository) : ViewModel() {
    private val mutableAvailableUpdate = MutableStateFlow<AppUpdateResult.UpdateAvailable?>(null)
    val availableUpdate = mutableAvailableUpdate.asStateFlow()
    private var isOpen = false
    private var checkJob: Job? = null

    fun onAppOpened() {
        if (isOpen) return
        isOpen = true
        checkJob = viewModelScope.launch {
            val result = try {
                withTimeoutOrNull(10_000) { repository.checkForUpdates(BuildConfig.VERSION_NAME) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            mutableAvailableUpdate.value = result as? AppUpdateResult.UpdateAvailable
        }
    }

    fun onAppClosed() {
        isOpen = false
        checkJob?.cancel()
        dismissUpdate()
    }

    fun dismissUpdate() {
        mutableAvailableUpdate.value = null
    }
}
