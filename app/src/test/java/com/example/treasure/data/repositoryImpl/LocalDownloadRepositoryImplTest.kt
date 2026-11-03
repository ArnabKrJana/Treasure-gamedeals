package com.example.treasure.data.repositoryImpl

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.example.treasure.data.local.dao.DownloadedAssetDao
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
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

/**
 * Unit tests verifying [LocalDownloadRepositoryImpl] download scheduling and local asset tracking.
 */
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
        val url = "https://example.com/image.jpg"
        val assetId = "asset123"
        val fileName = "wallpaper.jpg"
        val downloadId = 12345L

        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>()
        every { Uri.parse(url) } returns mockUri

        val mockRequest = mockk<DownloadManager.Request>(relaxed = true)
        mockkConstructor(DownloadManager.Request::class)
        every { anyConstructed<DownloadManager.Request>().setTitle(any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setDescription(any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setNotificationVisibility(any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setDestinationInExternalPublicDir(any(), any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setAllowedOverMetered(any()) } returns mockRequest
        every { anyConstructed<DownloadManager.Request>().setAllowedOverRoaming(any()) } returns mockRequest

        every { downloadManager.enqueue(any()) } returns downloadId

        val result = repository.downloadWallpaper(assetId, url, fileName)

        assertThat(result).isEqualTo(downloadId)
        verify { downloadManager.enqueue(any()) }
        coVerify { downloadedAssetDao.insert(any()) }

        unmockkStatic(Uri::class)
        unmockkConstructor(DownloadManager.Request::class)
    }

    @Test
    fun `getDownloadedAssetIds returns set of ids from flow`() = runTest {
        val ids = listOf("id1", "id2", "id1")
        every { downloadedAssetDao.getAllDownloadedAssetIds() } returns flowOf(ids)

        repository.getDownloadedAssetIds().collect { result ->
            assertThat(result).hasSize(2)
            assertThat(result).containsExactly("id1", "id2")
        }
    }
}
