package aam.avnet.com.aam;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

public class GyroCarCapture extends ActionBarActivity
{
    public static final boolean CREATE_ELMCOMMANDFILE = false;

    private static final String LOG_TAG = "GYROCARCAPTURE";
    private static final String SAMPLING_SERVICE_ACTIVATED_KEY = "samplingServiceActivated";
    private static final String STEPCOUNT_KEY = "sampleCounter";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private static final int MENU_BTCONNECTSECURE = 1;
    private static final int MENU_BTCONNECTINSECURE = 2;

    //Graph
    private LineChart mChart;

    enum BTConversationState {
        IDLE,
        INIT_SEQUENCE,
        SPEED_QUERY,
        DISCONNECTED
    };

    public void sendData(View v){
        sendMessage(command.getText().toString());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        uiHandler = new Handler();
        Log.d( LOG_TAG, "onCreate" );
        setContentView( R.layout.main );
        SensorManager sensorManager =
                (SensorManager)getSystemService( SENSOR_SERVICE  );

        sampleCounterTV = (TextView)findViewById( R.id.samplecounter );
        speedTV = (TextView)findViewById( R.id.speed );
        command = (EditText)findViewById(R.id.command);
        lastRequestTV = (TextView)findViewById( R.id.lastrequest );
        lastResponseTV = (TextView)findViewById( R.id.lastresponse );
        if( sampleCounterText != null ) {
            sampleCounterTV.setText( sampleCounterText );
            sampleCounterTV.setVisibility( View.VISIBLE );
        }

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mChart = (LineChart) findViewById(R.id.chart1);
        GraphUtil util = new GraphUtil(mChart);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d( LOG_TAG, "onStart");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupBT();
        }
        openElmCommandFile();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onStart");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
        openElmCommandFile();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
        closeElmCommandFile();
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState( outState );
        Log.d( LOG_TAG, "onSaveInstanceState" );
        outState.putBoolean( SAMPLING_SERVICE_ACTIVATED_KEY, samplingServiceActivated );
        if( sampleCounterText != null )
            outState.putString( STEPCOUNT_KEY, sampleCounterText );
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d( LOG_TAG, "onDestroy" );
        if (mChatService != null) mChatService.stop();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupBT();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(LOG_TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_BTCONNECTSECURE, 1, R.string.secure_connect );
        menu.add(0, MENU_BTCONNECTINSECURE, 1, R.string.insecure_connect );
        return result;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch( id ) {
            case MENU_BTCONNECTSECURE: {
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
            break;


            case MENU_BTCONNECTINSECURE: {
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            }
            break;
        }
        return true;
    }

    private void openElmCommandFile() {
        if( ( elmCommandFile == null ) && CREATE_ELMCOMMANDFILE ) {
            File elmCommandFileName = new File(
                    Environment.getExternalStorageDirectory(),
                    "elmcommands.txt" );
            try {
                elmCommandFile = new PrintWriter( new FileWriter( elmCommandFileName, true ) );
            } catch( IOException ex ) {
                Log.e( LOG_TAG, ex.getMessage(), ex );
            }
        }
    }

    private void closeElmCommandFile() {
        if( elmCommandFile != null ) {
            elmCommandFile.close();
            elmCommandFile = null;
        }
    }


    private void setupBT() {
        Log.d(LOG_TAG, "setupBT");


        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
        inBuf = new StringBuffer();
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        btConversationState = BTConversationState.IDLE;
        mChatService.connect(device, secure);
    }

    private void startELM327InitSequence() {
        currentSequence = ELM327InitSequence;
        currentSequenceIndex = 0;
        btConversationState = BTConversationState.INIT_SEQUENCE;
        sendMessage( currentSequence[currentSequenceIndex]);
    }

    private boolean sendNextSequenceElement() {
        ++currentSequenceIndex;
        if( currentSequenceIndex >= currentSequence.length)
            return false;
        sendMessage( currentSequence[currentSequenceIndex] );
        return true;
    }

    private void sendSpeedQuery() {
        btConversationState = BTConversationState.SPEED_QUERY;
        // String speedQuery = "01 0C 0D 05 1F 10 61"; //rpm, speed, coolant temp, engine run time, MAF, torque"
        String speedQuery = "010C0D051F"; //rpm, speed, coolant temp, engine run time, MAF, torque"
        sendMessage( speedQuery );
    }

    private void scheduleSpeedQuery() {
        /*uiHandler.postDelayed( new Runnable() {
            public void run() {
                sendSpeedQuery();
            }
        }, 1000L);*/
    }

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        if( actionBar != null )
            actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        if( actionBar != null )
            actionBar.setSubtitle(subTitle);
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        if( elmCommandFile != null )
            elmCommandFile.println( ">>> "+message );
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            Log.d(LOG_TAG, "sending message: '"+message+"'");
            lastRequestTV.setText(message);
            // Get the message bytes and tell the BluetoothChatService to write
            String msgcr = message + (char)0x000D;
            byte[] send = msgcr.getBytes();
            mChatService.write(send);
        }
    }

    private void processIncomingMessage( String message ) {
        Log.d( LOG_TAG, "Incoming message: '"+message+"'");
        if( elmCommandFile != null )
            elmCommandFile.println( "<<< "+message);
        switch( btConversationState ) {
            case DISCONNECTED:
                Log.d( LOG_TAG, "Dropping message in DISCONNECTED state" );
                break;

            case INIT_SEQUENCE:
                if( !sendNextSequenceElement())
                    sendSpeedQuery();
                break;

            case SPEED_QUERY:
                Log.d(LOG_TAG, "speed query result: "+message);
                updateSpeed(message);
                btConversationState = BTConversationState.IDLE;
                scheduleSpeedQuery();
                break;

            default:
                Log.d( LOG_TAG, "Unhandled message: "+btConversationState.toString());
                break;
        }

    }

    private void updateSpeed( String message ) {
        Log.e("Speed", message);
        speedTV.setText( message.substring(4));
        // rpm 010C
        // speed 010D
        if( message.startsWith( "410D")) {
            String rem = message.substring(4);
            StringTokenizer st = new StringTokenizer( rem );
            try {
                String sps = st.nextToken();
                //int speed = Integer.parseInt(sps,16);
                speedTV.setText( sps);
            } catch( NoSuchElementException ex ) {
                Log.e(LOG_TAG, "updateSpeed: empty rem");
            } catch( NumberFormatException ex ) {
                Log.e(LOG_TAG, "updateSpeed: invalid number: "+rem);
            }
        }
    }

    private void setData(int count, float range) {

        ArrayList<String> xVals = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            xVals.add((i) + "");
        }

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        for (int i = 0; i < count; i++) {
            float mult = range / 2f;
            float val = (float) (Math.random() * mult) + 50;// + (float)
            // ((mult *
            // 0.1) / 10);
            yVals1.add(new Entry(val, i));
        }

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(yVals1, "DataSet 1");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setCircleColor(Color.WHITE);
        set1.setLineWidth(2f);
        set1.setCircleSize(3f);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setDrawCircleHole(false);
//        set1.setCircleHoleColor(Color.WHITE);

        ArrayList<Entry> yVals2 = new ArrayList<Entry>();

        for (int i = 0; i < count; i++) {
            float mult = range;
            float val = (float) (Math.random() * mult) + 450;// + (float)
            // ((mult *
            // 0.1) / 10);
            yVals2.add(new Entry(val, i));
        }

        // create a dataset and give it a type
        LineDataSet set2 = new LineDataSet(yVals2, "DataSet 2");
        set2.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set2.setColor(Color.RED);
        set2.setCircleColor(Color.WHITE);
        set2.setLineWidth(2f);
        set2.setCircleSize(3f);
        set2.setFillAlpha(65);
        set2.setFillColor(Color.RED);
        set2.setDrawCircleHole(false);
        set2.setHighLightColor(Color.rgb(244, 117, 117));

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(set2);
        dataSets.add(set1); // add the datasets

        // create a data object with the datasets
        LineData data = new LineData(xVals, dataSets);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);

        // set data
        mChart.setData(data);
    }


    private PrintWriter elmCommandFile = null;
    private boolean samplingServiceRunning = false;
    private boolean samplingServiceActivated = false;
    private TextView gpsStatusTV;
    private TextView sampleCounterTV;
    private TextView speedTV;
    private EditText command;
    private TextView lastRequestTV;
    private TextView lastResponseTV;
    private String sampleCounterText = null;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    private BTConversationState btConversationState;
    private String[] currentSequence = null;
    private int currentSequenceIndex = -1;
    private Handler uiHandler;
    private StringBuffer inBuf;

    private static final String ELM327InitSequence[] = {
            "ATD",
            "ATZ",
            "ATE0",
            "ATE0",
            "ATM0",
            "ATL0",
            "ATH0",
            "ATS0",
            "ATSP0",
            "0100"
    };


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    Log.d(LOG_TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;

                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;

                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            btConversationState = BTConversationState.DISCONNECTED;
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;

                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    inBuf.append( readMessage );
                    if( ( inBuf.length() > 0 ) &&
                            ( inBuf.charAt( inBuf.length() - 1 ) == '>') ) {
                        String resp = new String( inBuf).trim();
                        lastResponseTV.setText( resp );
                        processIncomingMessage( resp );
                        inBuf = new StringBuffer();
                    }
                    break;

                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
// BluetoothChatService went to connected state, we can start initializing the ELM327
                    uiHandler.postDelayed(
                            new Runnable() {
                                public void run() {
                                    startELM327InitSequence();
                                }
                            }, 2000);
                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


}
