package mobilecomputing.kaist.com.sl;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import mobilecomputing.kaist.com.sl.allani.alexandre.playmusic.BeatDetection.Spectralflux;

public class HostActivity extends Activity {


    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";

    int REQUEST_CODE = 1;
    boolean flashLightStatus;
    List<Double> tolight = new ArrayList<Double>();
    int count;
    CameraManager cameraManager;
    String cameraId;


    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private MyHostBluetoothService myHostBluetoothService;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;

    private TextView status;
    private BluetoothAdapter bluetoothAdapter;

    private Button sendButton;
    private byte[] audioBytes;

    IntentFilter filter;

    private MediaPlayer song;
    private BufferedInputStream in;
    private File songFile;
    private String fileName="song.wav";
    protected static final int sendBufferSize=1024;
    private int fileLength;


    @Override@TargetApi(21)
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);
        enableBluetooth();
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }





        sendButton=findViewById(R.id.sendButton);
        status=findViewById(R.id.status);

        songFile=new File(Environment.getExternalStorageDirectory(),fileName);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA},  REQUEST_CODE);
            return;
        }


        //Converting now the wav file to a byte array

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            in = new BufferedInputStream(new FileInputStream(songFile));
        }
        catch (FileNotFoundException e){
            Log.e("File not found",e.getMessage());
        }
        int read;
        byte[] buff = new byte[1024];
        try {
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
            out.flush();
            audioBytes = out.toByteArray();

        }
        catch (IOException e){
            Log.e("Song conversion error",e.getMessage());
        }
        Log.e("byte array creation","done");

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //int i=0;

                byte[] header=new byte[512];
                for(byte k=0;k<10;k++){
                    header[k]=k;
                }
                fileLength=audioBytes.length;
                int numberOfPackets=(fileLength/1024)+1;
                int remaining=numberOfPackets;
                header[10]=(byte)((numberOfPackets/16348));
                remaining=numberOfPackets%16284;
                header[11]=(byte)(remaining/128);
                remaining=(byte)(remaining%128);
                header[12]=(byte)remaining;
                myHostBluetoothService.write(header);
                myHostBluetoothService.write(audioBytes);
                Toast.makeText(getApplicationContext(),"Done sending",Toast.LENGTH_LONG).show();

                }

        });
    }


    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case MyJoinBluetoothService.STATE_CONNECTED:
                            setStatus("Connected to: " + connectingDevice.getName());
                            break;
                        case MyJoinBluetoothService.STATE_CONNECTING:
                            setStatus("Connecting...");
                            break;
                        case MyJoinBluetoothService.STATE_LISTEN:
                            setStatus("Listening");
                            break;
                        case MyJoinBluetoothService.STATE_NONE:
                            setStatus("Not connected");
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    URI songURI=songFile.toURI();
                    Uri songUri=Uri.parse(songURI.toString());
                    song=MediaPlayer.create(getApplicationContext(),songUri);


                    Spectralflux nf = new Spectralflux(songFile);
                    try {
                        tolight = nf.getBeats();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    new AsyncTaskRunner().execute("c", "c", "c");


                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });
    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            myHostBluetoothService = new MyHostBluetoothService(this, handler);
        }
    }



    @Override
    public void onResume() {
        super.onResume();

        if (myHostBluetoothService != null) {
            if (myHostBluetoothService.getState() == MyHostBluetoothService.STATE_NONE) {
                myHostBluetoothService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (myHostBluetoothService != null)
            myHostBluetoothService.stop();
        unregisterReceiver(discoveryFinishReceiver);
    }

    private void enableBluetooth() {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        //Initializing bluetooth adapters
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);


        // Register for broadcasts when a device is discovered
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter);


        }

    private void setStatus(String s) {
        status.setText(s);
    }
    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add(getString(R.string.none_found));
                }
            }
        }
    };



    @Override
    public void onBackPressed(){
        finish();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d("Debug","Permission Granted");
            }else{
                Log.d("Debug","Permission Denied");
            }
        }
    }



    public class AsyncTaskRunner extends AsyncTask<String,String,String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @TargetApi(23)
        private void flashLightOn() {

            try {
                cameraManager.setTorchMode(cameraId, true);
                flashLightStatus = true;
            } catch (CameraAccessException e) {
            }
        }

        @TargetApi(23)
        private void flashLightOff() {
            try {
                cameraManager.setTorchMode(cameraId, false);
                flashLightStatus = false;
            } catch (CameraAccessException e) {
            }
        }


        @Nullable
        @Override
        protected final String doInBackground(String... strings) {

            Process.setThreadPriority(-20);
            long curTime;
            long time_to_sleep=0;
            count =0;
            song.start();
            long startTime =System.nanoTime();
            long atime;
            long toadd=0;
            long y =0;
            while (song.isPlaying()){
                curTime = System.nanoTime();

                if(curTime -startTime>20000000){
                    atime = System.nanoTime();
                    toadd = 1550;
                    if(tolight.get(count) == (double)1){
                        flashLightOn();
                        Log.d("Debug","count FLASH "+count);
                        Log.d("Debug",""+tolight.size());

                    }else{
                        flashLightOff();
                    }
                    count++;

                    startTime = System.nanoTime();
                    time_to_sleep = System.nanoTime() - 2*toadd;

                }

            }
            Log.d("Debug","count FLASH "+count);
            return null;
        }
    }
}
