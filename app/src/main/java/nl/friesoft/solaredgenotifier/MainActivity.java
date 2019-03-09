package nl.friesoft.solaredgenotifier;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Persistent persistent;

    // Miscellaneous constants
    public static final String TAG = "SolarEdgeNotif";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tvVersion = findViewById(R.id.tvVersion);
        try {
            tvVersion.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException nnfe) {
            tvVersion.setText("");
        }
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
