package com.cydoniancitizen.bingee.app

import com.cydoniancitizen.bingee.BuildConfig
import com.cydoniancitizen.bingee.core.result.AppError
import com.cydoniancitizen.bingee.domain.repository.AppUpdateRepository
import com.cydoniancitizen.bingee.domain.repository.AppUpdateResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val available = AppUpdateResult.UpdateAvailable(
        BuildConfig.VERSION_NAME,
        "2.0.0",
        "https://github.com/CydonianCitizen/Bingee/releases/tag/v2.0.0"
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun openingChecksInstalledVersionAndShowsOnlyAvailableUpdate() = runTest(dispatcher) {
        val requested = mutableListOf<String>()
        val viewModel = AppUpdateViewModel(
            repository {
                requested += it
                available
            }
        )
        assertNull(viewModel.availableUpdate.value)
        viewModel.onAppOpened()
        advanceUntilIdle()
        assertEquals(listOf(BuildConfig.VERSION_NAME), requested)
        assertEquals(available, viewModel.availableUpdate.value)
    }

    @Test
    fun currentVersionAndAllFailuresRemainSilent() = runTest(dispatcher) {
        listOf(
            AppUpdateResult.UpToDate(BuildConfig.VERSION_NAME),
            AppUpdateResult.Error(AppError.NetworkUnavailable),
            AppUpdateResult.Error(AppError.RateLimited),
            AppUpdateResult.Error(AppError.InvalidRemoteResponse)
        ).forEach { result ->
            val viewModel = AppUpdateViewModel(repository { result })
            viewModel.onAppOpened()
            advanceUntilIdle()
            assertNull(viewModel.availableUpdate.value)
        }
        val throwing = AppUpdateViewModel(repository { error("Unexpected failure") })
        throwing.onAppOpened()
        advanceUntilIdle()
        assertNull(throwing.availableUpdate.value)
    }

    @Test
    fun repeatedOpeningAndRecreationDoNotDuplicateCheckOrDismissedPopup() = runTest(dispatcher) {
        var calls = 0
        val response = CompletableDeferred<AppUpdateResult>()
        val viewModel = AppUpdateViewModel(
            repository {
                calls++
                response.await()
            }
        )
        viewModel.onAppOpened()
        viewModel.onAppOpened()
        runCurrent()
        assertEquals(1, calls)
        response.complete(available)
        advanceUntilIdle()
        viewModel.onAppOpened()
        assertEquals(available, viewModel.availableUpdate.value)
        viewModel.dismissUpdate()
        viewModel.onAppOpened()
        advanceUntilIdle()
        assertNull(viewModel.availableUpdate.value)
        assertEquals(1, calls)
    }

    @Test
    fun reopeningChecksAgainAfterDismissalOrOfflineFailure() = runTest(dispatcher) {
        var calls = 0
        val viewModel = AppUpdateViewModel(
            repository {
                calls++
                if (calls == 1) AppUpdateResult.Error(AppError.NetworkUnavailable) else available
            }
        )
        repeat(3) {
            viewModel.onAppOpened()
            advanceUntilIdle()
            if (it > 0) assertEquals(available, viewModel.availableUpdate.value)
            viewModel.dismissUpdate()
            viewModel.onAppClosed()
        }
        assertEquals(3, calls)
    }

    @Test
    fun backgroundingCancelsRequestAndClearsPopupBeforeNextOpen() = runTest(dispatcher) {
        var calls = 0
        val response = CompletableDeferred<AppUpdateResult>()
        val viewModel = AppUpdateViewModel(
            repository {
                calls++
                if (calls == 1) response.await() else available
            }
        )
        viewModel.onAppOpened()
        runCurrent()
        viewModel.onAppClosed()
        response.complete(available)
        advanceUntilIdle()
        assertNull(viewModel.availableUpdate.value)
        viewModel.onAppOpened()
        advanceUntilIdle()
        assertEquals(available, viewModel.availableUpdate.value)
        viewModel.onAppClosed()
        assertNull(viewModel.availableUpdate.value)
    }

    @Test
    fun hangingCheckTimesOutSilentlyAndNextOpenCanRetry() = runTest(dispatcher) {
        var calls = 0
        var cancelled = false
        val viewModel = AppUpdateViewModel(
            repository {
                calls++
                if (calls == 1) {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                } else {
                    available
                }
            }
        )
        viewModel.onAppOpened()
        runCurrent()
        advanceTimeBy(10_001)
        runCurrent()
        assertEquals(true, cancelled)
        assertNull(viewModel.availableUpdate.value)
        viewModel.onAppClosed()
        viewModel.onAppOpened()
        advanceUntilIdle()
        assertEquals(available, viewModel.availableUpdate.value)
    }

    private fun repository(check: suspend (String) -> AppUpdateResult): AppUpdateRepository =
        object : AppUpdateRepository {
            override suspend fun checkForUpdates(installedVersionName: String): AppUpdateResult =
                check(installedVersionName)
        }
}
