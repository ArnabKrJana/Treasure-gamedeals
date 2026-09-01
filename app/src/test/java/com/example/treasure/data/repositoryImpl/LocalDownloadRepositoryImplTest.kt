package com.example.treasure.data.repositoryImpl

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.example.treasure.data.local.dao.DownloadedAssetDao
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalDownloadRepositoryImplTest {

    private lateinit var repository: LocalDownloadRepositoryImpl
    private val context: Context = mockk()
    private val downloadedAssetDao: DownloadedAssetDao = mockk(relaxed = true)
    private val downloadManager: DownloadManager = mockk()
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { context.getSystemService(Context.DOWNLOAD_SERVICE) } returns downloadManager
        repository = LocalDownloadRepositoryImpl(context, downloadedAssetDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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

        // Mock DownloadManager.Request behavior
        val mockRequest = mockk<DownloadManager.Request>(relaxed = true)
        mockkConstructor(DownloadManager.Request::class)
        every { anyConstructed<DownloadManager.Request>().setTitle(any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setDescription(any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setNotificationVisibility(any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setDestinationInExternalPublicDir(any(), any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setAllowedOverMetered(any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setAllowedOverRoaming(any()) } returns mockRequest

        // Mock DownloadManager behavior
        every { downloadManager.enqueue(any()) } returns downloadId

        // Act
        val result = repository.downloadWallpaper(assetId, url, fileName)

        // Assert
        assertThat(result).isEqualTo(downloadId)
        verify { downloadManager.enqueue(any()) }
        coVerify { downloadedAssetDao.insert(any()) }
        
        unmockkStatic(Uri::class)
        unmockkConstructor(DownloadManager.Request::class)
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
