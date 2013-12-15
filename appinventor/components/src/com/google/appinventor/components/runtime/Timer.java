package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import android.os.Handler;
import android.content.SharedPreferences;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import java.lang.NumberFormatException;

@DesignerComponent(version = YaVersion.TIMER_COMPONENT_VERSION,
	description ="<p>Simple Timer</p>",
	category = ComponentCategory.SENSORS,
	nonVisible = true)
public class Timer extends AndroidNonvisibleComponent 
				   implements OnResumeListener, OnPauseListener, OnStopListener, OnDestroyListener {

	private static boolean DEFAULT_VISIBLE = false;

	private long startTime = 0L;
	private long currentTimeMillis = 0L;

	private long pauseStart = 0L;

	private Handler timerHandler = new Handler();

	private String reading = "00:00:00";

	/**
	* true = in foreground
	* false = in background
	*/

	private boolean sendUpdate = true;

	private SharedPreferences sharedPreferences;

	public Timer(ComponentContainer container) {
		super(container.$form());

		sharedPreferences = container.$context().getSharedPreferences("_TIMER_", Context.MODE_PRIVATE);

		form.registerForOnResume(this);
		form.registerForOnPause(this);
		form.registerForOnStop(this);
		form.registerForOnDestroy(this);
	}

	@Override
	public void onResume() {
		if (timerHandler == null) {
			timerHandler = new Handler();
			try {
     			String startString = (String) JsonUtil.getObjectFromJson((String) sharedPreferences.getString("_TIMER_START_TIME_", ""));
      			startTime = (startString.length() == 0) ? 0 : Long.parseLong(startString);

      			String pauseString = (String) JsonUtil.getObjectFromJson((String) sharedPreferences.getString("_TIMER_PAUSE_START_", ""));
      			pauseStart = (pauseString.length() == 0) ? 0 : Long.parseLong(pauseString);


    		} catch (JSONException e) {
      			throw new YailRuntimeError("Value failed to convert from JSON.", "JSON Creation Error.");
    		} catch (NumberFormatException e) {
    			startTime = 0;
    			pauseStart = 0;
    			sendUpdate = true;
    			reading = "00:00:00";
    		}
    	}
		if (startTime != 0 && pauseStart == 0) {
      		sendUpdate = true;
      		timerHandler.postDelayed(updateTimerThread, 0);
      	}
	}
 	
 	@Override
 	public void onPause() {
 		sendUpdate = false;
 		timerHandler.removeCallbacks(updateTimerThread);
	}

	@Override
 	public void onStop() {
 		sendUpdate = false;
 		timerHandler.removeCallbacks(updateTimerThread);
 	}

 	@Override
 	public void onDestroy() {
 		if (timerHandler != null) {
 			sendUpdate = false;
	 		timerHandler.removeCallbacks(updateTimerThread);
	 		timerHandler = null;
	 		final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
	    	try {
	      		sharedPrefsEditor.putString("_TIMER_START_TIME_", JsonUtil.getJsonRepresentation(String.format("%d", startTime)));
	      		sharedPrefsEditor.putString("_TIMER_PAUSE_START_", JsonUtil.getJsonRepresentation(String.format("%d", pauseStart)));
	      		sharedPrefsEditor.commit();
	    	} catch (JSONException e) {
	      		throw new YailRuntimeError("Value failed to convert to JSON.", "JSON Creation Error.");
	    	}
 		}
 	}

 	private Runnable updateTimerThread = new Runnable() {
 		public void run() {
 			currentTimeMillis = System.currentTimeMillis() - startTime;

 			int secs = (int) (currentTimeMillis/1000);
 			int mins = secs/60;
 			secs = secs % 60;
 			int milliseconds = (int) (currentTimeMillis % 1000);
 			reading = "" + String.format("%02d", mins) + ":" + String.format("%02d", secs) + ":" + String.format("%02d", milliseconds/10);
 			Updated();
 			if (sendUpdate) {
 				 timerHandler.postDelayed(updateTimerThread, 10);
 			}
 		}
 	};

 	@SimpleEvent
 	public void Updated() {
 		EventDispatcher.dispatchEvent(this, "Updated");
 	}

 	@SimpleFunction
	public void Start() {
		if (startTime == 0) {
			startTime = System.currentTimeMillis();
		}
		if (pauseStart != 0) {
			startTime += System.currentTimeMillis() - pauseStart;
			pauseStart = 0;
		 	sendUpdate = true;
		}
		timerHandler.postDelayed(updateTimerThread, 0);
	}

	@SimpleFunction
	public void Pause() {
		if (pauseStart == 0 && startTime != 0) {
			pauseStart = System.currentTimeMillis();
			timerHandler.removeCallbacks(updateTimerThread);
		}
	}

	@SimpleFunction
	public String Lap() {
		return reading;
	}

	@SimpleFunction
	public void Reset() {
		startTime = 0;
		pauseStart = 0;
		sendUpdate = true;
		timerHandler.removeCallbacks(updateTimerThread);
		reading = "00:00:00";
	}
}