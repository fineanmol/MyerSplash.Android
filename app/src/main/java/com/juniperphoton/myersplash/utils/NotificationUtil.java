package com.juniperphoton.myersplash.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.SparseArray;

import com.juniperphoton.myersplash.R;
import com.juniperphoton.myersplash.activity.ManageDownloadActivity;
import com.juniperphoton.myersplash.base.App;
import com.juniperphoton.myersplash.service.BackgroundDownloadService;

import java.util.HashMap;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class NotificationUtil {
    private static int mLastId = 0;
    public static int NOT_ALLOCATED_ID = -10000;
    private static HashMap<Uri, Integer> uriHashMap = new HashMap<>();
    private static SparseArray<NotificationCompat.Builder> integerBuilderHashMap = new SparseArray<>();

    private static int findNIdByUri(Uri downloadUri) {
        int nId = NOT_ALLOCATED_ID;
        if (uriHashMap.containsKey(downloadUri)) {
            nId = uriHashMap.get(downloadUri);
        }
        return nId;
    }

    private static NotificationCompat.Builder findBuilderById(int id) {
        if (integerBuilderHashMap.get(id) != null) {
            return integerBuilderHashMap.get(id);
        }
        return null;
    }

    private static NotificationManager getNotificationManager() {
        return (NotificationManager) App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void cancelNotification(Uri downloadUri) {
        int nId = findNIdByUri(downloadUri);
        if (nId != NOT_ALLOCATED_ID) {
            getNotificationManager().cancel(nId);
        }
    }

    public static void cancelNotificationById(int nId) {
        if (nId != NOT_ALLOCATED_ID) {
            getNotificationManager().cancel(nId);
        }
    }

    public static void showErrorNotification(Uri downloadUri, String fileName, String url) {
        int nId;
        nId = findNIdByUri(downloadUri);
        if (nId == NOT_ALLOCATED_ID) {
            uriHashMap.put(downloadUri, mLastId);
            nId = mLastId;
            mLastId++;
        }

        Intent intent = new Intent(App.getInstance(), BackgroundDownloadService.class);
        intent.putExtra(Params.NAME_KEY, fileName);
        intent.putExtra(Params.URL_KEY, url);
        intent.putExtra(Params.CANCEL_NID_KEY, nId);

        PendingIntent resultPendingIntent = PendingIntent.getService(App.getInstance(), 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(App.getInstance())
                .setContentTitle(App.getInstance().getString(R.string.download_error))
                .setContentText(App.getInstance().getString(R.string.download_error_retry))
                .setSmallIcon(R.drawable.vector_ic_clear_white);

        builder.addAction(R.drawable.ic_replay_white_48dp, App.getInstance().getString(R.string.retry_act),
                resultPendingIntent);

        getNotificationManager().notify(nId, builder.build());
    }

    public static void showCompleteNotification(Uri downloadUri, Uri fileUri) {
        int nId;
        nId = findNIdByUri(downloadUri);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(App.getInstance())
                .setContentTitle(App.getInstance().getString(R.string.saved))
                .setContentText(App.getInstance().getString(R.string.tap_to_open_manage))
                .setSmallIcon(R.drawable.small_icon);

        //File file = new File(fileUri.getPath());
        //Uri uri = FileProvider.getUriForFile(App.getInstance(), App.getInstance().getString(R.string.authorities), file);
        //Intent intent =  WallpaperManager.getInstance(App.getInstance()).getCropAndSetWallpaperIntent(uri);
        injectIntent(builder);

        if (nId != NOT_ALLOCATED_ID) {
            getNotificationManager().notify(nId, builder.build());
        }
    }

    public static void showProgressNotification(String title, String content, int progress,
                                                String filePath, Uri downloadUri) {
        int nId;
        nId = findNIdByUri(downloadUri);
        if (nId == NOT_ALLOCATED_ID) {
            uriHashMap.put(downloadUri, mLastId);
            nId = mLastId;
            mLastId++;
        }

        NotificationCompat.Builder builder = findBuilderById(nId);
        if (builder == null) {
            builder = new NotificationCompat.Builder(App.getInstance())
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(R.drawable.vector_ic_file_download);
            integerBuilderHashMap.put(nId, builder);
        } else {
            builder.setProgress(100, progress, false);
        }
        injectIntent(builder);
        getNotificationManager().notify(nId, builder.build());
    }

    private static void injectIntent(NotificationCompat.Builder builder) {
        Intent intent = new Intent(App.getInstance(), ManageDownloadActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(App.getInstance());
        stackBuilder.addNextIntent(intent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
    }
}
