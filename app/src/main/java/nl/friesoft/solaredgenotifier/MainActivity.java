package nl.friesoft.solaredgenotifier;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Persistent persistent;

    // Miscellaneous constants
    public static final String TAG = "SolarEdgeNotif";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        persistent = new Persistent(this);

        if (persistent.getBoolean(PrefFragment.PREF_ENABLE, true)) {
            if (!AlarmReceiver.isRunning(this)) {
                Log.i(MainActivity.TAG, "Alarm was not set yet, setting on MainActivity onCreate. This might trigger immediate notifications if set to 'Daily'");
                AlarmReceiver.setAlarm(this, 0l);
            } else {
                Log.i(MainActivity.TAG, "Alarm was previously set, not re-setting on MainActivity onCreate");
            }

            // enable the bootreceiver
            AlarmReceiver.enableBoot(this, true);
        } else {
            AlarmReceiver.enableBoot(this, false);
        }
    }
}
