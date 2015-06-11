package aam.avnet.com.aam;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;


/**
 * Created by 914893 on 1/11/15.
 */
public class GcmIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    Intent resultIntent;


    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                showNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                showNotification("Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                for (int i=0; i<5; i++) {
                    // Log.i(TAG, "Working... " + (i+1)
                    //       + "/5 @ " + SystemClock.elapsedRealtime());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
                // Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.

                switch(extras.getString("title")){
                    case "Pickup Request":
                        showNotification(extras.getString("message"));
                        break;
                    case "Duration Request":
                        sendLocation(extras.getString("responseDeviceId"),extras.getString("tripId"));
                        break;
                    case "Duration Response":
                        updateCurrentLocation(extras.getString("tripId"), extras.getString("message"));
                        break;
                }

                //Log.i("Notification::::::::::::::", "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendLocation(String deviceId,String tripId) {
        //TODO Implement Timer
        try {
            //Toast.makeText(this, "Duration Request Sent.", Toast.LENGTH_SHORT).show();
            //Send Notification to driver
            NotificationData notData = new NotificationData();
            Log.d("response id",deviceId);
            /*
            notData.setDeviceId(deviceId);
            notData.setTripId(tripId);
            notData.setTitle("Duration Response");
            notData.setMessage(getRecentLocation());
            new SendNotification().execute(notData);
            */
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private String getRecentLocation(){
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        String locationMessage = "";
        if(location == null){
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location != null) {
                locationMessage = "GPS" + "-" + location.getLatitude() + "," + location.getLongitude();
            }
            if(location==null){
                locationMessage="GPS and Network Not Currently Available. Please Try Later";
            }
        }else{
            locationMessage += "NETWORK" + "-" + location.getLatitude() + "," + location.getLongitude();
        }

        return locationMessage;
    }


    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void showNotification(String msg) {

        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String displayNotificationsKey = "notifications_new_message";
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,true);

        if ( displayNotifications ){
            //New code
            resultIntent = new Intent(this, MainActivity.class);
            resultIntent.putExtra("source","Notification");
            resultIntent.setAction("com.example.Action");
            //Store extras
            getUserDataFromShared(this);

            TaskStackBuilder stackBuilderHourly = TaskStackBuilder.create(this);
            stackBuilderHourly.addParentStack(MainActivity.class);
            stackBuilderHourly.addNextIntent(resultIntent);

            PendingIntent contentIntent =
                    stackBuilderHourly.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle("Trip Request")
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(msg))
                            .setContentText(msg)
                            .setAutoCancel(true);

            mBuilder.setContentIntent(contentIntent);
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        }
    }

    public void updateCurrentLocation(String tripId,String message){
        String[] src = new String[2];
        String result = "";
        Boolean driverLocationError=false;
        Boolean currentLocationError=false;

        if(message.contains("GPS") && message.contains("-")){
            src = message.split("-");
        }else if(message.contains("NETWORK") && message.contains("-")){
            src = message.split("-");
        }else {
            driverLocationError=true;
        }
        Log.d("Duration Response ",message);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        String locationMessage = "";
        if(location == null){
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location != null) {
                locationMessage = "GPS" + "-" + location.getLatitude() + "," + location.getLongitude();
            }
        }else{
            locationMessage += "NETWORK" + "-" + location.getLatitude() + "," + location.getLongitude();
        }

        String[] dest = new String[2];
        if(location != null){
            if(locationMessage.contains("GPS") && locationMessage.contains("-")){
                dest = locationMessage.split("-");
            }else if(locationMessage.contains("NETWORK") && locationMessage.contains("-")){
                dest = locationMessage.split("-");
            }else {
                currentLocationError=true;
            }
        }else{
            currentLocationError = true;
        }


        if(currentLocationError) {
            result = "There is a problem in finding your location, try again";
            Log.d("Duration ", src[1] + "," + dest[1] + "======= " + result);
        }else if(driverLocationError){
            result = "Driver's location is not acessable. Please try Later";
            Log.d("Duration ", src[1] + "," + dest[1] + "======= " + result);
        }else{
            if(src[1] != null && dest[1] != null) {
                result = getDistanceInfo(src[1], dest[1]);
                Log.d("Duration ", src[1] + "," + dest[1] + "======= " + result);
            }
        }
        timeNotification(result);

    }

    public String getDistanceInfo(String sourceAddress, String destinationAddress) {
        StringBuilder stringBuilder = new StringBuilder();
        String dist = "";
        String dur = "";
        try {

            destinationAddress = destinationAddress.replaceAll(" ","%20");
            String url = "http://maps.googleapis.com/maps/api/directions/json?origin=" + sourceAddress + "&destination=" + destinationAddress + "&mode=driving&sensor=false";

            HttpPost httppost = new HttpPost(url);

            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            stringBuilder = new StringBuilder();


            response = client.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }

        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject = new JSONObject(stringBuilder.toString());

            JSONArray array = jsonObject.getJSONArray("routes");

            JSONObject routes = array.getJSONObject(0);

            JSONArray legs = routes.getJSONArray("legs");

            JSONObject steps = legs.getJSONObject(0);

            Log.d("JSON :",steps.toString());

            JSONObject distance = steps.getJSONObject("distance");
            JSONObject duration = steps.getJSONObject("duration");

            //Log.i("Distance/Duration", distance.toString()+"/"+duration.toString());
            dist = distance.getString("text");
            dur = duration.getString("text");
            Log.d("dur: ",dur);

            /*//Increment Duration by 10 min
            Double min = Double.parseDouble(duration.getString("text").replaceAll("[^\\.0123456789]", ""));
            String[] temp = dur.split(" ");
            Time time = Time.valueOf(temp[1]);
            int min = time.getMinutes();
            min += 10;
            time.setMinutes(min);
            temp[1] = time.toString();
            dur = "";
            for(String a : temp){
                dur += a;
            }*/

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return dist + " - " + dur;
    }


    public void timeNotification(String msg) {

        //Log.d("Message",msg);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String displayNotificationsKey = "notifications_new_message";
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,true);

        if ( displayNotifications ){
            //New code
            resultIntent = new Intent(this, MainActivity.class);
            resultIntent.putExtra("source","Time Notification");
            resultIntent.setAction("com.example.Action");
            //Store extras
            getUserDataFromShared(this);

            TaskStackBuilder stackBuilderHourly = TaskStackBuilder.create(this);
            stackBuilderHourly.addParentStack(MainActivity.class);
            stackBuilderHourly.addNextIntent(resultIntent);

            PendingIntent contentIntent =
                    stackBuilderHourly.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle("ETA")
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(msg))
                            .setContentText(msg).setAutoCancel(true);

            mBuilder.setContentIntent(contentIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        }
    }

    private void getUserDataFromShared(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        if(prefs != null){
            resultIntent.putExtra("personName",prefs.getString(MainActivity.PROPERTY_USER_NAME,""));

        }

    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
}

