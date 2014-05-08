package it.unibo.cs.jonus.waidrec;

import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.widget.TextView;

public class Magnitude extends Activity {

	private SensorManager mSensorManager;

	private long lastAccelCheck;
	private long lastGyroCheck;
	private Sensor accelSensor;
	private Sensor gyroSensor;
	public double accelMagnitude;
	public double gyroMagnitude;

	private TextView accelView;
	private TextView gyroView;

	SensorEventListener accelListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (SystemClock.elapsedRealtime() > lastAccelCheck + 200) {
				lastAccelCheck = SystemClock.elapsedRealtime();
				double magnitude = Math.sqrt(event.values[0] * event.values[0]
						+ event.values[1] * event.values[1] + event.values[2]
						* event.values[2]);
				accelView.setText(String.valueOf(magnitude));
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	};

	SensorEventListener gyroListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (SystemClock.elapsedRealtime() > lastGyroCheck + 200) {
				lastGyroCheck = SystemClock.elapsedRealtime();
				double magnitude = Math.sqrt(event.values[0] * event.values[0]
						+ event.values[1] * event.values[1] + event.values[2]
						* event.values[2]);
				gyroView.setText(String.valueOf(magnitude));
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_magnitude);

		accelView = (TextView) findViewById(R.id.accelView);
		gyroView = (TextView) findViewById(R.id.gyroView);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		registerSensors();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.magnitude, menu);
		return true;
	}

	@Override
	public void onRestart() {
		super.onRestart();

		registerSensors();
	}

	@Override
	public void onStop() {
		unregisterSensors();

		super.onStop();
	}

	private void registerSensors() {
		accelSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		if (accelSensor != null) {
			List<Sensor> ls = mSensorManager
					.getSensorList(Sensor.TYPE_ACCELEROMETER);
			for (int i = 0; i < ls.size(); i++) {
				Sensor s_i = ls.get(i);
				mSensorManager.registerListener(accelListener, s_i,
						SensorManager.SENSOR_DELAY_NORMAL);
			}

			accelView.setText("0");
		}
		if (gyroSensor != null) {
			List<Sensor> ls = mSensorManager
					.getSensorList(Sensor.TYPE_GYROSCOPE);
			for (int i = 0; i < ls.size(); i++) {
				Sensor s_i = ls.get(i);
				mSensorManager.registerListener(gyroListener, s_i,
						SensorManager.SENSOR_DELAY_NORMAL);
			}

			gyroView.setText("0");
		}

		lastAccelCheck = SystemClock.elapsedRealtime();
		lastGyroCheck = SystemClock.elapsedRealtime();
	}

	private void unregisterSensors() {
		mSensorManager.unregisterListener(accelListener);
		mSensorManager.unregisterListener(gyroListener);
	}

}
