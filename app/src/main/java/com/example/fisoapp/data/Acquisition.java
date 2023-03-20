package com.example.fisoapp.data;

import android.os.Environment;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Acquisition {

    private byte[] mData;
    private short[] convertedData;
    private int lastIndex;
    private boolean ready_to_plot;
    private final int length_mData = 120000;

    public Acquisition(){
        mData = new byte[length_mData];             // Data buffer 1 byte
        convertedData = new short[mData.length/2];  // Data buffer reconverted to 2 bytes
        lastIndex = 0;
        ready_to_plot = false;
    }


    public byte[] getData(){
        return mData;
    }


    public void setData(byte[] Data){
        mData = Data;
    }


    public int getLastIndex(){
        return lastIndex;
    }


    public void setLastIndex(int LastIndex){
        lastIndex = LastIndex;
    }


    public short[] getConvertedData(){
        return convertedData;
    }


    public void setConvertedData(short[] data16){
        convertedData = data16;
    }


    public boolean isReadyToPlot(){
        return ready_to_plot;
    }


    public void updateReadyToPlot(boolean ready_state){
        ready_to_plot = ready_state;
    }


    public void saveAcquisition(short[] data, String fileName) throws IOException {
        // Saves the data received in a .csv file

        // CSV file name
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        LocalDateTime now = LocalDateTime.now();
        String file_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                + "/" + fileName + dtf.format(now) + ".csv";
        File file = new File(file_path);

        CSVWriter writer;

        // If the file already exists -> Appends data
        if (file.exists() && !file.isDirectory()){
            FileWriter mFileWriter = new FileWriter(file_path, true);
            writer = new CSVWriter(mFileWriter, CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER);
        }
        // If the file does not exists -> Creates it and adds data
        else {
            writer = new CSVWriter(new FileWriter(file_path), CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER);
        }

        // Conversion: short[] -> String[]
        String[] data_string = new String[data.length];
        for (int i = 0; i < data.length; i++){
            data_string[i] = "" + (data[i] & 0xFFFF);
        }

        // Save data
        writer.writeNext(data_string);
        writer.close();

    }

    
    public double calculateRMS(short[] data){
        // Calculate the RMS from a data array

        double rms = 0;

        for (int i = 0; i < data.length; i++) {
            rms += data[i] * data[i];
        }
        rms = Math.sqrt(rms / data.length);
        return rms;

    }


    public void addPacket(byte[] Packet){
        // Stores the 1 byte data received in a single array

        int lastIndex = getLastIndex();
        byte[] data = getData();

        // Copy array "Packet" into array "data"
        System.arraycopy(Packet,0, data, lastIndex, Packet.length);
        lastIndex += Packet.length;
        if (lastIndex >= length_mData){
            lastIndex = 0;
            updateReadyToPlot(true); // The buffer is full
        }
        setData(data);
        setLastIndex(lastIndex);
    }


    public void adaptSamples(byte[] Samples){
        // Adapts samples from 8 bits to 16 bits

        short[] data16 = new short[Samples.length/2];

        for(int i = 0; i<(Samples.length/2); i++){
            data16[i] = (short) ((Samples[2*i] << 8) | Samples[2*i+1] & 0xFF);
        }

        setConvertedData(data16);
    }
}

