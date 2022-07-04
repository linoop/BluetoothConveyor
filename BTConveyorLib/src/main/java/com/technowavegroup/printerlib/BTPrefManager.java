package com.technowavegroup.printerlib;

import android.content.Context;
import android.content.SharedPreferences;

public class BTPrefManager {
    private static final String CONVEYOR_PREF = "conveyor_pref";
    private static final String MAC_ADDRESS = "conveyor_mac";
    private static BTPrefManager BTPrefManagerInstance;
    private Context context;

    public BTPrefManager(Context context) {
        this.context = context;
    }

    public static synchronized BTPrefManager getInstance(Context context) {
        if (BTPrefManagerInstance == null) {
            BTPrefManagerInstance = new BTPrefManager(context);
        }
        return BTPrefManagerInstance;
    }

    public void saveDeviceMacAddress(String macAddress) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(CONVEYOR_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MAC_ADDRESS, macAddress);
        editor.apply();
    }

    public String getDeviceMacAddress() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(CONVEYOR_PREF, Context.MODE_PRIVATE);
        return sharedPreferences.getString(MAC_ADDRESS, "");
    }

    public boolean clearPrinterMacAddress(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(CONVEYOR_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        return editor.commit();
    }
}
