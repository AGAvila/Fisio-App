package com.example.fisoapp.data;

public class Acquisition {

    private  byte[] mPacket;
    private  byte[] mData;
    private  byte mRMS;
    private  byte mMNF;
    private  byte mThreshold;

    private int noPacket;
    private int lastIndex;

    public void Aquisition( ){
        mData = new byte[24000];
        mPacket = new byte[4000];
        lastIndex = 0;
    }


    public void appendPacket(byte[] Packet){
        //mPacket = Packet;
        System.arraycopy(Packet,0, mData, lastIndex, Packet.length);
        lastIndex += Packet.length;
        if (lastIndex >= 24000){
            lastIndex = 0;
        }

    }



}