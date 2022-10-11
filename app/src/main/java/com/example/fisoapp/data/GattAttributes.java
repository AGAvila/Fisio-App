package com.example.fisoapp.data;

import java.util.HashMap;

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String STATUS_CHARACTERISTIC = "7b8f9448-29cb-4dbb-a235-c192668bea5f";
    public static String COMMANDS_CHARACTERISTIC = "3cf92108-210d-437c-8866-029aca5a5faa";
    public static String OIL_LEVEL_CHARACTERISTIC = "5909b27f-3ccc-4c5f-9fbd-89efd55ea4ec";
    public static String OIL_TEMP_CHARACTERISTIC = "869c4dca-6b7d-46e8-9b01-12e8314c57c6";
    public static String CHECKCONTROL_SERVICE = "536eb3d3-2bca-4909-8e56-d89474fa235e";


    static {
        // Sample Services.
        attributes.put(CHECKCONTROL_SERVICE, "Check Control Service");

        // Sample Characteristics.
        attributes.put(STATUS_CHARACTERISTIC, "Status");
        attributes.put(OIL_LEVEL_CHARACTERISTIC, "Oil Level");
        attributes.put(COMMANDS_CHARACTERISTIC, "Commands");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }




}
