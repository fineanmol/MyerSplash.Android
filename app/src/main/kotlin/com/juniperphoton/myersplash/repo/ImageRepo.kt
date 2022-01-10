package com.juniperphoton.myersplash.repo

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.juniperphoton.myersplash.App
import com.juniperphoton.myersplash.R
import com.juniperphoton.myersplash.api.CloudService
import com.juniperphoton.myersplash.api.PhotoService
import com.juniperphoton.myersplash.model.UnsplashImage
import com.juniperphoton.myersplash.model.UnsplashImageFactory
import com.juniperphoton.myersplash.utils.LocalSettingHelper
import com.juniperphoton.myersplash.utils.Pasteur
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

abstract class ImageRepo {
    companion object {
        private const val TAG = "ImageRepo"

        const val DEFAULT_PAGE = 1
    }

    private var page = DEFAULT_PAGE

    protected val data = mutableListOf<UnsplashImage>()

    private val _images = MutableLiveData<List<UnsplashImage>>()
    val images: LiveData<List<UnsplashImage>>
        get() = _images

    fun onRestore(images: List<UnsplashImage>) {
        _images.value = images
    }

    suspend fun refresh() {
        Pasteur.info(TAG) {
            "start refresh: $this"
        }

        try {
            val newList = loadData(DEFAULT_PAGE)

            data.clear()
            data.addAll(newList)
            postValue()

            page++
        } catch (e: Exception) {
            Pasteur.warn(TAG) {
                "error on refresh"
            }
            throw e
        }
    }

    suspend fun loadMore() {
        Pasteur.info(TAG) {
            "start load more: $this"
        }

        try {
            val newList = loadData(page)

            data.addAll(newList)
            postValue()

            page++
        } catch (e: Exception) {
            Pasteur.warn(TAG) {
                "error on load more"
            }
            throw e
        }
    }

    abstract suspend fun loadData(page: Int): List<UnsplashImage>

    @UiThread
    private fun postValue() {
        _images.value = data
    }
}

class NewImageRepo @Inject constructor(
    private val service: PhotoService
) : ImageRepo() {
    override suspend fun loadData(page: Int): List<UnsplashImage> {
        return service.getNewPhotos(page).apply {
            val showSponsorship = LocalSettingHelper.getBoolean(
                App.instance,
                App.instance.getString(R.string.preference_key_show_sponsorship), true
            )
            if (!showSponsorship) {
                removeAll { it.sponsorship != null }
            }
        }
    }
}

class RandomImageRepo @Inject constructor(
    private val service: PhotoService
) : ImageRepo() {
    override suspend fun loadData(page: Int): List<UnsplashImage> {
        return service.getRandomPhotos(page)
    }
}

class DeveloperImageRepo @Inject constructor(
    private val service: PhotoService
) : ImageRepo() {
    override suspend fun loadData(page: Int): List<UnsplashImage> {
        return service.getDeveloperPhotos(page)
    }
}

class HighlightImageRepo : ImageRepo() {
    companion object {
        private const val TAG = "HighlightImageRepo"

        private val startDate = SimpleDateFormat("yyyy/MM/dd").parse("2021/12/31")
        private val endDate = SimpleDateFormat("yyyy/MM/dd").parse("2017/03/20")
    }

    override suspend fun loadData(page: Int): List<UnsplashImage> {
        return getHighlightsPhotos(page)
    }

    private suspend fun getHighlightsPhotos(page: Int): MutableList<UnsplashImage> {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.time = startDate

        val list = mutableListOf<UnsplashImage>()

        for (i in 0 until CloudService.DEFAULT_HIGHLIGHTS_COUNT) {
            val date = calendar.time
            if (date > endDate) {
                list.add(UnsplashImageFactory.createHighlightImage(calendar.time))
            } else {
                Pasteur.debug(TAG) {
                    "the date: $date is before end date $endDate"
                }
            }
            calendar.add(Calendar.DATE, -1)
        }

        delay(CloudService.HIGHLIGHTS_DELAY_MS)

        return list
    }
}

class SearchImageRepo @Inject constructor(
    private val service: PhotoService
) : ImageRepo() {
    var keyword: String? = null

    override suspend fun loadData(page: Int): List<UnsplashImage> {
        val word = keyword ?: return emptyList()
        return service.searchPhotosByQuery(word, page).list ?: emptyList()
    }
}