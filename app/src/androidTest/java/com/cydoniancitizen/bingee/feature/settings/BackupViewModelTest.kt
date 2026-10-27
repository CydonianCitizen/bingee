package com.cydoniancitizen.bingee.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cydoniancitizen.bingee.core.model.MediaSource
import com.cydoniancitizen.bingee.core.model.MediaType
import com.cydoniancitizen.bingee.data.importexport.BACKUP_FORMAT_ID
import com.cydoniancitizen.bingee.data.importexport.BACKUP_SCHEMA_VERSION
import com.cydoniancitizen.bingee.data.importexport.BackupData
import com.cydoniancitizen.bingee.data.importexport.BackupDataStore
import com.cydoniancitizen.bingee.data.importexport.BackupDocument
import com.cydoniancitizen.bingee.data.importexport.BackupFailureKind
import com.cydoniancitizen.bingee.data.importexport.BackupFileGateway
import com.cydoniancitizen.bingee.data.importexport.BackupJsonCodec
import com.cydoniancitizen.bingee.data.importexport.BackupLibraryEntry
import com.cydoniancitizen.bingee.data.importexport.BackupMedia
import com.cydoniancitizen.bingee.data.importexport.BackupParseResult
import com.cydoniancitizen.bingee.data.importexport.BackupPreferences
import com.cydoniancitizen.bingee.data.importexport.BackupRef
import com.cydoniancitizen.bingee.data.importexport.BackupShareFileStore
import com.cydoniancitizen.bingee.data.importexport.BackupValidationResult
import com.cydoniancitizen.bingee.data.importexport.BackupValidator
import com.cydoniancitizen.bingee.data.library.local.BingeeDatabase
import com.cydoniancitizen.bingee.data.settings.DataStoreReleaseNotificationPreferences
import com.cydoniancitizen.bingee.data.settings.PortableUserPreferencesStore
import com.cydoniancitizen.bingee.domain.background.BackgroundWorkScheduler
import com.cydoniancitizen.bingee.testutil.TestCalendarDateSource
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic backups only; the validation dispatcher holds each block until the test releases it. */
@RunWith(AndroidJUnit4::class)
class BackupViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val exportedAt = Instant.parse("2026-08-04T10:00:00Z")
    private val today = LocalDate.of(2026, 8, 18)
    private val backupFile = File(context.cacheDir, "synthetic-backup-test.json")
    private lateinit var database: BingeeDatabase
    private lateinit var store: BackupDataStore
    private lateinit var validation: HeldDispatcher

    @Volatile private var sharedIntent: Intent? = null

    @Volatile private var rejectShare = false
    private var beforeShare: (() -> Unit)? = null
    private val exportContext = object : ContextWrapper(context) {
        override fun startActivity(intent: Intent) {
            beforeShare?.invoke()
            if (rejectShare) throw ActivityNotFoundException("synthetic missing share target")
            sharedIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        }
    }

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, BingeeDatabase::class.java).build()
        store = BackupDataStore(
            database,
            database.portableSnapshotDao(),
            database.releaseEventDao(),
            preferences(),
            PortableUserPreferencesStore(context, database, database.portableSnapshotDao())
        )
        validation = HeldDispatcher()
        val existing = BackupValidator.validate(document("1"), today) as BackupValidationResult.Success
        store.restore(existing.plan)
    }

    @After
    fun tearDown() {
        backupFile.delete()
        database.close()
    }

    @Test
    fun validationRunsOnItsOwnDispatcherAndNothingIsWrittenBeforeConfirmation() = runBlocking {
        val before = database.portableSnapshotDao().readSnapshot()
        val viewModel = viewModel()

        viewModel.importFrom(write(document("2")))
        viewModel.awaitOperation(BackupOperation.VALIDATING)
        // The validation block reached the injected dispatcher and waits there, off the main thread.
        validation.awaitHeld()
        assertEquals(before, database.portableSnapshotDao().readSnapshot())

        validation.release()
        assertNotNull(viewModel.awaitOperation(BackupOperation.PREVIEW_READY).preview)
        assertEquals(before, database.portableSnapshotDao().readSnapshot())

        viewModel.confirmRestore()
        viewModel.awaitOperation(BackupOperation.SUCCESS)
        val restored = database.portableSnapshotDao().readSnapshot()
        assertEquals(listOf("2"), restored.refs.map { it.externalId })
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun semanticFailureKeepsItsKindAndWritesNothing() = runBlocking {
        val before = database.portableSnapshotDao().readSnapshot()
        val viewModel = viewModel()
        val orphan = document("3").let { doc ->
            doc.copy(data = doc.data.copy(library = listOf(BackupLibraryEntry(ref("404"), exportedAt))))
        }

        viewModel.importFrom(write(orphan))
        viewModel.awaitOperation(BackupOperation.VALIDATING)
        validation.awaitHeld()
        validation.release()

        assertEquals(
            BackupFailureKind.MISSING_REFERENCE,
            viewModel.awaitOperation(BackupOperation.FAILURE).failure
        )
        viewModel.confirmRestore()
        assertEquals(before, database.portableSnapshotDao().readSnapshot())
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun cancellingDuringValidationOrPreviewAppliesNothing() = runBlocking {
        val before = database.portableSnapshotDao().readSnapshot()

        val cleared = viewModel()
        cleared.importFrom(write(document("4")))
        cleared.awaitOperation(BackupOperation.VALIDATING)
        validation.awaitHeld()
        cleared.viewModelScope.cancel()
        validation.release()
        cleared.confirmRestore()
        assertNotEquals(BackupOperation.PREVIEW_READY, cleared.uiState.value.operation)

        val dismissed = viewModel()
        dismissed.importFrom(write(document("5")))
        dismissed.awaitOperation(BackupOperation.VALIDATING)
        validation.awaitHeld()
        validation.release()
        dismissed.awaitOperation(BackupOperation.PREVIEW_READY)
        dismissed.cancelPreview()
        dismissed.confirmRestore()
        assertEquals(BackupOperation.IDLE, dismissed.uiState.value.operation)

        assertEquals(before, database.portableSnapshotDao().readSnapshot())
        dismissed.viewModelScope.cancel()
    }

    @Test
    fun savingAndSharingExportRestorableUserData() = runBlocking {
        val viewModel = viewModel(exportContext)
        try {
            viewModel.saveTo(Uri.fromFile(backupFile))
            viewModel.awaitOperation(BackupOperation.SUCCESS)
            val saved = backupFile.inputStream().use { BackupJsonCodec.parse(it) } as BackupParseResult.Success
            assertEquals(3, saved.document.schemaVersion)
            assertEquals(listOf("1"), saved.document.data.media.map { it.primaryRef.externalId })

            viewModel.dismissFeedback()
            viewModel.share()
            viewModel.awaitOperation(BackupOperation.SUCCESS)
            val intent = checkNotNull(sharedIntent)
            assertEquals(Intent.ACTION_SEND, intent.action)
            val uri = checkNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            val shared = context.contentResolver.openInputStream(uri).use {
                BackupJsonCodec.parse(checkNotNull(it))
            } as BackupParseResult.Success
            assertEquals(saved.document, shared.document)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun failedSaveAndShareReportWriteFailureAndAllowRetry() = runBlocking {
        val viewModel = viewModel(exportContext)
        try {
            viewModel.saveTo(Uri.fromFile(context.cacheDir))
            assertEquals(BackupFailureKind.WRITE_FAILED, viewModel.awaitOperation(BackupOperation.FAILURE).failure)
            viewModel.saveTo(Uri.fromFile(backupFile))
            assertEquals(null, viewModel.awaitOperation(BackupOperation.SUCCESS).failure)
            assertTrue(backupFile.isFile)

            rejectShare = true
            viewModel.share()
            assertEquals(BackupFailureKind.WRITE_FAILED, viewModel.awaitOperation(BackupOperation.FAILURE).failure)
            rejectShare = false
            viewModel.share()
            assertEquals(null, viewModel.awaitOperation(BackupOperation.SUCCESS).failure)
            assertNotNull(sharedIntent)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun sharingRejectsConcurrentSaveWithoutWritingItsDestination() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        beforeShare = {
            started.countDown()
            check(release.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        }
        val viewModel = viewModel(exportContext)
        try {
            viewModel.share()
            assertTrue(started.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
            viewModel.saveTo(Uri.fromFile(backupFile))
            assertEquals(BackupOperation.SHARING, viewModel.uiState.value.operation)
            assertFalse(backupFile.exists())
            release.countDown()
            viewModel.awaitOperation(BackupOperation.SUCCESS)
            assertFalse(backupFile.exists())
        } finally {
            release.countDown()
            viewModel.viewModelScope.cancel()
        }
    }

    private fun viewModel(fileContext: Context = context) = BackupViewModel(
        clock = Clock.fixed(exportedAt, ZoneOffset.UTC),
        dataStore = store,
        fileGateway = BackupFileGateway(fileContext, BackupShareFileStore(context)),
        preferencesRepository = preferences(),
        scheduler = object : BackgroundWorkScheduler {
            override fun ensureCalendarRefresh() = Unit
            override fun reconcileNotificationWork(enabled: Boolean) = Unit
            override fun enqueueImmediateNotificationEvaluation() = Unit
        },
        dateSource = TestCalendarDateSource(today),
        validationDispatcher = validation
    )

    private fun preferences() =
        DataStoreReleaseNotificationPreferences(context, database, database.portableSnapshotDao())

    private suspend fun BackupViewModel.awaitOperation(operation: BackupOperation): BackupUiState =
        withTimeout(TIMEOUT_MILLIS) { uiState.first { it.operation == operation } }

    private fun write(document: BackupDocument): Uri {
        backupFile.writeBytes(BackupJsonCodec.encode(document))
        return Uri.fromFile(backupFile)
    }

    private fun document(movieId: String): BackupDocument {
        val movie = ref(movieId)
        return BackupDocument(
            BACKUP_FORMAT_ID,
            BACKUP_SCHEMA_VERSION,
            exportedAt,
            BackupData(
                media = listOf(
                    BackupMedia(movie, listOf(movie), MediaType.MOVIE, "Movie $movieId", null, null, null, null)
                ),
                seasons = emptyList(),
                episodes = emptyList(),
                library = listOf(BackupLibraryEntry(movie, exportedAt, MediaType.MOVIE)),
                movieProgress = emptyList(),
                episodeProgress = emptyList(),
                ratings = emptyList(),
                preferences = BackupPreferences(3, true, false, true)
            )
        )
    }

    private fun ref(id: String) = BackupRef(MediaSource.TMDB, id)

    /** Holds dispatched blocks until [release] runs them on the calling test thread. */
    private class HeldDispatcher : CoroutineDispatcher() {
        private val held = LinkedBlockingQueue<Runnable>()
        private var next: Runnable? = null

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            held.put(block)
        }

        fun awaitHeld() {
            next = checkNotNull(held.poll(TIMEOUT_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                "Validation never reached its dispatcher"
            }
        }

        fun release() {
            checkNotNull(next).run()
            next = null
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
