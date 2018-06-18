package mobilecomputing.kaist.com.sl.allani.alexandre.playmusic.BeatDetection;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import mobilecomputing.kaist.com.sl.R;
import mobilecomputing.kaist.com.sl.allani.alexandre.playmusic.BeatDetection.io.WavfileDecod;


/**
 * Created by alexa on 01/06/2018.
 */

public class Spectralflux {
    private static final int THRESHOLD_WINDOW_SIZE = 7;
    private static final double MULTIPLIER = 1.9f;

    private File file;
    private List<Double> spectralFlux = new ArrayList<Double>();
    private List <Double> threshold = new ArrayList<Double>();
    private List <Double> prunned = new ArrayList<Double>();
    private List <Double> peaks = new ArrayList<Double>();
    private List <Double> onAndOff = new ArrayList<Double>();

    public Spectralflux(File file) {
        this.file = file;
    }

    public List<Double> getBeats() throws FileNotFoundException {


        WavfileDecod myWav = new WavfileDecod(file);

        double[][] spectrogram = myWav.getWav().getSpectrogram(2048,0).getAbsoluteSpectrogramData();

        flux(spectralFlux, spectrogram);
        threshold(threshold,spectralFlux);
        compare(threshold,spectralFlux,prunned);
        peaklist(peaks, prunned);
        beats(peaks,onAndOff);

        return onAndOff;
    }

    public static void beats(List<Double> peaks, List<Double> onAndOff) {
        onAndOff.add((double) 0);
        for(int k=0; k<peaks.size()-1; k++) {
            if(peaks.get(k) >0) {
                onAndOff.add((double) 1);
            }
            else {
                onAndOff.add((double) 0);
            }
        }

    }

    public static void peaklist(List<Double> peaks, List<Double> prunned) {
        peaks.add((double)0);
        for(int l =0; l<prunned.size()-1; l++) {
            if(prunned.get(l)>prunned.get(l+1)) {
                peaks.add(prunned.get(l));
            }
            else {
                peaks.add((double) 0);
            }
        }
    }

    public static void compare(List<Double> threshold, List<Double> spectralFlux, List<Double> prunned) {
        boolean bool = true;
        double previous =0;

        for( int i = 0; i < threshold.size(); i++ )
        {
            if( (threshold.get(i) <= spectralFlux.get(i)) ) {
                if(bool) {
                    prunned.add(previous);
                }
                else {
                    prunned.add( spectralFlux.get(i) - threshold.get(i) );
                    previous = spectralFlux.get(i) - threshold.get(i);
                    bool = true;
                }
            }

            else {
                prunned.add( (double)0 );
                bool = false;
            }
        }

    }

    public static void threshold(List<Double> threshold, List<Double> spectralFlux) {
        for( int i = 0; i < spectralFlux.size(); i++ )
        {
            int start = Math.max( 0, i - THRESHOLD_WINDOW_SIZE );
            int end = Math.min( spectralFlux.size() - 1, i + THRESHOLD_WINDOW_SIZE );
            double mean = 0;
            for( int j = start; j <= end; j++ )
                mean += spectralFlux.get(j);
            mean /= (end - start);
            threshold.add( mean * MULTIPLIER );
        }
    }

    public static void flux(List<Double> spectralFlux, double[][] spectrogram) {
        int len = spectrogram[0].length;
        int dlen = spectrogram.length;
        double flux =0;
        double value = 0;
        for(int k =0; k <dlen; k+=1) { //k+=2 cause we want less point
            flux = 0;
            value =0;
            for(int i =0; i<len; i++) {
                if(k ==0) {
                    value = spectrogram[k][i];
                    flux += value < 0? 0: value;
                }
                else {
                    value= spectrogram[k][i] - spectrogram[k-1][i];
                    flux += value < 0? 0: value;
                }

            }
            spectralFlux.add((double) flux);
        }

    }


}
