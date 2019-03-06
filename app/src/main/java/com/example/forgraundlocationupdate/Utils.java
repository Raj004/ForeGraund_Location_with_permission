package com.example.forgraundlocationupdate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;

public class Utils {
  static final String KEY_REQUESTING_LOCATION_UPDATES="request_location_update";


  //get location
  static boolean requestLocationUpdate(Context context){
    return PreferenceManager.getDefaultSharedPreferences(context)
      .getBoolean(KEY_REQUESTING_LOCATION_UPDATES,false);
  }

  //set location
  static void setKeyRequestingLocationUpdates(Context context,boolean requestLocationUpdate){
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit()
      .putBoolean(KEY_REQUESTING_LOCATION_UPDATES,requestLocationUpdate)
      .apply();
  }
  static String getLocationText(Location location){
    return location ==null ? "UnKnown location" :
      "(" +location.getLatitude()+","+location.getLongitude()+")";
  }
  @SuppressLint("StringFormatInvalid")
  static String getLocationTitle(Context context){
    return context.getString(R.string.location_updated,
      DateFormat.getDateTimeInstance().format(new Date()));
  }
}
