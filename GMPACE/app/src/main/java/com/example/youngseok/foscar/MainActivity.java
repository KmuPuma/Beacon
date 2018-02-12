package com.example.youngseok.foscar;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECORangingListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class MainActivity extends RecoActivity implements RECORangingListener {

    final int RECEIVE_MESSAGE = 1;
    private static final String TAG = "BLUETOOTH";

    public static final String RECO_UUID = "24DDF411-8CF1-440C-87CD-E368DAF9C93E";

    private static final UUID RASPBERRY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = "43:43:A1:12:1F:AC";

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;

    Handler h;

    public static final boolean DISCONTINUOUS_SCAN = false;
    public static final boolean SCAN_RECO_ONLY = true;
    public static final boolean ENABLE_BACKGROUND_RANGING_TIMEOUT = true;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private static final int REQUEST_LOCATION = 10;
    private static final int REQUEST_ENABLE_BT = 1;

    private Handler mHandler;

    private DirectionThread mDirectionThread;
    private ConnectedThread mConnectedThread;

    private RecoRangingListAdapter mRangingListAdapter;
    private ListView mRegionListView;

    private TextView directionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        directionTextView = (TextView) findViewById(R.id.directionTextView);

        mHandler = new Handler();

        mDirectionThread = new DirectionThread();
        mDirectionThread.start();

        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.i("MainActivity", "The location permission (ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION) is not granted.");
                this.requestLocationPermission();
            } else {
                Log.i("MainActivity", "The location permission (ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION) is already granted.");
            }
        }

        //mRecoManager will be created here. (Refer to the RECOActivity.onCreate())
        //mRecoManager 인스턴스는 여기서 생성됩니다. RECOActivity.onCreate() 메소드를 참고하세요.

        //Set RECORangingListener (Required)
        //RECORangingListener 를 설정합니다. (필수)
        mRecoManager.setRangingListener(this);

        /**
         * Bind RECOBeaconManager with RECOServiceConnectListener, which is implemented in RECOActivity
         * You SHOULD call this method to use monitoring/ranging methods successfully.
         * After binding, onServiceConenct() callback method is called.
         * So, please start monitoring/ranging AFTER the CALLBACK is called.
         *
         * RECOServiceConnectListener와 함께 RECOBeaconManager를 bind 합니다. RECOServiceConnectListener는 RECOActivity에 구현되어 있습니다.
         * monitoring 및 ranging 기능을 사용하기 위해서는, 이 메소드가 "반드시" 호출되어야 합니다.
         * bind후에, onServiceConnect() 콜백 메소드가 호출됩니다. 콜백 메소드 호출 이후 monitoring / ranging 작업을 수행하시기 바랍니다.
         */
        mRecoManager.bind(this);
    }

    private void requestLocationPermission() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mRangingListAdapter = new RecoRangingListAdapter(this);
        mRegionListView = (ListView)findViewById(R.id.list_ranging);
        mRegionListView.setAdapter(mRangingListAdapter);

        Log.d(TAG, "...onResume - try connect...");

        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();

        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "....Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new MainActivity.ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.stop(mRegions);
        this.unbind();
    }

    private void unbind() {
        try {
            mRecoManager.unbind();
        } catch (RemoteException e) {
            Log.i("MainActivity", "Remote Exception");
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceConnect() {
        Log.i("MainActivity", "onServiceConnect()");
        mRecoManager.setDiscontinuousScan(MainActivity.DISCONTINUOUS_SCAN);
        this.start(mRegions);
        //Write the code when RECOBeaconManager is bound to RECOBeaconService
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<RECOBeacon> recoBeacons, RECOBeaconRegion recoRegion) {
        Log.i("MainActivity", "didRangeBeaconsInRegion() region: " + recoRegion.getUniqueIdentifier() + ", number of beacons ranged: " + recoBeacons.size());
        mRangingListAdapter.updateAllBeacons(recoBeacons);
        mRangingListAdapter.notifyDataSetChanged();
        //Write the code when the beacons in the region is received
    }

    @Override
    protected void start(ArrayList<RECOBeaconRegion> regions) {

        /**
         * There is a known android bug that some android devices scan BLE devices only once. (link: http://code.google.com/p/android/issues/detail?id=65863)
         * To resolve the bug in our SDK, you can use setDiscontinuousScan() method of the RECOBeaconManager.
         * This method is to set whether the device scans BLE devices continuously or discontinuously.
         * The default is set as FALSE. Please set TRUE only for specific devices.
         *
         * mRecoManager.setDiscontinuousScan(true);
         */

        for(RECOBeaconRegion region : regions) {
            try {
                mRecoManager.startRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                Log.i("RECORangingActivity", "Remote Exception");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.i("RECORangingActivity", "Null Pointer Exception");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void stop(ArrayList<RECOBeaconRegion> regions) {
        for(RECOBeaconRegion region : regions) {
            try {
                mRecoManager.stopRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                Log.i("RECORangingActivity", "Remote Exception");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.i("RECORangingActivity", "Null Pointer Exception");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onServiceFail(RECOErrorCode errorCode) {
        //Write the code when the RECOBeaconService is failed.
        //See the RECOErrorCode in the documents.
        return;
    }

    @Override
    public void rangingBeaconsDidFailForRegion(RECOBeaconRegion region, RECOErrorCode errorCode) {
        Log.i("RECORangingActivity", "error code = " + errorCode);
        //Write the code when the RECOBeaconService is failed to range beacons in the region.
        //See the RECOErrorCode in the documents.
        return;
    }

    private void errorExit(String title, String message){
        Toast.makeText(this, title + " - " + message, Toast.LENGTH_LONG).show();
        this.finish();
    }

    class DirectionThread extends Thread {
        @Override
        public void run() {
            super.run();

            while(true) {
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(RecoRangingListAdapter.beaconLeft > RecoRangingListAdapter.beaconRight && RecoRangingListAdapter.beaconLeft > RecoRangingListAdapter.beaconFront) {
                            directionTextView.setText("LEFT");
                            mConnectedThread.write("LEFT....");
                        } else if(RecoRangingListAdapter.beaconRight > RecoRangingListAdapter.beaconLeft && RecoRangingListAdapter.beaconRight > RecoRangingListAdapter.beaconFront) {
                            directionTextView.setText("RIGHT");
                            mConnectedThread.write("RIGHT...");
                        } else if(RecoRangingListAdapter.beaconFront > RecoRangingListAdapter.beaconLeft && RecoRangingListAdapter.beaconFront > RecoRangingListAdapter.beaconRight) {
                            directionTextView.setText("STRAIGHT");
                            mConnectedThread.write("STRAIGHT");
                        }

//                        mConnectedThread.write("0");
                    }
                });
            }
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, RASPBERRY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(RASPBERRY_UUID);
    }

    private void checkBTState() {
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    public class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
//                    h.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }

}
