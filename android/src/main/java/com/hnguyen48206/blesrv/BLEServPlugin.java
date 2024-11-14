package com.hnguyen48206.blesrv;

import static android.content.Context.MODE_PRIVATE;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

@CapacitorPlugin(name = "BLEServ")
public class BLEServPlugin extends Plugin {
    private static final String PREFS_NAME = "CapacitorStorage";
    private static final String SERVICE_RUNNING_KEY = "serviceRunning";
    private Context context;
    private BLEServ implementation = new BLEServ();
    @Override
    public void load() {
        // Get the context
        context = this.getActivity().getApplicationContext();
    }
    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void startService(PluginCall call) {
        Intent intent = new Intent(getContext(), BLEForegroundService.class);
        getContext().startService(intent);
        call.resolve();
    }

    @PluginMethod
    public void stopService(PluginCall call) {
        saveServiceState(false);
        call.resolve();
    }

    private void saveServiceState(boolean isRunning) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SERVICE_RUNNING_KEY, isRunning);
        editor.apply();
    }
}
