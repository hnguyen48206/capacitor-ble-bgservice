package com.hnguyen48206.blesrv;

import android.util.Log;

public class BLEServ {


    public String startService(String value) {
        Log.i("Echo", value);
        return value;
    }

    public String stopService(String value) {
        Log.i("Echo", value);
        return value;
    }
}
