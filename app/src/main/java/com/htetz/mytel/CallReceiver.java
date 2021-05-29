package com.htetz.mytel;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static com.htetz.mytel.MainActivity.NOTI_TITLE;
import static com.htetz.mytel.MainActivity.RUNNING;

public class CallReceiver extends BroadcastReceiver {
    private final String TAG = "MytelBlocker";
    private SharedPreferences sharedPreferences;

    private final static AtomicInteger c = new AtomicInteger(0);
    public static int getID() {
        return c.incrementAndGet();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction()) && intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            if (!sharedPreferences.getBoolean(RUNNING,false))
                return;

            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            /* swy: we can receive two notifications; the first one doesn't
                    have EXTRA_INCOMING_NUMBER, so just skip it */
            Log.i(TAG, "Received call: " + incomingNumber);
            if (incomingNumber == null)
                return;

            String prefix = context.getString(R.string.mytel_prefix);
            if (incomingNumber.startsWith(prefix)){
                rejectCall(context,incomingNumber);
            }
        }
    }

    @SuppressLint("MissingPermission")
    protected void rejectCall(@NonNull Context context,String number) {
            boolean failed = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                try {
                    telecomManager.endCall();
                    Log.d(TAG, "Invoked 'endCall' on TelecomManager");
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't end call with TelecomManager", e);
                    failed = true;
                }
            } else {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                try {
                    Method m = tm.getClass().getDeclaredMethod("getITelephony");
                    m.setAccessible(true);
                    ITelephony telephony = (ITelephony) m.invoke(tm);
                    telephony.endCall();
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't end call with TelephonyManager", e);
                    failed = true;
                }
            }
            if (failed) {
                Toast.makeText(context, "SORRY", Toast.LENGTH_LONG).show();
            }else showNoti(context,number);
    }


    private void showNoti(Context context,String number){
        if (!sharedPreferences.getBoolean("show_noti",false))
            return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager =  (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("default", context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Mytel number blocker ;)");
            notificationManager.createNotificationChannel(channel);
        }

        Notification notify = new NotificationCompat.Builder(context, "Mytel_Blocker_By_HtetzNaing")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(number)
                .setContentText(sharedPreferences.getString(NOTI_TITLE,null))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .addPerson("tel:" + number)
                .setGroup("rejected")
                .setChannelId("default")
                .setGroupSummary(true) /* swy: fix notifications not appearing on kitkat: https://stackoverflow.com/a/37070917/674685 */
                .build();

        String tag = number != null ? number : "private";
        NotificationManagerCompat.from(context).notify(tag, getID(), notify);
    }
}
