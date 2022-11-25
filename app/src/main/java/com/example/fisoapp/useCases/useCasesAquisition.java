package com.example.fisoapp.useCases;

import android.app.Activity;

import com.example.fisoapp.data.Acquisition;

import java.util.AbstractQueue;

public class useCasesAquisition {

    private byte test;
    private Acquisition mAquisition;
    private Activity mactivity;

    public  useCasesAquisition(Acquisition acquisition, Activity activity){
        mAquisition = acquisition;
        mactivity = activity;
    }


    public void saveAquisition( byte[] aquisition, char[] fileName ){

        //ToDo: Make save function
    }

    public double calculateRMS(){
        double rms = 0;
        byte [] data = mAquisition.getData();

        for (int i = 0; i < data.length; i++) {
            rms += data[i] * data[i];
        }
        rms = Math.sqrt(rms / data.length);
        return rms;

    }


    public void addPacket(byte[] Packet){
        int lastIndex = mAquisition.getLastIndex();
        byte[] data = mAquisition.getData();

        System.arraycopy(Packet,0, data, lastIndex, Packet.length);
        lastIndex += Packet.length;
        if (lastIndex >= 24000){
            lastIndex = 0;
        }
        mAquisition.setData(data);
        mAquisition.setLastIndex(lastIndex);

    }


    private short[] adaptSamples(byte[] Samples){
        short[] data16 = new short[1] ;

        for(int i = 0; i<Samples.length; i++){
            //ToDo
        }


        return data16;

    }
}
