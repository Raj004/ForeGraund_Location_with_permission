package com.example.forgraundlocationupdate;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,ActivityCompat.OnRequestPermissionsResultCallback {
  private static final String TAG = MainActivity.class.getSimpleName();
  private MyReciver myReceiver;
  private LocationUpdatesService mService = null;
  private boolean mBound = false;
  Button start_btn, stop_btn;
  TextView tv_location;
  LocationRequest locationRequest;
  LocationManager locationManager;
  Context context;
  GoogleApiClient mGoogleApiClient;
  LocationSettingsRequest.Builder locationSettingsRequest;
  public static final int REQUEST_LOCATION = 001;

  PendingResult<LocationSettingsResult> pendingResult;


  public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

  private final ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {


      LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
      mService = binder.getService();
      mBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      mService = null;
      mBound = false;
    }
  };


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.e("Actvity ", "Actvity");

    myReceiver = new MyReciver();


    setContentView(R.layout.activity_main);
    context = this;

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//      Toast.makeText(this, "Gps is Enabled", Toast.LENGTH_SHORT).show();
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//        checkPermission1();
//      }
//    } else {
//     mEnableGps();
//    }
    tv_location = (TextView) findViewById(R.id.text1);
  }

  private void mEnableGps() {
    mGoogleApiClient = new GoogleApiClient.Builder(context)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
    mGoogleApiClient.connect();
    mLocationSetting();

  }

  private void mLocationSetting() {
    locationRequest = LocationRequest.create();
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    locationRequest.setInterval(1 * 1000);
    locationRequest.setFastestInterval(1 * 1000);

    locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

    mResult();

  }

  private void mResult() {
    pendingResult = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, locationSettingsRequest.build());
    pendingResult.setResultCallback(new ResultCallback<LocationSettingsResult>() {
      @Override
      public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        Status status = locationSettingsResult.getStatus();

        switch (status.getStatusCode()) {
          case LocationSettingsStatusCodes.SUCCESS:
            // All location settings are satisfied. The client can initialize location
            // requests here.
            try {
              Toast.makeText(MainActivity.this, "Yes-----", Toast.LENGTH_LONG).show();

              status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
            } catch (IntentSender.SendIntentException e) {
              e.printStackTrace();
            }

            Toast.makeText(MainActivity.this, "Yes-----", Toast.LENGTH_LONG).show();

            break;
          case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

            try {
              Toast.makeText(MainActivity.this, "n0000-----", Toast.LENGTH_SHORT).show();


              status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
            } catch (IntentSender.SendIntentException e) {

            }
            break;
          case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
            // Location settings are not satisfied. However, we have no way to fix the
            // settings so we won't show the dialog.
            break;
        }
      }

    });
  }

            //this permission use
/*  private void checkPermission1() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    ) {//Can add more as per requirement

      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
              REQUEST_LOCATION);

    }
  }*/
  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case REQUEST_LOCATION: {

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          setButtonsState(Utils.requestLocationUpdate(this));
          if(mService!=null){
          mService.requestLocationUpdate();
          }
          else
          {
            bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
                    Context.BIND_AUTO_CREATE);

            Toast.makeText(MainActivity.this, "Granted-----", Toast.LENGTH_SHORT).show();

          }
        } else {

          // permission denied, boo! Disable the
          Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
        }
        return;
      }
    }
  }
  public static boolean isLocationEnabled(Context context) {
    int locationMode = 0;
    String locationProviders;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
      try {
        locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

      } catch (Settings.SettingNotFoundException e) {
        e.printStackTrace();
        return false;
      }

      return locationMode != Settings.Secure.LOCATION_MODE_OFF;

    }else{
      locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
      return !TextUtils.isEmpty(locationProviders);
    }
  }
  @Override
  protected void onStart() {

    super.onStart();
    PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this);

    start_btn = (Button) findViewById(R.id.btn);
    stop_btn = (Button) findViewById(R.id.btn2);

    start_btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {//Can add more as per requirement

          ActivityCompat.requestPermissions(MainActivity.this,
                  new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                  REQUEST_LOCATION);

        }
        else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
          new AlertDialog.Builder(context)
                  .setTitle(R.string.gps_not_found_title)  // GPS not found
                  .setMessage(R.string.gps_not_found_message) // Want to enable?
                  .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                      MainActivity.this.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                  })
                  .setNegativeButton(R.string.no, null)
                  .show();

        } else {
          if (mService == null) {
            Toast.makeText(context, "Service is Not availble yet..", Toast.LENGTH_SHORT).show();
          } else {
            mService.requestLocationUpdate();
            setButtonsState(Utils.requestLocationUpdate(MainActivity.this));

            bindService(new Intent(MainActivity.this, LocationUpdatesService.class), mServiceConnection,
                    Context.BIND_AUTO_CREATE);
          }
        }
      }
    });

    stop_btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (mService == null) {
          Toast.makeText(context, "Service is Not availble yet..", Toast.LENGTH_SHORT).show();

        } else {
          mService.removeLocationUpdates();
        }
      }
    });

    // App ReStart Permission handle and setup button    //raj007
    // Restore the state of the buttons when the activity (re)launches.
    // Bind to the service. If the service is in foreground mode, this signals to the service
    // that since this activity is in the foreground, the service can exit foreground mode.
     if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    ) {//Can add more as per requirement

      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
              123);
     }
    else {
      setButtonsState(Utils.requestLocationUpdate(this));

      bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
              Context.BIND_AUTO_CREATE);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
            new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
  }

  @Override
  protected void onPause() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
    super.onPause();
  }

  @Override
  protected void onStop() {
    if (mBound) {
      unbindService(mServiceConnection);
      mBound = false;
    }
    PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this);
    super.onStop();
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {

  }

  @Override
  public void onConnectionSuspended(int i) {

  }
  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

  }
  //BroadcastReceiver
  private class MyReciver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      Location location=intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
      if(location!=null){
        Toast.makeText(MainActivity.this,Utils.getLocationText(location),
                Toast.LENGTH_SHORT).show();
        tv_location.setText(Utils.getLocationText(location));

      }
    }
  }
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String s){
    if(s.equals(Utils.KEY_REQUESTING_LOCATION_UPDATES)){
      setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_LOCATION_UPDATES,false));
    }

  }
  private void setButtonsState(boolean requestingLocalUpdate) {
    if(requestingLocalUpdate){
      start_btn.setEnabled(false);
      stop_btn.setEnabled(true);
    }else {
      start_btn.setEnabled(true);
      stop_btn.setEnabled(false);
      tv_location.setText("");
    }
  }
}