package mobilecomputing.kaist.com.sl;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mobilecomputing.kaist.com.sl.allani.alexandre.playmusic.BeatDetection.Spectralflux;

public class JoinActivity extends Activity {


    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";


    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private MyJoinBluetoothService myJoinBluetoothService;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;

    private TextView status;
    private TextView readMessageView;
    private String readMessage;

    private Button connectButton;
    private ListView listView;
    private Dialog dialog;
    private BluetoothAdapter bluetoothAdapter;
    private FileDescriptor songFileDescriptor;
    private File file;
    private FileOutputStream outputStream;
    private byte[] emptyByteArray = new byte[0];
    private String fileName = "song.wav";
    private byte[] readBuf, realReadBuf, lastReadBuf;
    private int sendBufferSize = HostActivity.sendBufferSize;
    private byte bufferState;
    private boolean isTailByteArray;
    private int lastBufferSize;
    private MediaPlayer mediaPlayer;
    private boolean isHeader,isTempHeader;
    private int packetCounter=-1,numberOfPackets=0;
    URI songURI;
    Uri songUri;

    int REQUEST_CODE = 1;
    boolean flashLightStatus;
    List<Double> tolight = new ArrayList<Double>();
    Handler mHandler;
    int count;
    CameraManager cameraManager;
    String cameraId;



    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case MyJoinBluetoothService.STATE_CONNECTED:
                            setStatus("Connected to: " + connectingDevice.getName());
                            connectButton.setEnabled(false);
                            break;
                        case MyJoinBluetoothService.STATE_CONNECTING:
                            setStatus("Connecting...");
                            connectButton.setEnabled(false);
                            break;
                        case MyJoinBluetoothService.STATE_LISTEN:
                            setStatus("Listening");
                            break;
                        case MyJoinBluetoothService.STATE_NONE:
                            setStatus("Not connected");
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    //Here we read the received byte array
                    readBuf = (byte[]) msg.obj;
                    Log.e("aaa","aa "+packetCounter +" "+numberOfPackets);
                    if (packetCounter == -1) {
                        isTempHeader=true;
                        for (byte i = 0; i < 10; i++) {
                            if (readBuf[i] != i) {
                                isTempHeader = false;
                                break;
                            }
                        }
                        if(isTempHeader){isHeader=true;}
                    }
                    if(isHeader) {
                        numberOfPackets = readBuf[10] * 16384 + readBuf[11] * 128 + readBuf[12];
                        packetCounter=0;
                        isHeader=false;
                    }
                    else {
                        if (packetCounter < numberOfPackets - 1) {
                            writeToFile(outputStream, readBuf);
                            packetCounter++;
                        } else if (packetCounter == numberOfPackets - 1) {
                            packetCounter=-1;
                            writeToFile(outputStream, readBuf);
                            songURI = file.toURI();
                            songUri = Uri.parse(songURI.toString());
                            mediaPlayer = MediaPlayer.create(getApplicationContext(), songUri);
                            myJoinBluetoothService.write(new byte[]{1, 2, 3});
                            Spectralflux nf = new Spectralflux(file);
                            try {
                                tolight = nf.getBeats();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            new AsyncTaskRunner().execute("c", "c", "c");
                        }
                    }

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

    @Override@TargetApi(21)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_join);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

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

        bufferState = 0;

        //Creating the file
        createFile(emptyByteArray, fileName);

        findViewsByIds();
        connectButton = findViewById(R.id.connectButton);
        readMessageView = findViewById(R.id.readMessageView);


        //check device support bluetooth or not
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            finish();
        }

        //show bluetooth devices dialog when click connect button
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPrinterPickDialog();
            }
        });

        try {
            outputStream = new FileOutputStream("/storage/emulated/0/" + fileName, true);
        } catch (FileNotFoundException e) {
            Log.e("appending to file", e.getMessage());
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            myJoinBluetoothService = new MyJoinBluetoothService(this, handler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (myJoinBluetoothService != null) {
            if (myJoinBluetoothService.getState() == MyJoinBluetoothService.STATE_NONE) {
                myJoinBluetoothService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (myJoinBluetoothService != null)
            myJoinBluetoothService.stop();
        try {
            outputStream.close();
        } catch (IOException e) {
            Log.e("closing stream error", e.getMessage());
        }
    }

    private void showPrinterPickDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_bluetooth);
        dialog.setTitle("Bluetooth Devices");

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        //Initializing bluetooth adapters
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        //locate listviews and attach the adapters
        ListView listView = dialog.findViewById(R.id.pairedDeviceList);
        ListView listView2 = dialog.findViewById(R.id.discoveredDeviceList);
        listView.setAdapter(pairedDevicesAdapter);
        listView2.setAdapter(discoveredDevicesAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add(getString(R.string.none_paired));
        }

        //Handling listview item click event
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }

        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void setStatus(String s) {
        status.setText(s);
    }


    private void connectToDevice(String deviceAddress) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        myJoinBluetoothService.connect(device);
    }

    private void findViewsByIds() {
        status = findViewById(R.id.status);
        connectButton = findViewById(R.id.connectButton);
        listView = findViewById(R.id.list);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void createFile(byte[] bytesToWrite, String mFileName) {
        file = new File(Environment.getExternalStorageDirectory(), mFileName);
        file.delete();
        file = new File(Environment.getExternalStorageDirectory(), mFileName);
        Log.e("log path", file.getAbsolutePath());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            // Writes bytes from the specified byte array to this file output stream
            fos.write(bytesToWrite);
        } catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while writing file " + ioe);
        } finally {
            // close the streams using close method
            try {
                fos.close();
            } catch (IOException ioe) {
                System.out.println("Error while closing stream: " + ioe);
            }
        }

    }

    private void writeToFile(FileOutputStream mOutputStream, byte[] buffer) {
        try {
            mOutputStream.write(buffer);
        } catch (IOException e) {
            Log.e("appending to file", e.getMessage());
        } finally {
            try {
                mOutputStream.flush();
            } catch (IOException e) {
                Log.e("Cannot flush stream", e.getMessage());
            }
        }
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
            mediaPlayer.start();
            long startTime =System.nanoTime();
            long atime;
            long toadd=0;
            long y =0;
            try {
                while (mediaPlayer.isPlaying()) {
                    curTime = System.nanoTime();

                    if (curTime - startTime > 20000000) {
                        atime = System.nanoTime();
                        toadd = 1550;
                        if (tolight.get(count) == (double) 1) {
                            flashLightOn();
                            Log.d("Debug", "count FLASH " + count);
                            Log.d("Debug", "" + tolight.size());

                        } else {
                            flashLightOff();
                        }
                        count++;

                        startTime = System.nanoTime();
                        time_to_sleep = System.nanoTime() - 2 * toadd;

                    }

                }
            }
            catch (Exception e){
                Log.e("Debug","Too soon");
            }
            Log.d("Debug","count FLASH "+count);
            return null;
        }
    }




}