package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import android.content.SharedPreferences;

import org.json.JSONException;
import java.lang.NumberFormatException;


@DesignerComponent(version = YaVersion.ALARM_COMPONENT_VERSION,
	description ="<p>Simple Alarm</p>",
	category = ComponentCategory.SENSORS,
	nonVisible = true)
public class Alarm extends AndroidNonvisibleComponent 
				   implements OnResumeListener, OnPauseListener, OnStopListener, OnDestroyListener {

	private AlarmReceiver ar;
	private Context context;

	private long startTime;
	private int end;
	private int interval;
	private String active = "false";
	private String onetime;

	private SharedPreferences sharedPreferences;

	public Alarm(ComponentContainer container) {
		super(container.$form());

		context = container.$context();
		ar = new AlarmReceiver(this);

		IntentFilter intentFilter = new IntentFilter(ar.ACTION);
		context.registerReceiver(ar, intentFilter);

		sharedPreferences = container.$context().getSharedPreferences("_TIMER_", Context.MODE_PRIVATE);

		form.registerForOnResume(this);
		form.registerForOnPause(this);
		form.registerForOnStop(this);
		form.registerForOnDestroy(this);
	}

	@SimpleFunction
	public void StartRepeating(int end, int interval) {
		Log.i("Alarm", "StartRepeating");

	 	saveState(end, interval, "true", "false");
	 	ar.setAlarm(context, end, interval, true);
	}

	@SimpleFunction
	public void StartOnce(int end) {
		Log.i("Alarm", "StartOnce");

		saveState(end, 0, "true", "true");
		ar.setAlarm(context, end, 0, false);
	}

	@SimpleFunction
	public void Stop() {
		this.startTime = 0;
		this.end = 0;
		this.interval = 0;
		this.active = "false";
		ar.cancelAlarm(context);
	}

	@SimpleEvent
 	public void GoesOff() {
 		EventDispatcher.dispatchEvent(this, "GoesOff");
 	}

	@Override
	public void onResume() {
		Log.i("Alarm", "onResume");
		if (ar == null || context == null) {
			Log.i("Alarm", "ar == null || context == null");

			ar = new AlarmReceiver(this);
			IntentFilter intentFilter = new IntentFilter(ar.ACTION);

			context = this.form.$context();
			context.registerReceiver(ar, intentFilter);
		}

		Log.i("Alarm", "reading sharedPreferences");

		try {
 			String startString = (String) JsonUtil.getObjectFromJson((String) sharedPreferences.getString("_ALARM_START_TIME_", "0"));
  			startTime = Long.parseLong(startString);

  			String pauseString = (String) JsonUtil.getObjectFromJson((String) sharedPreferences.getString("_ALARM_END_DELTA_", "0"));
  			end = Integer.parseInt(pauseString);

  			String intervalString = (String) JsonUtil.getObjectFromJson((String) sharedPreferences.getString("_ALARM_INTERVAL_", "0"));
  			interval = Integer.parseInt(intervalString);
  			Log.i("Alarm", "finished reading numbers");
  			Log.i("Alarm", "startTime: " + startTime + " end: " + end + " interval: " + interval);
    	} catch (JSONException e) {
    		Log.i("Alarm", "JSONException");
    		startTime = 0;
			end = 0;
			interval = 0;
      		throw new YailRuntimeError("Value failed to convert from JSON.", "JSON Creation Error.");
    	} catch (NumberFormatException e) {
    		Log.i("Alarm", "NumberFormatException");
    		startTime = 0;
			end = 0;
			interval = 0;
    	} finally {
    		Log.i("Alarm", "finally");
    		active = sharedPreferences.getString("_ALARM_ACTIVE_", "false");
    		onetime = sharedPreferences.getString("_ALARM_ONETIME_", "true");

    		if (active.equals("true") && startTime != 0) {
    			long currentMills = System.currentTimeMillis();
    			if ("onetime".equals("true") && startTime + end < currentMills) {
    				StartOnce((int) (currentMills - startTime - end));
    			}
    			else if ("onetime".equals("false") && interval > 0) {
    				StartRepeating((int) (currentMills - startTime - end), interval);
    			}
    		}
      	}
	}

	@Override
	public void onPause() {
		Log.i("Alarm", "onPause");
	}

	@Override
	public void onStop() {
		Log.i("Alarm", "onStop");
	}

	@Override
	public void onDestroy() {
		Log.i("Alarm", "onDestroy");
 		final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    	try {
      		sharedPrefsEditor.putString("_ALARM_START_TIME_", JsonUtil.getJsonRepresentation(String.format("%d", startTime)));
      		sharedPrefsEditor.putString("_ALARM_END_DELTA_", JsonUtil.getJsonRepresentation(String.format("%d", end)));
      		sharedPrefsEditor.putString("_ALARM_INTERVAL_", JsonUtil.getJsonRepresentation(String.format("%d", interval)));
    	} catch (JSONException e) {
      		throw new YailRuntimeError("Value failed to convert to JSON.", "JSON Creation Error.");
    	} finally {
    		sharedPrefsEditor.putString("_ALARM_ACTIVE_", active);
      		sharedPrefsEditor.putString("_ALARM_ONETIME_", onetime);
      		sharedPrefsEditor.commit();
      		ar.cancelAlarm(context);
    	}
	}

	public void saveState(int end, int interval, String active, String onetime) {
		this.startTime = System.currentTimeMillis();
	 	this.end = end;
	 	this.interval = interval;
	 	this.active = active;
 		this.onetime = onetime;
	}
}