package com.google.appinventor.components.runtime;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;


import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.widget.Toast;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    public final String ACTION = "com.google.appinventor.components.runtime.AlarmReceiver";

    private Alarm callback;

    public AlarmReceiver(Alarm alarm) {
        callback = alarm;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Alarm", "onReceive");

        callback.Fired();
     }  

	public void setAlarm(Context context, int end, int interval, boolean repeat) {
        Log.i("Alarm", "setRepeatingAlarm");
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        if (repeat) {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + end, interval, pi);
        }
        else {
            alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + end, pi);
        }
    }

    public void cancelAlarm(Context context) {
        Log.i("Alarm", "cancelAlarm");
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarm.cancel(sender);
    }
}