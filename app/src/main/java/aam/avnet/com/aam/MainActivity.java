package aam.avnet.com.aam;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import aam.avnet.com.aam.Background.RegisterUserTask;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    /**
     * A flag indicating that a PendingIntent is in progress and prevents us
     * from starting further intents.
     */

    private Button btnSignIn;
    private TextView userId,vehmodel,vehman,yof,yor;
    private TextView txtName, txtEmail;

    private String personName,personVehcileMan,personVehicleModel;

    /* GCM */
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    public static final String PROPERTY_USER_NAME = "user_name";
    public static final String PROPERTY_VEHICLE_MODEL = "vehicle_model";
    public static final String PROPERTY_VEHICLE_MAN = "vehicle_man";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private ProgressDialog dialog;

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "731411698158";

    /**
     * Tag used on log messages.
     */
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;
    String regid;
    boolean notRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_vehicle);

        btnSignIn = (Button) findViewById(R.id.btn_sign_in);
        userId = (TextView) findViewById(R.id.userId);
        vehmodel = (TextView) findViewById(R.id.vehicleModel);
        vehman = (TextView) findViewById(R.id.vehicleModel);

        this.getSupportActionBar().hide();
        // Button click listeners
        btnSignIn.setOnClickListener(this);
        context = getApplicationContext();
        //Get user details from preferences if empty this the first time user is coming to app.
        loadUserDetailsFromPreferences();

        if(personName.isEmpty()){
            notRegistered = true;
        }else{
            notRegistered = false;
            regid = getRegistrationId(context);
            signInWithCorp();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(dialog!= null)
            dialog.dismiss();
    }

    protected void onStart() {
        super.onStart();
        //TODO connect();
    }

    protected void onStop() {
        super.onStop();
        if(dialog!= null)
            dialog.dismiss();
    }
    //TODO onConnected() { getProfileInformation();}

    /**
     * Button on click listener
     * */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_sign_in:
                // Signin button clicked
                if(userId.getText().toString().length() == 0 ||
                   vehman.getText().toString().length() == 0 ||
                   vehmodel.getText().toString().length() == 0){

                    Toast.makeText(this, "All Details are Necessary", Toast.LENGTH_SHORT).show();
                    return;
                }
                signInWithCorp();
                break;
        }
    }

    /**
     * Connecting to LDAP and Fetching user's information name, email, profile pic
     * */
    private void signInWithCorp() {

        final Intent intent = new Intent(this, GyroCarCapture.class);

        final Activity activity = this;

        if(notRegistered){

                if (checkPlayServices()) {
                    gcm = GoogleCloudMessaging.getInstance(activity);
                    regid = getRegistrationId(context);
                    Log.d("RegId",regid);
                    if (regid.isEmpty()) {
                        registerInBackground();
                    }
                }
                personName = userId.getText().toString();
                personVehcileMan = vehman.getText().toString();
                personVehicleModel = vehmodel.getText().toString();

                intent.putExtra("personAccountId",personName);
                intent.putExtra("personVehicleMan",personVehcileMan);
                intent.putExtra("personVehicleModel",personVehicleModel);
                intent.putExtra("regId",regid);
                startActivity(intent);
                finish();
        }
        else{
            intent.putExtra("personName",personName);
            intent.putExtra("regId",regid);
            startActivity(intent);
            finish();
        }
    }

    /* Registration */

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        /*
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        */
        return registrationId;
    }

    private void loadUserDetailsFromPreferences(){

        final SharedPreferences prefs = getGCMPreferences(context);
        personName = prefs.getString(PROPERTY_USER_NAME,"");
        personVehcileMan = prefs.getString(PROPERTY_VEHICLE_MODEL,"");
        personVehcileMan = prefs.getString(PROPERTY_VEHICLE_MAN,"");
    }
    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /*
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

*/
    private void registerInBackground() {
        new AsyncTask<Void,Void,String>() {

            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    storeOrUpdateUserData(context);
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.d("GCM",msg);
            }
        }.execute(null, null, null);

    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        //int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        //editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private void storeOrUpdateUserData(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_USER_NAME,personName);
        editor.putString(PROPERTY_VEHICLE_MAN,personName);
        editor.putString(PROPERTY_VEHICLE_MODEL,personName);
        editor.commit();
    }

    private void sendRegistrationIdToBackend() {
        // Your implementation here.
        UserProfile userData = new UserProfile();
        userData.setUsername(personName);
        userData.setDeviceId(regid);

        new RegisterUserTask().execute(userData);
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
