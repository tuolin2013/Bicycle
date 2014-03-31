package com.tuo.utils.gps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class GpsApi {
	private final static String API_KEY = "ow7b0a6c04ljmkz0dn69";
	private String rat;
	private int cid, lac, mcc, mnc, cellPadding;

	private TelephonyManager tm;

	private LocationManager mLocationManager = null;
	private GsmCellLocation mGsmCellLocation;

	public GpsApi(Context ctx) {
		mLocationManager = (LocationManager) ctx.getApplicationContext()
				.getSystemService(Context.LOCATION_SERVICE);
		tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

	}

	public Location getLocation() {

		Criteria criteria = new Criteria();
		String best;
		best = mLocationManager.getBestProvider(criteria, true);
		Location location = mLocationManager.getLastKnownLocation(best);
		return location;

	}

	public boolean isGpsEnabled() {
		return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	public boolean isNetworkEnabled() {
		return mLocationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	public Map<String, String> getBestLocation() throws Exception {
		mGsmCellLocation = (GsmCellLocation) tm.getCellLocation();
		if (mGsmCellLocation == null) {
			return null;
		}

		cid = mGsmCellLocation.getCid();
		lac = mGsmCellLocation.getLac();
		/*
		 * Mcc and mnc is concatenated in the networkOperatorString. The first 3
		 * chars is the mcc and the last 2 is the mnc.
		 */
		String networkOperator = tm.getNetworkOperator();
		if (networkOperator != null && networkOperator.length() > 0) {
			try {
				mcc = Integer.parseInt(networkOperator.substring(0, 3));
				mnc = Integer.parseInt(networkOperator.substring(3));
			} catch (NumberFormatException e) {
			}
		}

		/*
		 * Check if the current cell is a UMTS (3G) cell. If a 3G cell the cell
		 * id padding will be 8 numbers, if not 4 numbers.
		 */

		switch (tm.getNetworkType()) {
		case TelephonyManager.NETWORK_TYPE_HSDPA:
		case TelephonyManager.NETWORK_TYPE_HSPA:
		case TelephonyManager.NETWORK_TYPE_HSUPA:
		case TelephonyManager.NETWORK_TYPE_UMTS:
			cellPadding = 8;
			rat = "W";
			break;
		default:
			cellPadding = 4;
			rat = "G";
			break;
		}
		Map<String, String> result = null;
		InputStream is = null;
		ByteArrayOutputStream bos = null;
		byte[] data = null;
		try {

			// Build the url
			String url = "http://location-api.com/cps/?key=" + API_KEY
					+ "&cell=" + rat + "," + mcc + "," + mnc + "," + lac + ","
					+ cid;

			StringBuilder uri = new StringBuilder(url);

			// Create an HttpGet request
			HttpGet request = new HttpGet(uri.toString());

			// Send the HttpGet request
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);

			// Check the response status
			int status = response.getStatusLine().getStatusCode();
			if (status != HttpURLConnection.HTTP_OK) {
				throw new IOException("HTTP response code: " + status);
			}

			// The response was ok (HTTP_OK) so lets read the data
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
			bos = new ByteArrayOutputStream();
			byte buf[] = new byte[256];
			while (true) {
				int rd = is.read(buf, 0, 256);
				if (rd == -1)
					break;
				bos.write(buf, 0, rd);
			}
			bos.flush();
			data = bos.toByteArray();
			if (data != null) {
				String responseString = new String(data);
				System.out.println(responseString);
				result = new HashMap<String, String>();
				String[] values = responseString.split(";");
				for (String value : values) {
					String[] parts = value.split("=");
					if (parts.length == 2) {
						result.put(parts[0], parts[1]);
					}
				}

				if (result.containsKey("status")) {
					int reqStatus = parseInt(result.get("status"));
					switch (reqStatus) {
					case 0:

						break;

					case 1:
						throw new IOException("The cell could not be found"
								+ "in the database");
					case 2:
						throw new IOException("You have reached the limit"
								+ "for the number of requests.");
					case 3:
						throw new IOException("Make sure the API key is "
								+ "present and valid");
					case 4:
					case 5:
						throw new IOException("Invalid parameters");

					}

				} else {
					throw new IOException("Something went wrong");
				}

			}
		} catch (MalformedURLException e) {
			Log.e("ERROR", e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new IOException(
					"URL was incorrect. Did you forget to set the API_KEY?");
		} finally {
			// make sure we clean up after us
			try {
				if (bos != null)
					bos.close();
			} catch (Exception e) {
			}
			try {
				if (is != null)
					is.close();
			} catch (Exception e) {
			}
		}

		return result;

	}

	private int parseInt(String s) {
		try {
			if (s != null && s.length() > 0)
				return Integer.parseInt(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;

	}

}

