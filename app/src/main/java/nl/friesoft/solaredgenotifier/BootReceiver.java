package nl.friesoft.solaredgenotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.i(MainActivity.TAG, "Boot completed, issuing SolarEdge alarm in 0msec");
            AlarmReceiver.setAlarm(context, 0l);
        }
    }
}
