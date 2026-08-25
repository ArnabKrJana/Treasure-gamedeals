package com.example.treasure.data.repositoryImpl

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.example.treasure.data.local.dao.DownloadedAssetDao
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LocalDownloadRepositoryImplTest {

    private lateinit var repository: LocalDownloadRepositoryImpl
    private val context: Context = mockk()
    private val downloadedAssetDao: DownloadedAssetDao = mockk(relaxed = true)
    private val downloadManager: DownloadManager = mockk()

    @Before
    fun setup() {
        every { context.getSystemService(Context.DOWNLOAD_SERVICE) } returns downloadManager
        repository = LocalDownloadRepositoryImpl(context, downloadedAssetDao)
    }

    @Test
    fun `downloadWallpaper enqueues request and saves to DAO`() = runTest {
        // Prepare
        val url = "https://example.com/image.jpg"
        val assetId = "asset123"
        val fileName = "wallpaper.jpg"
        val downloadId = 12345L
        
        // Mocking Uri.parse because it's a static Android method
        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>()
        every { Uri.parse(url) } returns mockUri

        // Mock DownloadManager behavior
        every { downloadManager.enqueue(any()) } returns downloadId

        // Act
        val result = repository.downloadWallpaper(assetId, url, fileName)

        // Assert
        assertThat(result).isEqualTo(downloadId)
        verify { downloadManager.enqueue(any()) }
        coVerify { downloadedAssetDao.insert(any()) }
        
        unmockkStatic(Uri::class)
    }

    @Test
    fun `getDownloadedAssetIds returns set of ids from flow`() = runTest {
        // Prepare
        val ids = listOf("id1", "id2", "id1") // Note duplicate to check Set conversion
        every { downloadedAssetDao.getAllDownloadedAssetIds() } returns flowOf(ids)

        // Act & Assert
        repository.getDownloadedAssetIds().collect { result ->
            assertThat(result).hasSize(2)
            assertThat(result).containsExactly("id1", "id2")
        }
    }
}
