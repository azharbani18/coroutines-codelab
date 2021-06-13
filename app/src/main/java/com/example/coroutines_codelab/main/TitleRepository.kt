package com.example.coroutines_codelab

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.common.util.concurrent.Futures.withTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TitleRepository(val network: MainNetwork, val titleDao: TitleDao) {

    /**
     * [LiveData] to load title.
     *
     * This is the main interface for loading a title. The title will be loaded from the offline
     * cache.
     *
     * Observing this will not cause the title to be refreshed, use [TitleRepository.refreshTitle]
     * to refresh the title.
     */
    val title: LiveData<String?> = titleDao.titleLiveData.map { it?.title }


    /**
     * Refresh the current title and save the results to the offline cache.
     *
     * This method does not return the new title. Use [TitleRepository.title] to observe
     * the current tile.
     */
    suspend fun refreshTitle() {
        try {
            val result = withTimeout(5_000) {
                network.fetchNextTitle()
            }
            titleDao.insertTitle(Title(result))
        } catch (error: Throwable) {
            throw TitleRefreshError("Unable to refresh title", error)
        }
    }

    /**
     * This API is exposed for callers from the Java Programming language.
     *
     * The request will run unstructured, which means it won't be able to be cancelled.
     *
     * @param titleRefreshCallback a callback
     */
    fun refreshTitleInterop(titleRefreshCallback: TitleRefreshCallback) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                refreshTitle()
                titleRefreshCallback.onCompleted()
            } catch (throwable: Throwable) {
                titleRefreshCallback.onError(throwable)
            }
        }
    }
}

/**
 * Thrown when there was a error fetching a new title
 *
 * @property message user ready error message
 * @property cause the original cause of this exception
 */
class TitleRefreshError(message: String, cause: Throwable) : Throwable(message, cause)

interface TitleRefreshCallback {
    fun onCompleted()
    fun onError(cause: Throwable)
}
