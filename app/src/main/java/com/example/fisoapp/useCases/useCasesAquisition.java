package com.example.fisoapp.useCases;

import android.app.Activity;
import android.os.Environment;

import com.example.fisoapp.data.Acquisition;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class useCasesAquisition {

    private byte test;
    private Acquisition mAquisition;
    private Activity mactivity;

    public  useCasesAquisition(Acquisition acquisition, Activity activity){
        mAquisition = acquisition;
        mactivity = activity;
    }

    //ToDo Make save function
    public void saveAcquisition(byte[] acquisition, String fileName ) throws IOException {
        // Method that saves the data received from sensors in a .csv file

        String file_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                + "/" + fileName + ".csv";
        File file = new File(file_path);

        CSVWriter writer;

         // If the file already exists -> Appends data
        if (file.exists() && !file.isDirectory()){
            FileWriter mFileWriter = new FileWriter(file_path, true);
            writer = new CSVWriter(mFileWriter);
        }
        // If the file does not exists -> Creates it and adds data
        else {
            writer = new CSVWriter(new FileWriter(file_path));
        }

        // Conversion: Byte[] -> String[]
        String[] acquisition_str = new String[acquisition.length];
        for (int i = 0; i < acquisition.length; i++){
            acquisition_str[i] = "" + acquisition[i];
        }

        // Save data
        writer.writeNext(acquisition_str);
        writer.close();

    }

    public double calculateRMS(){
        double rms = 0;
        byte[] data = mAquisition.getData();

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

        for(int i = 0; i<Samples.length/2-1; i++){
            //ToDo: adapt samples from 8 bits to 16 bits
            data16[i] = (short) (Samples[2*i]*2^8+Samples[2*i+1]);
        }


        return data16;

    }
}
