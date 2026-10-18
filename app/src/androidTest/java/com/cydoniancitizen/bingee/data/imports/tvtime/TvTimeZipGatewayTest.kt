package com.cydoniancitizen.bingee.data.imports.tvtime

import android.content.Context
import android.os.Build
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvTimeZipGatewayTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sourceDirectory = File(context.cacheDir, "backup_exports").apply { mkdirs() }

    @After
    fun cleanFiles() {
        sourceDirectory.listFiles()?.forEach(File::delete)
        context.cacheDir.listFiles()
            ?.filter { it.name.startsWith("tv-time-import-") }
            ?.forEach(File::delete)
    }

    @Test
    fun cleanupRemovesOnlyAbandonedZipsAndKeepsActiveCopiesAndOtherFiles() = kotlinx.coroutines.runBlocking {
        val source = zip("movies.json" to "[]")
        val gateway = gateway()
        val unrelated = File(context.cacheDir, "tv-time-import-not-a-zip.txt").apply { writeText("keep") }
        val directory = File(context.cacheDir, "tv-time-import-directory.zip").apply { mkdirs() }
        val child = File(directory, "keep.txt").apply { writeText("keep") }
        try {
            val result = gateway.withArchive(uri(source)) { archive ->
                val active = context.cacheDir.listFiles().orEmpty().single {
                    it.isFile && it.name.endsWith(".zip") && it.name.startsWith("tv-time-import-")
                }
                val abandoned = File(context.cacheDir, "tv-time-import-abandoned.zip").apply { writeText("old copy") }

                gateway.cleanupAbandonedCopies()

                assertFalse(abandoned.exists())
                assertTrue(active.exists())
                assertEquals("keep", unrelated.readText())
                assertEquals("keep", child.readText())
                archive.open(archive.entries.single()).bufferedReader().use { assertEquals("[]", it.readText()) }
            }
            assertTrue(result is TvTimeArchiveResult.Success)
            assertTrue(source.exists())
        } finally {
            unrelated.delete()
            child.delete()
            directory.delete()
        }
    }

    @Test
    fun cancellationReleasesThePrivateCopyAndPreservesTheSource() = kotlinx.coroutines.runBlocking {
        val source = zip("movies.json" to "[]")
        try {
            gateway().withArchive(uri(source)) { throw CancellationException("cancel import") }
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) {
            assertTrue(source.exists())
            assertFalse(context.cacheDir.listFiles().orEmpty().any { it.name.startsWith("tv-time-import-") })
        }
    }

    @Test
    fun overlappingImportsKeepEachOthersPrivateCopiesUntilClosed() = kotlinx.coroutines.runBlocking {
        val source = zip("movies.json" to "[]")
        val gateway = gateway()
        val result = gateway.withArchive(uri(source)) { outer ->
            val outerCopy = context.cacheDir.listFiles().orEmpty().single { it.name.startsWith("tv-time-import-") }
            val innerResult = gateway.withArchive(uri(source)) {
                assertTrue("A second import must not remove the first import's copy", outerCopy.exists())
                assertEquals(2, context.cacheDir.listFiles().orEmpty().count { it.name.startsWith("tv-time-import-") })
            }
            assertTrue(innerResult is TvTimeArchiveResult.Success)
            assertTrue(outerCopy.exists())
            outer.open(outer.entries.single()).bufferedReader().use { assertEquals("[]", it.readText()) }
        }

        assertTrue(result is TvTimeArchiveResult.Success)
        assertTrue(source.exists())
        assertFalse(context.cacheDir.listFiles().orEmpty().any { it.name.startsWith("tv-time-import-") })
    }

    @Test
    fun validArchiveExposesOnlySafeEntryMetadataAndCleansTemporaryCopy() = kotlinx.coroutines.runBlocking {
        File(context.cacheDir, "tv-time-import-stale.zip").writeText("stale")
        val source = zip(
            "list.json" to "[]",
            "movies.json" to "[]",
            "series.json" to "[]",
            "report.html" to "<html></html>"
        )

        val result = gateway().withArchive(uri(source)) { archive ->
            assertEquals(4, archive.entries.size)
            assertEquals(3, archive.entries.count { it.kind == TvTimeArchiveEntryKind.JSON })
            assertEquals(1, archive.entries.count { it.kind == TvTimeArchiveEntryKind.HTML })
        }

        assertTrue(result is TvTimeArchiveResult.Success)
        assertFalse(context.cacheDir.listFiles().orEmpty().any { it.name.startsWith("tv-time-import-") })
    }

    @Test
    fun rejectsTraversalAbsoluteDriveUncAndDotSegments() = kotlinx.coroutines.runBlocking {
        // Android 14+ rejects these names in ZipFile's constructor, before our name validator.
        // The gateway maps that platform ZipException to MALFORMED_ARCHIVE; rejection is preserved.
        val platformRejectsPaths = Build.VERSION.SDK_INT >= 34
        val unsafeNames = mapOf(
            "../movies.json" to if (platformRejectsPaths) {
                TvTimeArchiveFailureKind.MALFORMED_ARCHIVE
            } else {
                TvTimeArchiveFailureKind.PATH_TRAVERSAL
            },
            "./movies.json" to TvTimeArchiveFailureKind.PATH_TRAVERSAL,
            "/movies.json" to if (platformRejectsPaths) {
                TvTimeArchiveFailureKind.MALFORMED_ARCHIVE
            } else {
                TvTimeArchiveFailureKind.ABSOLUTE_PATH
            },
            "C:/movies.json" to TvTimeArchiveFailureKind.DRIVE_PATH,
            "\\\\server\\share\\movies.json" to TvTimeArchiveFailureKind.UNC_PATH
        )
        unsafeNames.forEach { (name, expected) ->
            assertFailure(zip(name to "[]"), expected)
        }
    }

    @Test
    fun rejectsDuplicateNormalizedAndCaseCollidingPaths() = kotlinx.coroutines.runBlocking {
        assertFailure(
            zip("folder/data.json" to "[]", "folder/ data.json" to "[]"),
            TvTimeArchiveFailureKind.DUPLICATE_PATH
        )
        assertFailure(
            zip("Movies.json" to "[]", "movies.json" to "[]"),
            TvTimeArchiveFailureKind.CASE_COLLISION
        )
    }

    @Test
    fun rejectsNestedArchiveByExtensionOrMagic() = kotlinx.coroutines.runBlocking {
        assertFailure(zip("nested.zip" to "not relevant"), TvTimeArchiveFailureKind.NESTED_ARCHIVE)
        assertFailure(
            zipBytes("disguised.json" to byteArrayOf(0x50, 0x4b, 0x03, 0x04, 0x00)),
            TvTimeArchiveFailureKind.NESTED_ARCHIVE
        )
    }

    @Test
    fun rejectsSuspiciousCompressionAndMalformedArchive() = kotlinx.coroutines.runBlocking {
        assertFailure(
            zipBytes("movies.json" to ByteArray(256 * 1024)),
            TvTimeArchiveFailureKind.SUSPICIOUS_COMPRESSION
        )
        val malformed = File(sourceDirectory, "malformed.zip").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        assertFailure(malformed, TvTimeArchiveFailureKind.MALFORMED_ARCHIVE)
    }

    @Test
    fun rejectsDeclaredOversizedEntryAndTotalBeforeInflation() = kotlinx.coroutines.runBlocking {
        val oversizedEntry = zip("one.json" to "[]")
        patchCentralUncompressedSizes(oversizedEntry, listOf(TvTimeImportLimits.MAX_ENTRY_UNCOMPRESSED_BYTES + 1))
        assertFailure(oversizedEntry, TvTimeArchiveFailureKind.OVERSIZED_ENTRY)

        val oversizedTotal = zip("one.json" to "[]", "two.json" to "[]", "three.json" to "[]")
        patchCentralUncompressedSizes(
            oversizedTotal,
            listOf(400L * 1024 * 1024, 400L * 1024 * 1024, 400L * 1024 * 1024)
        )
        assertFailure(oversizedTotal, TvTimeArchiveFailureKind.OVERSIZED_TOTAL)
    }

    private suspend fun assertFailure(file: File, expected: TvTimeArchiveFailureKind) {
        val result = gateway().withArchive(uri(file)) { Unit }
        assertTrue(result is TvTimeArchiveResult.Failure)
        assertEquals(file.name, expected, (result as TvTimeArchiveResult.Failure).failure.kind)
        assertTrue(file.exists())
        assertFalse(context.cacheDir.listFiles().orEmpty().any { it.name.startsWith("tv-time-import-") })
    }

    private fun gateway() = AndroidTvTimeZipGateway(context)

    private fun uri(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.backup-files",
        file
    )

    private fun zip(vararg entries: Pair<String, String>): File =
        zipBytes(*entries.map { it.first to it.second.toByteArray(Charsets.UTF_8) }.toTypedArray())

    private fun zipBytes(vararg entries: Pair<String, ByteArray>): File =
        File.createTempFile("synthetic-tv-time-", ".zip", sourceDirectory).also { file ->
            ZipOutputStream(file.outputStream()).use { output ->
                entries.forEach { (name, value) ->
                    output.putNextEntry(ZipEntry(name))
                    output.write(value)
                    output.closeEntry()
                }
            }
        }

    private fun patchCentralUncompressedSizes(file: File, sizes: List<Long>) {
        val bytes = file.readBytes()
        var searchFrom = 0
        sizes.forEach { size ->
            val index = findSignature(bytes, searchFrom, byteArrayOf(0x50, 0x4b, 0x01, 0x02))
            require(index >= 0)
            writeUnsignedIntLittleEndian(bytes, index + 24, size)
            searchFrom = index + 4
        }
        file.writeBytes(bytes)
    }

    private fun findSignature(bytes: ByteArray, start: Int, signature: ByteArray): Int {
        for (index in start..bytes.size - signature.size) {
            if (signature.indices.all { bytes[index + it] == signature[it] }) return index
        }
        return -1
    }

    private fun writeUnsignedIntLittleEndian(bytes: ByteArray, offset: Int, value: Long) {
        repeat(4) { index -> bytes[offset + index] = (value shr (index * 8)).toByte() }
    }
}
