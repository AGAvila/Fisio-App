package com.example.fisoapp.domain;

import java.util.HashMap;

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String TX_CHAR = "0003CDD1-0000-1000-8000-00805f9b0131";
    public static String RX_CHAR = "0003CDD2-0000-1000-8000-00805f9b0131";
    public static String OIL_LEVEL_CHARACTERISTIC = "5909b27f-3ccc-4c5f-9fbd-89efd55ea4ec";
    public static String OIL_TEMP_CHARACTERISTIC = "869c4dca-6b7d-46e8-9b01-12e8314c57c6";
    public static String CHECKCONTROL_SERVICE = "536eb3d3-2bca-4909-8e56-d89474fa235e";


    static {
        // Sample Services.
        attributes.put(CHECKCONTROL_SERVICE, "Check Control Service");

        // Sample Characteristics.
        attributes.put(TX_CHAR, "Tx");
        attributes.put(RX_CHAR, "Rx");
    }


    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

}
