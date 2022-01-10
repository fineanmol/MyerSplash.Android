package com.juniperphoton.myersplash.broadcastreceiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.juniperphoton.myersplash.App
import com.juniperphoton.myersplash.MainActivity
import com.juniperphoton.myersplash.R
import com.juniperphoton.myersplash.api.CloudService
import com.juniperphoton.myersplash.api.IOService
import com.juniperphoton.myersplash.di.AppComponent
import com.juniperphoton.myersplash.extension.getLengthInKB
import com.juniperphoton.myersplash.extension.writeToFile
import com.juniperphoton.myersplash.model.UnsplashImage
import com.juniperphoton.myersplash.model.UnsplashImageFactory
import com.juniperphoton.myersplash.repo.RandomImageRepo
import com.juniperphoton.myersplash.service.DownloadService
import com.juniperphoton.myersplash.utils.AppWidgetUtils
import com.juniperphoton.myersplash.utils.FileUtils
import com.juniperphoton.myersplash.utils.Params
import com.juniperphoton.myersplash.utils.Pasteur
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File

class WallpaperWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val TAG = "WallpaperWidgetProvider"
    }

    private val service: IOService = AppComponent.instance.ioService

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) = runBlocking {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        if (appWidgetIds == null || context == null) {
            return@runBlocking
        }
        Pasteur.debug(TAG) {
            "on update"
        }

        val image = RandomImageRepo(CloudService.createService()).loadData(1).random()
        val downloadUrl = image.listUrl ?: kotlin.run {
            Pasteur.debug(TAG) {
                "onUpdate, downloadUrl is null"
            }
            return@runBlocking
        }

        Pasteur.debug(TAG) {
            "onUpdate, about to load display image $downloadUrl"
        }

        val file = File(FileUtils.cachedPath, "${downloadUrl.hashCode()}.jpg")
        if (file.exists() && file.getLengthInKB() > 100) {
            Pasteur.debug(TAG) {
                "on update, file exists"
            }

            AppWidgetUtils.doWithWidgetId {
                updateWidget(App.instance, it, file.absolutePath, image)
            }
            return@runBlocking
        }

        try {
            Pasteur.debug(TAG) {
                "on update, about to download $downloadUrl"
            }

            val responseBody = withTimeout(CloudService.DOWNLOAD_TIMEOUT_MS) {
                service.downloadFile(downloadUrl)
            }
            val outputFile = responseBody.writeToFile(file.path, null)
            outputFile.let {
                AppWidgetUtils.doWithWidgetId { id ->
                    updateWidget(App.instance, id, it.absolutePath, image)
                }
            }
        } catch (e: Exception) {
            Pasteur.warn(TAG) {
                "error on download image $e for $downloadUrl"
            }

            e.printStackTrace()
        }
    }

    private fun updateWidget(
        context: Context,
        widgetId: Int,
        filePath: String,
        image: UnsplashImage
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)
        remoteViews.setTextViewText(
            R.id.widget_update_time_text,
            UnsplashImageFactory.TODAY_STRING_FOR_DISPLAY
        )
        val bm = BitmapFactory.decodeFile(filePath)
        remoteViews.setImageViewBitmap(R.id.widget_center_image, bm)

        Pasteur.debug(TAG, "pending to download: $UnsplashImageFactory.DOWNLOAD_URL")

        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra(Params.URL_KEY, image.downloadUrl)
            putExtra(Params.NAME_KEY, image.fileNameForDownload)
            putExtra(Params.PREVIEW_URI, filePath)
            putExtra(Params.IS_UNSPLASH_WALLPAPER, false)
        }

        val pendingIntent =
            PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteViews.setOnClickPendingIntent(R.id.widget_download_btn, pendingIntent)

        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent =
            PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteViews.setOnClickPendingIntent(R.id.widget_center_image, mainPendingIntent)

        manager.updateAppWidget(widgetId, remoteViews)
    }
}