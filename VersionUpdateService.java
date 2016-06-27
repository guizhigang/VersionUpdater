package cn.gnsit.wenwan.app.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.alibaba.fastjson.JSON;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import cn.gnsit.wenwan.app.R;
import cn.gnsit.wenwan.app.activity.MainActivity;
import cn.gnsit.wenwan.app.api.ApiManager;
import cn.gnsit.wenwan.app.base.MainApplication;
import cn.gnsit.wenwan.app.event.VersionUpdateEvent;
import cn.gnsit.wenwan.app.model.ErrorCode;
import cn.gnsit.wenwan.app.model.VersionUpdateModel;
import cn.gnsit.wenwan.app.net.DownloadCallBack;
import cn.gnsit.wenwan.app.net.NetManager;
import cn.gnsit.wenwan.app.net.RequestCallBack;
import cn.gnsit.wenwan.app.utils.AppUtil;
import cn.gnsit.wenwan.app.utils.FolderUtil;
import cn.gnsit.wenwan.app.utils.LogUtil;
import cn.gnsit.wenwan.app.utils.StringUtil;
import cn.gnsit.wenwan.app.utils.ToastUtil;
import cn.gnsit.wenwan.app.utils.VersionUpdateUtil;
import cn.gnsit.wenwan.app.wxapi.WXPayUtil;
import okhttp3.Headers;

public class VersionUpdateService extends Service {
    private static final String TAG = VersionUpdateService.class.getSimpleName();
    private LocalBinder binder = new LocalBinder();

    private DownLoadListener downLoadListener;
    private boolean downLoading;
    private int progress;

    private NotificationManager mNotificationManager;
    private NotificationUpdaterThread notificationUpdaterThread;
    private Notification.Builder notificationBuilder;
    private final int NOTIFICATION_ID = 100;

    private VersionUpdateModel versionUpdateModel;

    public VersionUpdateService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "onCreate called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy called");
        setDownLoadListener(null);
        setCheckVersionCallBack(null);
        stopDownLoadForground();
        if (mNotificationManager != null)
            mNotificationManager.cancelAll();
        downLoading = false;
    }

    public interface DownLoadListener {
        void begain();

        void inProgress(float progress, long total);

        void downLoadLatestSuccess(File file);

        void downLoadLatestFailed();
    }

    public interface CheckVersionCallBack {
        void onSuccess();

        void onError();
    }

    private CheckVersionCallBack checkVersionCallBack;

    public void setCheckVersionCallBack(CheckVersionCallBack checkVersionCallBack) {
        this.checkVersionCallBack = checkVersionCallBack;
    }

    private class NotificationUpdaterThread extends Thread {
        @Override
        public void run() {
            while (true) {
                notificationBuilder.setContentTitle("正在下载更新" + progress + "%"); // the label of the entry
                notificationBuilder.setProgress(100, progress, false);
                mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.getNotification());
                if (progress >= 100) {
                    break;
                }
            }
        }
    }

    public boolean isDownLoading() {
        return downLoading;
    }

    public void setDownLoading(boolean downLoading) {
        this.downLoading = downLoading;
    }

    /**
     * 让Service保持活跃,避免出现:
     * 如果启动此服务的前台Activity意外终止时Service出现的异常(也将意外终止)
     */
    private void starDownLoadForground() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "下载中,请稍后...";
        // The PendingIntent to launch our activity if the user selects this notification
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MainActivity.class), 0);
        notificationBuilder = new Notification.Builder(this);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);  // the status icon
        notificationBuilder.setTicker(text);  // the status text
        notificationBuilder.setWhen(System.currentTimeMillis());  // the time stamp
        notificationBuilder.setContentText(text);  // the contents of the entry
//        notificationBuilder.setContentIntent(contentIntent);  // The intent to send when the entry is clicked
        notificationBuilder.setContentTitle("正在下载更新" + 0 + "%"); // the label of the entry
        notificationBuilder.setProgress(100, 0, false);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setAutoCancel(true);
        Notification notification = notificationBuilder.getNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void stopDownLoadForground() {
        stopForeground(true);
    }

    public void doCheckUpdateTask() {
        final int currentBuild = AppUtil.getVersionCode(this);
        String client = "android";
        String q = "needUpgrade";
        ApiManager.getInstance().versionApi.upgradeRecords(q, currentBuild, client, new RequestCallBack() {
            @Override
            public void onSuccess(Headers headers, String response) {
                try {
                    versionUpdateModel = JSON.parseObject(response, VersionUpdateModel.class);
                    if (versionUpdateModel.getBuild() < currentBuild) {
                        versionUpdateModel.setNeedUpgrade(false);
                    }
                    //TEST DATA
                    versionUpdateModel.setNeedUpgrade(true);

                    MainApplication.getInstance().setVersionUpdateModelCache(versionUpdateModel);
                    if (checkVersionCallBack != null)
                        checkVersionCallBack.onSuccess();
                } catch (Exception e) {
                    ToastUtil.toast(VersionUpdateService.this, "获取版本信息失败");
                }
            }

            @Override
            public void onError(int code, String response) {
                if (checkVersionCallBack != null) {
                    checkVersionCallBack.onError();
                }
            }
        });
    }

    public void doDownLoadTask() {
        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        starDownLoadForground();

        notificationUpdaterThread = new NotificationUpdaterThread();
        notificationUpdaterThread.start();

        final File fileDir = FolderUtil.getDownloadCacheFolder();
        final String url = versionUpdateModel.getUpgradeUrl();
        final String fileName_ = url.substring(url.lastIndexOf("/") + 1);
        final String fileName = StringUtil.string2MD5(fileName_) + ".apk";

        downLoading = true;

        if (downLoadListener != null) {
            downLoadListener.begain();
        }

        NetManager.getInstance().download(url, fileDir.getAbsolutePath(), fileName, new DownloadCallBack() {
            @Override
            public void inProgress(float progress_, long total) {
                progress = (int) (progress_ * 100);
                if (downLoadListener != null) {
                    downLoadListener.inProgress(progress_, total);
                }
                if (progress >= 100) {
                    mNotificationManager.cancelAll();
                }
            }

            @Override
            public void onSuccess(Headers headers, String response) {
                final File destFile = new File(fileDir.getAbsolutePath(), fileName);
                if (downLoadListener != null) {
                    downLoadListener.downLoadLatestSuccess(destFile);
                }
                downLoading = false;
                installApk(destFile, VersionUpdateService.this);
            }

            @Override
            public void onError(int code, String response) {
                downLoading = false;
                if (mNotificationManager != null)
                    mNotificationManager.cancelAll();
                if (downLoadListener != null) {
                    downLoadListener.downLoadLatestFailed();
                }
            }
        });
    }

    public VersionUpdateModel getVersionUpdateModel() {
        return versionUpdateModel;
    }

    public void setDownLoadListener(DownLoadListener downLoadListener) {
        this.downLoadListener = downLoadListener;
    }

    //安装apk
    public void installApk(File file, Context context) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //执行的数据类型
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public VersionUpdateService getService() {
            return VersionUpdateService.this;
        }
    }
}
