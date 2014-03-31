
package com.tuo.utils.gps;

import android.location.Location;

public interface GPSCallback
{
	public abstract void onGPSUpdate(Location location);
}
