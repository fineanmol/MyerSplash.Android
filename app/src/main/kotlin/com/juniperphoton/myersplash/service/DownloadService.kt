package com.juniperphoton.myersplash.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.juniperphoton.myersplash.App
import com.juniperphoton.myersplash.R
import com.juniperphoton.myersplash.api.CloudService.DOWNLOAD_TIMEOUT_MS
import com.juniperphoton.myersplash.api.IOService
import com.juniperphoton.myersplash.db.DownloadItemDao
import com.juniperphoton.myersplash.di.AppComponent
import com.juniperphoton.myersplash.extension.notifyFileUpdated
import com.juniperphoton.myersplash.extension.writeToFile
import com.juniperphoton.myersplash.utils.*
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

class DownloadService : Service(), CoroutineScope by MainScope() {
    companion object {
        private const val TAG = "DownloadService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // A map storing download url to downloading disposable object
    private val downloadUrlToJobMap = mutableMapOf<String, Job>()

    @Inject
    internal lateinit var dao: DownloadItemDao

    @Inject
    lateinit var service: IOService

    override fun onCreate() {
        super.onCreate()
        AppComponent.instance.inject(this)
    }

    @SuppressLint("CheckResult")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Pasteur.info(TAG, "on start command $intent")
        intent?.let {
            // launch in main thread
            launch {
                onHandleIntent(it)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Pasteur.w(TAG, "on destroy, cancel all, size: ${downloadUrlToJobMap.size}")
        cancel()
        super.onDestroy()
    }

    private suspend fun checkStatus() {
        Pasteur.info(TAG, "checking status, thread: ${Thread.currentThread()}")
        dao.markAllFailed()
    }

    private suspend fun onHandleIntent(intent: Intent) {
        if (intent.getBooleanExtra(Params.CHECK_STATUS, false)) {
            withContext(Dispatchers.IO) {
                checkStatus()
            }
            if (downloadUrlToJobMap.isEmpty()) {
                Pasteur.w(TAG, "about to stop self")
                stopSelf()
            }
            return
        }

        Pasteur.info(TAG, "onHandleIntent")

        val downloadUrl = intent.getStringExtra(Params.URL_KEY) ?: return
        val fileName = intent.getStringExtra(Params.NAME_KEY) ?: return

        val cancelJob = intent.getBooleanExtra(Params.CANCELED_KEY, false)
        val previewUrl = intent.getStringExtra(Params.PREVIEW_URI)
        val isUnsplash = intent.getBooleanExtra(Params.IS_UNSPLASH_WALLPAPER, true)
        val fromRetry = intent.getIntExtra(Params.RETRY_KEY, 0)

        if (fromRetry != 0) {
            NotificationUtils.cancelNotification(fromRetry)
        }

        if (!isUnsplash) {
            Toaster.sendShortToast(R.string.downloading)
        }

        val previewUri: Uri? = if (previewUrl.isNullOrEmpty()) null else {
            Uri.parse(previewUrl)
        }

        if (cancelJob) {
            cancelJob(downloadUrl)
        } else {
            Pasteur.d(TAG, "on handle intent progress: $downloadUrl")
            downloadImage(downloadUrl, fileName, previewUri, isUnsplash)
        }
    }

    private suspend fun cancelJob(url: String) {
        Pasteur.d(TAG, "on handle intent cancelled, thread: ${Thread.currentThread()}")
        downloadUrlToJobMap[url]?.let { job ->
            job.cancelAndJoin()
            downloadUrlToJobMap.remove(url)
            Pasteur.info(TAG, "job cancelled")
            NotificationUtils.cancelNotification(Uri.parse(url))
            Toaster.sendShortToast(getString(R.string.cancelled_download))
        }
    }

    private fun downloadImage(url: String, fileName: String,
                              previewUri: Uri?, isUnsplash: Boolean) {
        val job = launch(context = CoroutineExceptionHandler { _, e ->
            Pasteur.e(TAG, "CoroutineExceptionHandler error $e, url $url")
        }) {
            Pasteur.info(TAG, "on start downloading")
            try {
                withContext(Dispatchers.IO) {
                    dao.setProgress(url, 0)
                }

                val file = DownloadUtils.getFileToSave(fileName)

                val responseBody = withTimeout(DOWNLOAD_TIMEOUT_MS) {
                    service.downloadFile(url)
                }

                Pasteur.d(TAG, "outputFile download onNext, size=${responseBody.contentLength()}")

                withContext(Dispatchers.IO) {
                    dao.setProgress(url, 1)
                }

                responseBody.writeToFile(file!!.path) { p ->
                    Pasteur.i(TAG, "dao setting progress: $p")
                    dao.setProgress(url, p)
                }
                onSuccess(url, file, previewUri, isUnsplash)
                Pasteur.d(TAG, getString(R.string.completed))
            } catch (e: CancellationException) {
                // CancellationException will be ignored by CoroutineExceptionHandler,
                // thus we handle it here.
                Pasteur.e(TAG, "CancellationException error $e, url $url")
                onError(url, fileName, null, false)
            } catch (e: Exception) {
                Pasteur.e(TAG, "other error $e, url $url")
                onError(url, fileName, null, true)
            }
        }
        downloadUrlToJobMap[url] = job
    }

    private suspend fun onSuccess(url: String, file: File, previewUri: Uri?, isUnsplash: Boolean) {
        Pasteur.d(TAG, "output file:" + file.absolutePath)

        val newFile = File("${file.path.replace(" ", "")}.jpg")
        file.renameTo(newFile)

        Pasteur.d(TAG, "renamed file:" + newFile.absolutePath)
        newFile.notifyFileUpdated(App.instance)

        withContext(Dispatchers.IO) {
            dao.setSuccess(url, newFile.path)
        }

        NotificationUtils.showCompleteNotification(Uri.parse(url), previewUri,
                if (isUnsplash) null else newFile.absolutePath)
    }

    private suspend fun onError(url: String, fileName: String, previewUri: Uri?, showNotification: Boolean) {
        if (showNotification) {
            NotificationUtils.showErrorNotification(Uri.parse(url), fileName, url, previewUri)
        }

        withContext(Dispatchers.IO) {
            dao.setFailed(url)
        }
    }
}
