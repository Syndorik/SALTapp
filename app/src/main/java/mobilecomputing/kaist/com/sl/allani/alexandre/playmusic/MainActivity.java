package mobilecomputing.kaist.com.sl.allani.alexandre.playmusic;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


import mobilecomputing.kaist.com.sl.R;
import mobilecomputing.kaist.com.sl.allani.alexandre.playmusic.BeatDetection.Spectralflux;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;
import android.os.Process;

public class MainActivity extends AppCompatActivity {
    /*
    Button mButton;
    Button mButton2;
    Button mButton3;
    Button mButton4;
    MediaPlayer mediaPlayer;
    int REQUEST_CODE = 1;
    boolean flashLightStatus;
    List<Double> tolight = new ArrayList<Double>();
    Handler mHandler;
    int count;
    CameraManager cameraManager;
    String cameraId;


    @Override @TargetApi(21)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
             cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


        mButton = findViewById(R.id.sendButton);
        mButton2 = findViewById(R.id.sendButton);
        mButton3 = findViewById(R.id.sendButton);
        mButton4 = findViewById(R.id.sendButton);
        mHandler = new Handler();
/*
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
/*
        Spectralflux nf = new Spectralflux(R.raw.cddc,getApplicationContext());



        tolight = nf.getBeats();

        mediaPlayer = MediaPlayer.create(getApplicationContext(),R.raw.cddc);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTaskRunner().execute("c", "c", "c");
            }
        });

        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.reset();
                mediaPlayer = MediaPlayer.create(getApplicationContext(),R.raw.cddc);
            }
        });

        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.pause();
            }
        });

        mButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(flashLightStatus){
                    //flashLightOff();
                }else{
                    //flashLightOn();
                }

            }
        });
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



    public class AsyncTaskRunner extends AsyncTask<String,String,String>{
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

        /*
        if(tolight.get(count) == (double)1){
                        flashLightOn();
                    }else{
                        flashLightOff();
                    }
                    count++;
         */
        /*
        * curTime = System.nanoTime();
                if(curTime -startTime>22000000-time_to_sleep){
                    atime = System.nanoTime();
                    if(tolight.get(count) == (double)1){
                        flashLightOn();
                        //Log.d("Debug","count FLASH "+count);

                    }else{
                        flashLightOff();
                        //Log.d("Debug","count "+count);
                    }
                    count++;
                    time_to_sleep = System.nanoTime() - atime;
                    startTime = System.nanoTime();
                    Log.d("Debug",""+ time_to_sleep);
                }
        * */
/*


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
            while (mediaPlayer.isPlaying()){
                curTime = System.nanoTime();
                toadd += 1950;
                if(curTime -startTime>22600000-time_to_sleep-toadd){
                    atime = System.nanoTime();
                    toadd = 1550;
                    if(tolight.get(count) == (double)1){
                        flashLightOn();
                        Log.d("Debug","count FLASH "+count);

                    }else{
                        flashLightOff();
                    }
                    count++;
                    if (count > 2500 && count<2502) {
                        toadd = 22600000;
                    }
                    startTime = System.nanoTime();
                    time_to_sleep = System.nanoTime() - 2*atime;

                }

            }
            Log.d("Debug","count FLASH "+count);
            return null;
        }
    }

*/


}
