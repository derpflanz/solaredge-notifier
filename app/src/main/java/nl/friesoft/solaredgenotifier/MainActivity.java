package nl.friesoft.solaredgenotifier;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

    /*
        We use the Android Studio to track our features. They are listed here.

        [dropped] TODO Timed notification, on a certain time every day
        [done] TODO Notification whenever output is below a certain value, or 'always' (min_energy = maxlong)
        TODO A smooth way to input the API key (QR?)
        TODO Multi system (multi API key) support
        TODO More advanced ways of checking (averaging, looking back more days, etc, AI?)
        TODO Make a link from our notifier's "InstallationActivity" to the SolarEdge app if available
        TODO Start alarmmanager on boot
        TODO Store settings on phone
        TODO Multi site per API key
        TODO Enable / disable notifier
        TODO Create MainActivity to change settings

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

     */

public class MainActivity extends AppCompatActivity implements ISolarEdgeListener {
    public static final int ERR_UNKNOWN = 99;
    private static final int REQ_MANAGE_API_KEYS = 1;
    private Persistent persistent;

    // Preference names, to be stored / retrieved from Persistent
    public static final String PREF_API_KEY = "pref_api_key";
    public static final String PREF_LASTCHECKED = "pref_lastchecked";
    public static final String PREF_MIN_ENERGY = "pref_min_energy";

    // Preference defaults
    private static final String DEF_API_KEY1 = "7W2265S86DQXAJKDBJYTEDYZLRSA817F";
    private static final String DEF_API_KEY2 = "PIEC7WMKQU7WLAU4BZT7F6BPMKXBKNWJ";

    // the check is: if output < min_energy: notify, so setting this to Long.MAX_VALUE
    // means, always notify on successful check
    private static final long DEF_MIN_ENERGY = Long.MAX_VALUE;

    // Miscellaneous constants
    public static final String TAG = "SolarEdgeNotif";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        persistent = new Persistent(this, getApplicationContext().getPackageName());
        persistent.putLong(PREF_MIN_ENERGY, DEF_MIN_ENERGY);

        // fire the Broadcast intent once, so the AlarmReceiver
        // can set the alarm, but of course not when coming
        // from the notification
        Intent i = new Intent(this, AlarmReceiver.class);
        sendBroadcast(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean r = true;
        switch (item.getItemId()) {
            case R.id.manage_api_keys:
                Intent i = new Intent(this, ManageApiKeysActivity.class);
                startActivityForResult(i, REQ_MANAGE_API_KEYS);
                break;
            default:
                r = super.onOptionsItemSelected(item);
                break;
        }

        return r;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void btnTrigger_onClick(View view) {
        SolarEdge sol = new SolarEdge(this, DEF_API_KEY1);
        sol.initialise();
    }

    @Override
    public void onInitialised(SolarEdge solarEdge) {
        TextView tvName = findViewById(R.id.tvName);
        tvName.setText(solarEdge.getInfo().getName());

        solarEdge.energy(solarEdge.getInfo().getId(), new Date(), new Date());
    }

    @Override
    public void onError(SolarEdge solarEdge, SolarEdgeException exception) {
        Toast.makeText(this, "Something went wrong: "+exception.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onEnergy(SolarEdge solarEdge, SolarEdgeEnergy result) {
        TextView tvEnergy = findViewById(R.id.tvEnergy);
        tvEnergy.setText(String.format("%d %s", result.getTotalEnergy(), result.getEnergyUnit()));
    }
}
