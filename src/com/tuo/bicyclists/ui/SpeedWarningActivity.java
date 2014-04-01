package com.tuo.bicyclists.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tuo.bicyclists.R;
import com.tuo.bicyclists.util.tools.DatetimeTools;
import com.tuo.third.library.shimmer.Shimmer;
import com.tuo.third.library.shimmer.ShimmerTextView;
import com.tuo.ui.widget.SpeedometerView;
import com.tuo.utils.gps.AppSettings;
import com.tuo.utils.gps.Constants;
import com.tuo.utils.gps.Distance;
import com.tuo.utils.gps.GPSCallback;
import com.tuo.utils.gps.GPSManager;
import com.tuo.utils.gps.GpsApi;
import com.widget.radialmenu.RadialMenuItem;
import com.widget.radialmenu.RadialMenuWidget;

public class SpeedWarningActivity extends Activity implements GPSCallback {
	String TAG = "SpeedWarningActivity";

	// ui
	SpeedometerView speedometer;
	ImageView compassImage;
	TextView compassTextView, temperatureTextView, mileageTextView;
	TextView hourTextView, minTextView, secTextView, startTimeTextView;
	Button startButton, endButton;
	ShimmerTextView shimmerTextView;
	// record the compass picture angle turned
	private float currentDegree = 0f;
	// device sensor manager
	private SensorManager mSensorManager;
	private Sensor temperatureSensor, compassSensor;

	private LocationManager mLocationManager = null;
	private GPSManager gpsManager = null;
	private double speed = 0.0;
	private int measurement_index = Constants.INDEX_KM;
	private AbsoluteSizeSpan sizeSpanLarge = null;
	private AbsoluteSizeSpan sizeSpanSmall = null;

	// Radial menu
	private RadialMenuWidget pieMenu;
	public Activity activity = SpeedWarningActivity.this;
	public RadialMenuItem whereAmImenuItem, menuCloseItem, whereTogoMenuItem, donateMenuItem;
	public RadialMenuItem firstChildItem, secondChildItem, thirdChildItem;
	private List<RadialMenuItem> children = new ArrayList<RadialMenuItem>();

	// gps
	private Location startLocation = null, currentLocation;
	private double mileage;
	private long startTimeStamp, endTimeStamp;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_speed);
		initView();
		// initialize your android device sensor capabilities
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		temperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		compassSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mSensorManager.registerListener(temperatureSensorEventListener, temperatureSensor, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(compassSensorEventListener, compassSensor, SensorManager.SENSOR_DELAY_FASTEST);
		gpsManager = new GPSManager();
		gpsManager.startListening(getApplicationContext());
		gpsManager.setGPSCallback(this);
		((TextView) findViewById(R.id.info_message)).setText(getString(R.string.info));
		measurement_index = AppSettings.getMeasureUnit(this);
		initializeLocationManager();
	}

	@Override
	public void onGPSUpdate(Location location) {
		currentLocation = location;
		location.getLatitude();
		location.getLongitude();
		speed = location.getSpeed();

		String speedString = "" + roundDecimal(convertSpeed(speed), 2);
		String unitString = measurementUnitString(measurement_index);

		setSpeedText(R.id.info_message, speedString + " " + unitString);
		setSpeedometer(speedString);
		if (speed > 0) {
			mileageHandler.postDelayed(CalculateMileageRunnable, 3000);
		}

	}

	@Override
	protected void onDestroy() {
		gpsManager.stopListening();
		gpsManager.setGPSCallback(null);
		gpsManager = null;
		mSensorManager.unregisterListener(temperatureSensorEventListener);
		mSensorManager.unregisterListener(compassSensorEventListener);
		if (mLocationManager != null) {
			mLocationManager.removeGpsStatusListener(gpsListener);
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = true;

		switch (item.getItemId()) {
		case R.id.menu_about: {
			displayAboutDialog();

			break;
		}
		case R.id.unit_km: {
			measurement_index = 0;

			AppSettings.setMeasureUnit(this, 0);

			break;
		}
		case R.id.unit_miles: {
			measurement_index = 1;

			AppSettings.setMeasureUnit(this, 1);

			break;
		}
		default: {
			result = super.onOptionsItemSelected(item);

			break;
		}
		}

		return result;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// for the system's orientation sensor registered listeners
		mSensorManager.registerListener(temperatureSensorEventListener, temperatureSensor, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(compassSensorEventListener, compassSensor, SensorManager.SENSOR_DELAY_FASTEST);
		initializeLocationManager();
	}

	@Override
	protected void onPause() {
		super.onPause();

		// to stop the listener and save battery
		mSensorManager.unregisterListener(temperatureSensorEventListener);
		mSensorManager.unregisterListener(compassSensorEventListener);
		if (mLocationManager != null) {
			mLocationManager.removeGpsStatusListener(gpsListener);
		}

	}

	void initData() {
		mileage = 0f;
		startTimeStamp = System.currentTimeMillis();
	}

	Handler timeHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			JSONObject obj = (JSONObject) msg.obj;
			try {
				java.text.DecimalFormat df = new java.text.DecimalFormat("00");

				hourTextView.setText(df.format(Double.parseDouble(obj.getString("hour"))));
				minTextView.setText(df.format(Double.parseDouble(obj.getString("min"))));
				secTextView.setText(df.format(Double.parseDouble(obj.getString("sec"))));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	};
	Runnable timeRunnable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			// 要做的事情
			JSONObject obj = DatetimeTools.getDistanceTime(startTimeStamp, System.currentTimeMillis());
			timeHandler.obtainMessage(0, -1, -1, obj).sendToTarget();
			timeHandler.postDelayed(this, 1000);
		}
	};

	void initView() {
		shimmerTextView = (ShimmerTextView) findViewById(R.id.shimmerTextView1);
		// Customize SpeedometerView
		speedometer = (SpeedometerView) findViewById(R.id.speedometer);

		// Add label converter
		speedometer.setLabelConverter(new SpeedometerView.LabelConverter() {
			@Override
			public String getLabelFor(double progress, double maxProgress) {
				return String.valueOf((int) Math.round(progress));
			}
		});

		// configure value range and ticks
		speedometer.setMaxSpeed(60);
		speedometer.setMajorTickStep(10);
		speedometer.setMinorTicks(1);
		speedometer.setLabelTextSize(48);
		speedometer.setSpeed(0);

		// Configure value range colors
		// speedometer.addColoredRange(0, 15, Color.GREEN);
		// speedometer.addColoredRange(15, 30, Color.YELLOW);
		// speedometer.addColoredRange(30, 60, Color.RED);

		compassImage = (ImageView) findViewById(R.id.iv_compass);
		compassTextView = (TextView) findViewById(R.id.tv_compass);
		temperatureTextView = (TextView) findViewById(R.id.tv_temperature);

		mileageTextView = (TextView) findViewById(R.id.tv_mileage);
		hourTextView = (TextView) findViewById(R.id.tv_hour);
		minTextView = (TextView) findViewById(R.id.tv_min);
		secTextView = (TextView) findViewById(R.id.tv_sec);
		startTimeTextView = (TextView) findViewById(R.id.tv_start_time);
		startButton = (Button) findViewById(R.id.btn_start);
		endButton = (Button) findViewById(R.id.btn_end);

		final View mileageView = findViewById(R.id.ll_mileage);

		startButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				initData();
				mileageView.setVisibility(View.VISIBLE);
				timeHandler.postDelayed(timeRunnable, 1000);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				startTimeTextView.setText(sdf.format(new Date(startTimeStamp)));
				v.setEnabled(false);
			}
		});

		endButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				timeHandler.removeCallbacks(timeRunnable);
			}
		});

		initRadialMenu();
	}

	private void initRadialMenu() {
		pieMenu = new RadialMenuWidget(SpeedWarningActivity.this);

		menuCloseItem = new RadialMenuItem("Close", null);
		menuCloseItem.setDisplayIcon(android.R.drawable.ic_menu_close_clear_cancel);

		whereAmImenuItem = new RadialMenuItem("whereAmI", "我的位置");

		// firstChildItem = new RadialMenuItem("First", "First");
		// firstChildItem.setOnMenuItemPressed(new
		// RadialMenuItem.RadialMenuItemClickListener() {
		// @Override
		// public void execute() {
		// pieMenu.dismiss();
		// }
		// });
		//
		// secondChildItem = new RadialMenuItem("Second", null);
		// secondChildItem.setDisplayIcon(R.drawable.ic_launcher);
		// secondChildItem.setOnMenuItemPressed(new
		// RadialMenuItem.RadialMenuItemClickListener() {
		// @Override
		// public void execute() {
		// Toast.makeText(activity, "Second inner menu selected.",
		// Toast.LENGTH_LONG).show();
		// }
		// });
		//
		// thirdChildItem = new RadialMenuItem("Third", "Third");
		// thirdChildItem.setDisplayIcon(R.drawable.ic_launcher);
		// thirdChildItem.setOnMenuItemPressed(new
		// RadialMenuItem.RadialMenuItemClickListener() {
		// @Override
		// public void execute() {
		// Toast.makeText(activity, "Third inner menu selected.",
		// Toast.LENGTH_LONG).show();
		// }
		// });

		whereTogoMenuItem = new RadialMenuItem("whereTogo", "地图导航");
		donateMenuItem = new RadialMenuItem("donate", "捐赠咖啡");

		// children.add(firstChildItem);
		// children.add(secondChildItem);
		// children.add(thirdChildItem);
		// menuExpandItem.setMenuChildren(children);

		menuCloseItem.setOnMenuItemPressed(new RadialMenuItem.RadialMenuItemClickListener() {
			@Override
			public void execute() {
				// menuLayout.removeAllViews();
				pieMenu.dismiss();
			}
		});

		whereAmImenuItem.setOnMenuItemPressed(new RadialMenuItem.RadialMenuItemClickListener() {
			@Override
			public void execute() {

				pieMenu.dismiss();
			}
		});

		pieMenu.setAnimationSpeed(0L);
		pieMenu.setSourceLocation(200, 200);
		pieMenu.setIconSize(15, 30);
		pieMenu.setTextSize(16);
		pieMenu.setOutlineColor(Color.BLACK, 225);
		pieMenu.setInnerRingColor(0xAA66CC, 180);
		pieMenu.setOuterRingColor(0x0099CC, 180);
		// pieMenu.setHeader("Test Menu", 20);
		pieMenu.setCenterCircle(menuCloseItem);

		pieMenu.addMenuEntry(new ArrayList<RadialMenuItem>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add(whereAmImenuItem);
				add(whereTogoMenuItem);
				add(donateMenuItem);
			}
		});
		View menuLayout = findViewById(R.id.ll_container);
		menuLayout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (pieMenu.isShown()) {
					pieMenu.dismiss();

				} else {
					int scrWidth = getWindowManager().getDefaultDisplay().getWidth();
					int scrHeight = getWindowManager().getDefaultDisplay().getHeight();
					int w = pieMenu.getWidth();
					int h = pieMenu.getHeight();

					pieMenu.setCenterLocation(scrWidth / 2, scrHeight / 2);
					pieMenu.show(v);
				}

				return false;
			}
		});
	}

	private double convertSpeed(double speed) {
		return ((speed * Constants.HOUR_MULTIPLIER) * Constants.UNIT_MULTIPLIERS[measurement_index]);
	}

	private String measurementUnitString(int unitIndex) {
		String string = "";

		switch (unitIndex) {
		case Constants.INDEX_KM:
			string = "km/h";
			break;
		case Constants.INDEX_MILES:
			string = "mi/h";
			break;
		}

		return string;
	}

	private double roundDecimal(double value, final int decimalPlace) {
		BigDecimal bd = new BigDecimal(value);

		bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
		value = bd.doubleValue();

		return value;
	}

	private void setSpeedText(int textid, String text) {
		Spannable span = new SpannableString(text);
		int firstPos = text.indexOf(32);

		span.setSpan(sizeSpanLarge, 0, firstPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		span.setSpan(sizeSpanSmall, firstPos + 1, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		TextView tv = ((TextView) findViewById(textid));

		tv.setText(span);
	}

	private void setSpeedometer(String speed) {

		double progress = Double.parseDouble(speed);
		long duration = 1000;
		long startDelay = 1000;
		if (progress > 0) {
			speedometer.setSpeed(progress, duration, startDelay);
		}

	}

	private void setDegrees(float degree) {
		float _decDegree = 0;
		String _message = "";
		// 设置灵敏度
		if (Math.abs(_decDegree - degree) >= 2) {
			_decDegree = degree;

			int range = 22;

			String degreeStr = String.valueOf(_decDegree);

			// 指向正北
			if (_decDegree > 360 - range && _decDegree < 360 + range) {
				_message = "正北 " + degreeStr + "°";
			}

			// 指向正东
			if (_decDegree > 90 - range && _decDegree < 90 + range) {
				_message = "正东 " + degreeStr + "°";
			}

			// 指向正南
			if (_decDegree > 180 - range && _decDegree < 180 + range) {
				_message = "正南 " + degreeStr + "°";
			}

			// 指向正西
			if (_decDegree > 270 - range && _decDegree < 270 + range) {
				_message = "正西 " + degreeStr + "°";
			}

			// 指向东北
			if (_decDegree > 45 - range && _decDegree < 45 + range) {
				_message = "东北 " + degreeStr + "°";
			}

			// 指向东南
			if (_decDegree > 135 - range && _decDegree < 135 + range) {
				_message = "东南 " + degreeStr + "°";
			}

			// 指向西南
			if (_decDegree > 225 - range && _decDegree < 225 + range) {
				_message = "西南 " + degreeStr + "°";
			}

			// 指向西北
			if (_decDegree > 315 - range && _decDegree < 315 + range) {
				_message = "西北 " + degreeStr + "°";
			}
			compassTextView.setText(_message);
		}

	}

	private void displayAboutDialog() {
		final LayoutInflater inflator = LayoutInflater.from(this);
		final View settingsview = inflator.inflate(R.layout.about, null);
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(getString(R.string.app_name));
		builder.setView(settingsview);

		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});

		builder.create().show();
	}

	private final SensorEventListener compassSensorEventListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
				// TODO Auto-generated method stub
				float degree = Math.round(event.values[0]);
				// create a rotation animation (reverse turn degree degrees)
				RotateAnimation ra = new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f);

				// how long the animation will take place
				ra.setDuration(210);

				// set the animation after the end of the reservation status
				ra.setFillAfter(true);

				// Start the animation
				compassImage.startAnimation(ra);
				currentDegree = -degree;
				setDegrees(degree);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	};

	private final SensorEventListener temperatureSensorEventListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
				/* 温度传感器返回当前的温度，单位是摄氏度（°C）。 */
				float temperature = event.values[0];
				temperatureTextView.setText(String.valueOf(temperature) + "°C");
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	};

	Handler mileageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00");
			double d = (Double) msg.obj;
			mileage += d;
			mileageTextView.setText(df.format(mileage));
		}
	};
	Runnable CalculateMileageRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (currentLocation != null && startLocation != null) {
				// double d =
				// Distance.calculateDistance(currentLocation.getLatitude(),
				// currentLocation.getLongitude(),
				// startLocation.getLatitude(), startLocation.getLongitude(),
				// Distance.KILOMETERS);
				double d = Distance.getDistance(startLocation.getLatitude(), startLocation.getLongitude(), currentLocation.getLatitude(),
						currentLocation.getLongitude());
				mileageHandler.obtainMessage(0, -1, -1, d).sendToTarget();
			}
		}

	};

	private void initializeLocationManager() {
		Log.e(TAG, "initializeLocationManager");
		int LOCATION_INTERVAL = 500;
		float LOCATION_DISTANCE = 0f;

		if (mLocationManager == null) {
			mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		}
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setSpeedRequired(true);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		String best;
		best = mLocationManager.getBestProvider(criteria, true);
		MyLocationListener locationListener = new MyLocationListener();
		if (best != null && best.length() > 0) {
			mLocationManager.requestLocationUpdates(best, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);

		} else {
			List<String> providers = mLocationManager.getProviders(true);
			for (final String provider : providers) {
				mLocationManager.requestLocationUpdates(provider, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
			}
		}

		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			mLocationManager.addGpsStatusListener(gpsListener);
		}

	}

	private GpsStatus.Listener gpsListener = new GpsStatus.Listener() {

		public void onGpsStatusChanged(int event) {

			switch (event) {
			case GpsStatus.GPS_EVENT_STARTED:
				Log.i(TAG, "定位启动");
				Toast.makeText(SpeedWarningActivity.this, "定位启动", Toast.LENGTH_LONG).show();
				break;
			case GpsStatus.GPS_EVENT_STOPPED:
				Log.i(TAG, "定位结束");
				Toast.makeText(SpeedWarningActivity.this, "定位结束", Toast.LENGTH_LONG).show();
				break;
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				// 获取当前状态
				GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
				// 获取卫星颗数的默认最大值
				int maxSatellites = gpsStatus.getMaxSatellites();
				// 创建一个迭代器保存所有卫星
				Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
				int count = 0;
				while (iters.hasNext() && count <= maxSatellites) {
					GpsSatellite s = iters.next();
					count++;
				}
				Log.i(TAG, "搜索到：" + count + "颗卫星");
				Shimmer shimmer = new Shimmer();
				shimmerTextView.setText("搜索到：" + count + "颗卫星");
				shimmer.start(shimmerTextView);
				break;
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				Log.i(TAG, "第一次定位");
				Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (gpsLocation != null) {
					if (startLocation == null) {
						startLocation = gpsLocation;
					}

				}
				Toast.makeText(SpeedWarningActivity.this, "第一次定位", Toast.LENGTH_LONG).show();
				break;
			}

		}

	};

	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onLocationChanged: " + location);
		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onProviderDisabled: " + provider);
		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onProviderEnabled: " + provider);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			switch (status) {
			case LocationProvider.AVAILABLE:
				Log.i(TAG, "当前:" + provider + " 状态为可见状态");
				break;
			case LocationProvider.OUT_OF_SERVICE:
				Log.i(TAG, "当前" + provider + "状态为服务区外状态");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Log.i(TAG, "当前" + provider + "状态为暂停服务状态");
			}

		}

	}

}