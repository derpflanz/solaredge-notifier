package nl.friesoft.solaredgenotifier;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

    /*
        We use the Android Studio to track our features. They are listed here.

        [dropped] TODO Timed notification, on a certain time every day
        [done] TODO Notification whenever output is below a certain value, or 'always' (min_energy = maxlong)
        [done] TODO Store settings on phone
        [done] TODO Enable / disable notifier
        [done] TODO Create MainActivity to change settings
        [done] TODO Multi system (multi API key) support

        (for next versions)
        TODO A smooth way to input the API key (QR/OCR?)
        TODO More advanced ways of checking (averaging, looking back more days, etc, AI?)
        TODO Make a link from our notifier's "InstallationActivity" to the SolarEdge app if available
        TODO Multi site per API key

        (for release)
        [done] TODO Implement the InstallationActivity
        [dropped] TODO Implement the no API keys notification
        TODO Start alarmmanager on boot
        [done] TODO (bug) The PendingIntent in the Notification always gives one API/InstallId

        Basic functionality:
        1. An alarm is set to broadcast an intent "AlarmReceiver"
        2. This intent goes to the server and receives solar output of "yesterday"
        3. If the server does not respond, try again in 15 minutes
        4. If the server responds, try again in 24 hours
        5. If there is a response, process it:
            5a. If set to always, notify with output of yesterday
            5b. If set to threshold, check threshold and notify if lower
        6. If notification is tapped, open the DiagActivity to show yesterday,
            last week, last month output

        Notes:
        o There is no way to disable the notification. If you do not want notifications, either
          switch them off in the Android settings, or uninstall the app.
        o Info on the AlarmManager can be found here:
          https://developer.android.com/training/scheduling/alarms#java
        o API keys for testing are 7W2265S86DQXAJKDBJYTEDYZLRSA817F and
          PIEC7WMKQU7WLAU4BZT7F6BPMKXBKNWJ, you can use SMS to send them
          to the emulator

     */

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
