package com.juniperphoton.myersplash.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.juniperphoton.myersplash.MainActivity
import com.juniperphoton.myersplash.R
import com.juniperphoton.myersplash.activity.DownloadsListActivity

/**
 * @author dengweichao @ Zhihu Inc.
 * @since 2021-02-17
 */
object ShortcutsManager {
    private const val DOWNLOADS_SHORTCUT_ID = "downloads_shortcut"
    private const val SEARCH_SHORTCUT_ID = "search_shortcut"

    const val ACTION_SEARCH = "action.search"
    private const val ACTION_DOWNLOADS = "action.download"

    fun Context.initShortcuts() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) {
            return
        }

        val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return

        val shortcut = ShortcutInfo.Builder(this, DOWNLOADS_SHORTCUT_ID)
            .setShortLabel(getString(R.string.downloadLowercase))
            .setLongLabel(getString(R.string.downloadLowercase))
            .setIcon(Icon.createWithResource(this, R.drawable.ic_download_shortcut))
            .setIntent(Intent(this, DownloadsListActivity::class.java).apply {
                action = ACTION_DOWNLOADS
            })
            .build()

        val search = ShortcutInfo.Builder(this, SEARCH_SHORTCUT_ID)
            .setShortLabel(getString(R.string.searchLowercase))
            .setLongLabel(getString(R.string.searchLowercase))
            .setIcon(Icon.createWithResource(this, R.drawable.ic_search_shortcut))
            .setIntent(Intent(this, MainActivity::class.java).apply {
                action = ACTION_SEARCH
            })
            .build()

        shortcutManager.dynamicShortcuts = listOf(shortcut, search)
    }
}