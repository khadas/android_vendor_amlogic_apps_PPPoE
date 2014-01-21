/**
 * @Package com.amlogic.otauicase
 * @Description Copyright (c) Inspur Group Co., Ltd. Unpublished Inspur Group
 *              Co., Ltd. Proprietary & Confidential This source code and the
 *              algorithms implemented therein constitute confidential
 *              information and may comprise trade secrets of Inspur or its
 *              associates, and any use thereof is subject to the terms and
 *              conditions of the Non-Disclosure Agreement pursuant to which
 *              this source code was originally received.
 */
package com.amlapp.update.otaupgrade;

import java.util.Timer;
import java.util.TimerTask;

import com.amlogic.update.CheckUpdateTask;
import com.amlogic.update.DownloadUpdateTask;
import com.amlogic.update.Notifier;
import com.amlogic.update.UpdateTasks;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * @ClassName UpdateService
 * @Description TODO
 * @Date 2013-7-15
 * @Email
 * @Author
 * @Version V1.0
 */
public class UpdateService extends Service {
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Service#onBind(android.content.Intent)
     */
    public static final String TAG               = "otaupgrade";
    public static final String KEY_START_COMMAND = "start_command";
    public static final String ACTION_CHECK = "com.android.update.check";
    public static final String ACTION_DOWNLOAD = "com.android.update.download";
    public static final String ACTION_AUTOCHECK = "com.android.update.action.autocheck";
    public static final String ACTION_DOWNLOAD_OK = "com.android.update.DOWNLOAD_OK";

    public static final String ACTION_DOWNLOAD_SUCCESS = "com.android.update.downloadsuccess";
    public static final String ACTION_DOWNLOAD_FAILED = "com.android.update.downloadfailed";
    public static final int CHECK_UPGRADE_OK = 103;
    public static final int    TASK_ID_CHECKING  = 101;
    public static final int    TASK_ID_DOWNLOAD  = 102;
    private Timer timer = null;
    private UpdateTasks        mCheckingTask, mDownloadTask;
    private Notifier notice = new DownloadNotify();
    @Override
    public IBinder onBind(Intent arg0) {
        return new uiBinder();
    }
    public int onStartCommand(Intent intent, int paramInt1, int paramInt2) {
        if(intent == null)
            return 0;
        String act = intent.getAction();
        if (PrefUtils.DEBUG) {
            Log.i(TAG, "get a start cmd : " + act);
        }    
        if (act.equals(ACTION_CHECK)) {
            if (PrefUtils.DEBUG) Log.v(TAG, "status=" + mCheckingTask.getRunningStatus());
            if (mCheckingTask.getRunningStatus() != UpdateTasks.RUNNING_STATUS_RUNNING) {
                mCheckingTask.start();
            }
        }else if (act.equals(ACTION_DOWNLOAD)) {
            if (PrefUtils.DEBUG) Log.v(TAG, "status=" + mDownloadTask.getRunningStatus());
            if (mDownloadTask.getRunningStatus() != UpdateTasks.RUNNING_STATUS_RUNNING) {
                mDownloadTask.start();
            }
        }else if (act.equals(ACTION_AUTOCHECK)) {
            if(timer == null) {
                timer = new Timer();
                timer.schedule(autoCheckTask,0,60000*30);//period=30min
            }
        }
        return 0;
    }
    public class uiBinder extends Binder {
        public boolean resetTask(int taskId) {
            boolean b = false;
            switch (taskId) {
                case TASK_ID_CHECKING:
                    b = mCheckingTask.reset();
                    break;
                case TASK_ID_DOWNLOAD:
                    b = mDownloadTask.reset();
                    break;
            }
            return b;
        }
        
        public void setTaskPause(int taskId) {
            switch (taskId) {
                case TASK_ID_CHECKING:
                    mCheckingTask.pause();
                    break;
                case TASK_ID_DOWNLOAD:
                    mDownloadTask.pause();
                    break;
            }
        }
        
        public void setTaskResume(int taskId) {
            switch (taskId) {
                case TASK_ID_CHECKING:
                    mCheckingTask.resume();
                    break;
                case TASK_ID_DOWNLOAD:
                    mDownloadTask.resume();
                    break;
            }
        }
        
        public int getTaskRunnningStatus(int taskId) {
            int status = -1;
            switch (taskId) {
                case TASK_ID_CHECKING:
                    status = mCheckingTask.getRunningStatus();
                    break;
                case TASK_ID_DOWNLOAD:
                    status = mDownloadTask.getRunningStatus();
                    break;
            }
            return status;
        }
        
        public Object getTaskResult(int taskId) {
            Object result = null;
            switch (taskId) {
                case TASK_ID_CHECKING:
                    result = mCheckingTask.getResult();
                    break;
                case TASK_ID_DOWNLOAD:
                    result = mDownloadTask.getResult();
                    break;
            }
            return result;
        }
        
        public int getTaskErrorCode(int taskId) {
            int errorCode = -1;
            switch (taskId) {
                case TASK_ID_CHECKING:
                    errorCode = mCheckingTask.getErrorCode();
                    break;
                case TASK_ID_DOWNLOAD:
                    errorCode = mDownloadTask.getErrorCode();
                    break;
            }
            return errorCode;
        }
        
        public int getTaskProgress(int taskId) {
            int progress = -1;
            switch (taskId) {
                case TASK_ID_CHECKING:
                    progress = mCheckingTask.getProgress();
                    break;
                case TASK_ID_DOWNLOAD:
                    progress = mDownloadTask.getProgress();
                    break;
            }
            return progress;
        }
        public void startTask(int taskId){
            switch (taskId) {
                case TASK_ID_CHECKING:
                    Log.v(TAG, "status=" + mDownloadTask.getRunningStatus());
                    if (mDownloadTask.getRunningStatus()!=UpdateTasks.RUNNING_STATUS_UNSTART){
                        mDownloadTask.reset();
                    }
                    if (mCheckingTask.getRunningStatus() == UpdateTasks.RUNNING_STATUS_UNSTART) {
                        mCheckingTask.start();
                    }
                    break;
                case TASK_ID_DOWNLOAD:
                    if (mDownloadTask.getRunningStatus() != UpdateTasks.RUNNING_STATUS_RUNNING) {
                        mDownloadTask.start();
                    }
                    break;
            }
        }
    };
    
    private void initInstance() {
        if (mCheckingTask == null) {
            mCheckingTask = new CheckUpdateTask(this);
        }
        if (mDownloadTask == null) {
            mDownloadTask = new DownloadUpdateTask(this);
            ((DownloadUpdateTask) mDownloadTask).setNotify(notice);
        }
    }
    
    private Context mContext = null;
    
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getBaseContext();
        initInstance();
    }
    public class DownloadNotify implements Notifier{

        /* (non-Javadoc)
         * @see com.amlogic.update.Notifier#Failednotify()
         */
        @Override
        public void Failednotify() {}

        /* (non-Javadoc)
         * @see com.amlogic.update.Notifier#Successnotify()
         */
        @Override
        public void Successnotify() {
            NotificationManager notificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent intent = new Intent(mContext, UpdateActivity.class);
            intent.setAction(ACTION_DOWNLOAD_SUCCESS);
            PendingIntent contentItent = PendingIntent.getActivity(mContext, 0, intent, 0);
            Notification notification = new Notification();
            notification.icon = R.drawable.ic_icon;
            notification.tickerText = "Downlaod Success";
            notification.defaults = Notification.DEFAULT_SOUND; 
            notification.setLatestEventInfo(mContext, "Upgrade", "downlaod notify", contentItent);
            notificationManager.notify(0, notification);
        }
        
    }

    private TimerTask autoCheckTask = new TimerTask() {
        public void run() {
            if(mDownloadTask.getRunningStatus() != UpdateTasks.RUNNING_STATUS_RUNNING && mCheckingTask.getRunningStatus() != UpdateTasks.RUNNING_STATUS_RUNNING) {
                mCheckingTask.start();
            }
        }
    };
}
