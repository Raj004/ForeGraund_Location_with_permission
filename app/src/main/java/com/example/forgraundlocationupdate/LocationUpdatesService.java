package com.example.forgraundlocationupdate;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import static com.example.forgraundlocationupdate.MainActivity.MY_PERMISSIONS_REQUEST_LOCATION;

public class LocationUpdatesService extends Service {
  private static final String PACKAGE_NAME=
        "com.example.forgraundlocationupdate";
  private static final String TAG =LocationUpdatesService.class.getSimpleName();


  private static final String CHANNEL_ID = "channel_01";

  static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

  static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
  private static final String EXTRA_STARTED_FROM_NOTIFICATION=PACKAGE_NAME +
    ".started_from_notification";

  private final IBinder mBinder= new LocalBinder();

  private static final long UPDATE_INTERVAL_IN_MILLISECONDS=1000;

  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
    UPDATE_INTERVAL_IN_MILLISECONDS / 2;

  private static final int NOTIFICATION_ID = 12345678;

  private boolean mChangingConfiguration = false;
  private NotificationManager mNotificationManager;
  /**
   * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
   */
  private LocationRequest mLocationRequest;

  /**
   * Provides access to the Fused Location Provider API.
   */
  private FusedLocationProviderClient mFusedLocationClient;

  /**
   * Callback for changes in location.
   */
  private LocationCallback mLocationCallback;
  private Handler mServiceHandler;
  private Location mLocation;


  public LocationUpdatesService(){

  }
  @Override
  public void onCreate(){
    Log.e("Service ","Service");
    mFusedLocationClient= LocationServices.getFusedLocationProviderClient(this);
    mLocationCallback= new LocationCallback(){
      @Override
      public void onLocationResult(LocationResult locationResult){
        super.onLocationResult(locationResult);
        onNewLocation(locationResult.getLastLocation());
      }
    };
    createLocationRequest();
    getLastLocation();
    HandlerThread handlerThread= new HandlerThread(TAG);
    handlerThread.start();
    mServiceHandler= new Handler(handlerThread.getLooper());
    mNotificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = getString(R.string.app_name);
      // Create the channel for the notification
      NotificationChannel mChannel =
        new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

      // Set the Notification Channel for the Notification Manager.
      mNotificationManager.createNotificationChannel(mChannel);
    }
  }
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "Service started");
    boolean startedFromNotification=intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
      false);
    if(startedFromNotification){
      removeLocationUpdates();
      stopSelf();
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mChangingConfiguration = true;
  }
  private void createLocationRequest() {
    mLocationRequest = new LocationRequest();
    mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
    mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

  }




  @Override
  public IBinder onBind(Intent intent) {
    Log.i(TAG,"in onBind()");
    stopForeground(true);
    mChangingConfiguration=false;
    return mBinder;
  }



  @Override
  public void onRebind(Intent intent) {
    // Called when a client (MainActivity in case of this sample) returns to the foreground
    // and binds once again with this service. The service should cease to be a foreground
    // service when that happens.
    Log.i(TAG, "in onRebind()");
    stopForeground(true);
    mChangingConfiguration = false;
    super.onRebind(intent);
  }

  @Override
  public boolean onUnbind(Intent intent){
    Log.i(TAG,"Last cleint unbound from service");
    if(!mChangingConfiguration && Utils.requestLocationUpdate(this)){
      Log.i(TAG,"STARTING forground service");
      startForeground(NOTIFICATION_ID,getNotification());
    }
    return true;

  }


  @Override
  public void onDestroy() {
    mServiceHandler.removeCallbacksAndMessages(null);
  }
  public void requestLocationUpdate(){
    Log.i(TAG,"Requesting location updates");
    Utils.setKeyRequestingLocationUpdates(this,true);
    startService(new Intent(getApplicationContext(),LocationUpdatesService.class));
    try{
      mFusedLocationClient.requestLocationUpdates(mLocationRequest,
              mLocationCallback,Looper.myLooper());
    }catch (SecurityException unlikly){
      Utils.setKeyRequestingLocationUpdates(this,false);
      Log.e(TAG, "Lost location permission. Could not request updates. " + unlikly);
    }
  }
 public void removeLocationUpdates(){
    Log.i(TAG,"Removeing location updates");
    try {
      mFusedLocationClient.removeLocationUpdates(mLocationCallback);
      Utils.setKeyRequestingLocationUpdates(this,false);
      stopSelf();
    }catch (SecurityException unlikly){
      Utils.setKeyRequestingLocationUpdates(this,true);
      Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikly);
    }
 }
  private void getLastLocation() {
    try {
      mFusedLocationClient.getLastLocation()
              .addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                  if (task.isSuccessful() && task.getResult() != null) {
                    mLocation = task.getResult();
                  } else {
                    Log.w(TAG, "Failed to get location.");
                  }
                }
              });
    } catch (SecurityException unlikely) {
      Log.e(TAG, "Lost location permission." + unlikely);
    }
  }





  private void onNewLocation(Location location) {
    Log.i(TAG, "New location: " + location);

    mLocation = location;

    // Notify anyone listening for broadcasts about the new location.
    Intent intent = new Intent(ACTION_BROADCAST);
    intent.putExtra(EXTRA_LOCATION, location);
    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

    // Update notification content if running as a foreground service.
    if (serviceIsRunningInForeground(this)) {
      mNotificationManager.notify(NOTIFICATION_ID, getNotification());
    }
  }


  public boolean serviceIsRunningInForeground(Context context) {
    ActivityManager manager = (ActivityManager) context.getSystemService(
            Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
            Integer.MAX_VALUE)) {
      if (getClass().getName().equals(service.service.getClassName())) {
        if (service.foreground) {
          return true;
        }
      }
    }
    return false;
  }



  private Notification getNotification() {
    Intent intent = new Intent(this, LocationUpdatesService.class);

    CharSequence text = Utils.getLocationText(mLocation);

    // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
    intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

    // The PendingIntent that leads to a call to onStartCommand() in this service.
    PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT);

    // The PendingIntent to launch activity.
    PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class), 0);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
            .addAction(R.drawable.yes, getString(R.string.launch_activity),
                    activityPendingIntent)
            .addAction(R.drawable.images, getString(R.string.remove_notification),
                    servicePendingIntent)
            .setContentText(text)
            .setContentTitle(Utils.getLocationTitle(this))
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.yes)
            .setTicker(text)
            .setWhen(System.currentTimeMillis());

    // Set the Channel ID for Android O.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setChannelId(CHANNEL_ID); // Channel ID
    }

    return builder.build();
  }
  public class LocalBinder extends Binder {
    LocationUpdatesService getService() {
      return LocationUpdatesService.this;
    }
  }
}
