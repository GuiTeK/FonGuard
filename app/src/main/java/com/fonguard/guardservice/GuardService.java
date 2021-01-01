/*
 * FonGuard
 * Copyright (C) 2021  Guillaume TRUCHOT <guillaume.truchot@outlook.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fonguard.guardservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.fonguard.MainActivity;
import com.fonguard.R;
import com.fonguard.guardservice.triggers.MotionTrigger;

import java.util.concurrent.atomic.AtomicBoolean;

public class GuardService extends Service {
    public enum ServiceAction {
        START,
        STOP
    }

    private static final String LOG_TAG = GuardService.class.getName();
    private static final AtomicBoolean sIsServiceStarted = new AtomicBoolean(false);

    private PowerManager.WakeLock mWakeLock;
    private MotionTrigger mMotionTrigger;

    private HandlerThread mMotionTriggerHandlerThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            if (intent.getAction() != null &&
                    intent.getAction().equals(ServiceAction.START.toString())) {
                startService();
            } else if (intent.getAction() != null &&
                    intent.getAction().equals(ServiceAction.STOP.toString())) {
                stopService();
            } else {
                throw new IllegalArgumentException("Intent action must be either START or STOP");
            }
        } else {
            Log.i(LOG_TAG, "service was restarted by the system itself");
            startService();
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Intent restartIntent;

        Log.i(LOG_TAG, "service is being killed by system...");

        super.onDestroy();

        stopService();

        restartIntent = new Intent(this, RestartReceiver.class);
        restartIntent.setAction(RestartReceiver.INTENT_ACTION_RESTART_GUARD_SERVICE);

        Log.i(LOG_TAG, "sending broadcast to restart GuardService...");
        sendBroadcast(restartIntent);
    }

    public static void actionService(ServiceAction action, Context context) {
        Intent intent = new Intent(context, GuardService.class);

        intent.setAction(action.toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static boolean isServiceStarted() {
        return sIsServiceStarted.get();
    }


    private void startService() {
        Notification notification;
        PowerManager powerManager;
        Message motionTriggerStartMsg;

        Log.i(LOG_TAG, "starting service...");

        if (sIsServiceStarted.get()) {
            Log.w(LOG_TAG, "service is already started");
            return;
        }

        notification = createNotification();
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);

        startForeground(1, notification);

        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "FonGuard:GuardService");
        mWakeLock.acquire();

        mMotionTriggerHandlerThread = new HandlerThread("MotionTriggerHandlerThread");
        mMotionTriggerHandlerThread.start();
        mMotionTrigger = new MotionTrigger(mMotionTriggerHandlerThread.getLooper(),
                this);
        motionTriggerStartMsg = mMotionTrigger.obtainMessage(MotionTrigger.HANDLER_MSG_RESTART);
        mMotionTrigger.sendMessage(motionTriggerStartMsg);

        sIsServiceStarted.set(true);

        Log.i(LOG_TAG, "service started");
    }

    private void stopService() {
        Log.i(LOG_TAG, "stopping service...");

        if (!sIsServiceStarted.get()) {
            Log.w(LOG_TAG, "service is already stopped");
            return;
        }

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        stopForeground(true);
        stopSelf();

        sIsServiceStarted.set(false);
        Log.i(LOG_TAG, "service stopped");
    }


    private Notification createNotification() {
        String notificationChannelId = "GuardService";
        Notification.Builder notificationBuilder;
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(
                    notificationChannelId,
                    getString(R.string.guardservice_foreground_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);

            notificationManager.createNotificationChannel(notificationChannel);
        }

        notificationBuilder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                new Notification.Builder(this, notificationChannelId) :
                new Notification.Builder(this);

        return notificationBuilder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.guardservice_foreground_notification_title))
                .setContentText(getString(R.string.guardservice_foreground_notification_text))
                .setContentIntent(notificationPendingIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
    }
}
