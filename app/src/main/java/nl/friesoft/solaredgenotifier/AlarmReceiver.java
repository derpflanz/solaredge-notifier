package nl.friesoft.solaredgenotifier;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.Calendar;
import java.util.Set;

public class AlarmReceiver extends BroadcastReceiver implements ISolarEdgeListener {
    private static final String CHANNEL_OUTPUT_LEVELS = "OUTPUTLEVELS";
    public static final String EXTRA_API_KEY = "EXTRA_API_KEY";
    public static final String EXTRA_INSTALLATION_ID = "EXTRA_INSTALLATION_ID";
    Persistent persistent;
    Context context;

    private static final long INTERVAL_ERROR = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    private static final long INTERVAL_SUCCESS = AlarmManager.INTERVAL_DAY;

    public void setNextCheck(long interval) {
        Log.i(MainActivity.TAG, "Setting alarm to check again in "+interval+" msec.");

        AlarmManager aMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, i, 0);
        aMgr.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval, pendingIntent);
    }

    @Override
    public void onReceive(Context _context, Intent intent) {
        context = _context;

        createNotificationChannels();

        // when we are fired, we start checking immediately
        persistent = new Persistent(context, context.getApplicationContext().getPackageName());
        Set<String> apikeys = persistent.getStringSet(MainActivity.PREF_API_KEY, null);

        if (apikeys == null) {
            // TODO notify once that the notifier is not set up correctly
            Log.e(MainActivity.TAG, "No API keys set.");
        } else {
            for(String apikey: apikeys) {
                SolarEdge sol = new SolarEdge(this, apikey);
                sol.initialise();
            }
        }
    }

    private PendingIntent getPendingIntent(String apikey, int installationId) {
        Intent intent = new Intent(context, InstallationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_API_KEY, apikey);
        intent.putExtra(EXTRA_INSTALLATION_ID, installationId);

        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    @Override
    public void onInitialised(SolarEdge solarEdge) {
        Log.i(MainActivity.TAG, String.format("API key %s matches installation %s.", solarEdge.getApikey(), solarEdge.getInfo().getName()));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);

        solarEdge.energy(solarEdge.getInfo().getId(), cal.getTime(), cal.getTime());
    }

    @Override
    public void onError(SolarEdge solarEdge, SolarEdgeException exception) {
        Log.e(MainActivity.TAG, "Some error occurred: "+exception.getMessage());

        // on error, try again in 15 minutes
        setNextCheck(INTERVAL_ERROR);
    }

    @Override
    public void onEnergy(SolarEdge solarEdge, SolarEdgeEnergy result) {
        Log.i(MainActivity.TAG, String.format("%s had %d %s", solarEdge.getInfo().getName(), result.getTotalEnergy(), result.getEnergyUnit()));

        // by default, we only notify when no output is generated
        long min_energy = persistent.getLong(MainActivity.PREF_MIN_ENERGY, 0L);
        if (result.getTotalEnergy() < min_energy) {
            String message, longmessage, title;
            int icon;

            if (min_energy == Long.MAX_VALUE) {
                // when set to MAX_VALUE, we use a slightly nicer message
                title = context.getString(R.string.output);
                message = String.format(context.getString(R.string.energyoutput),
                        solarEdge.getInfo().getName(), result.getTotalEnergy(), result.getEnergyUnit());
                longmessage = String.format(context.getString(R.string.output_trend),
                        solarEdge.getInfo().getName(), result.getTotalEnergy(), result.getEnergyUnit());
                icon = R.drawable.outline_wb_sunny_24;
            } else {
                title = context.getString(R.string.outputbelowlevel);
                message = String.format(context.getString(R.string.energyoutput),
                        solarEdge.getInfo().getName(), result.getTotalEnergy(), result.getEnergyUnit());
                longmessage = String.format(context.getString(R.string.outputlow_long),
                        solarEdge.getInfo().getName(), result.getTotalEnergy(), result.getEnergyUnit(), min_energy);
                icon = R.drawable.outline_wb_cloudy_24;
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_OUTPUT_LEVELS)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(longmessage))
                    .setContentIntent(getPendingIntent(solarEdge.getApikey(), solarEdge.getInfo().getId()))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // notificationId is a unique int for each notification that you must define
            // we use the installation's ID to make sure all notifications get sent
            notificationManager.notify(solarEdge.getInfo().getId(), mBuilder.build());
        }

        // success, check again tomorrow
        setNextCheck(INTERVAL_SUCCESS);
    }

    private void createNotificationChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // output levels notification channel
            NotificationChannel output_level = new NotificationChannel(CHANNEL_OUTPUT_LEVELS,
                    context.getString(R.string.output_levels),
                    NotificationManager.IMPORTANCE_DEFAULT);
            output_level.setDescription(context.getString(R.string.output_level_descr));

            // add more channels here ...

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(output_level);
        }
    }
}
