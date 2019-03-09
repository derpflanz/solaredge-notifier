package nl.friesoft.solaredgenotifier;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;
import java.util.Set;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver implements ISolarEdgeListener {
    private static final String CHANNEL_OUTPUT_LEVELS = "OUTPUTLEVELS";
    public static final String EXTRA_API_KEY = "EXTRA_API_KEY";
    public static final String EXTRA_INSTALLATION_ID = "EXTRA_INSTALLATION_ID";
    public static final String EXTRA_REASON = "EXTRA_REASON";
    private static final int REQ_APIKEYS = 1;
    private static final String CHANNEL_WARNINGS = "WARNINGS";
    Persistent persistent;
    Context context;

    public static int REASON_ALLOK = 0;
    public static int REASON_ERROR = 1;

    private static final long INTERVAL_ERROR = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    private static final long INTERVAL_SUCCESS = AlarmManager.INTERVAL_DAY;

    // set interval to null to cancel the alarm
    public static void setAlarm(Context ctx, Long interval) {
        AlarmManager alarmManager = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx,0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        if (interval == null) {
            Log.d(MainActivity.TAG, "Alarm canceled");
            alarmManager.cancel(pendingIntent);
        } else {
            Log.d(MainActivity.TAG, "Alarm set in "+interval+" msecs");
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval, pendingIntent);
        }
    }

    public static boolean isRunning(Context ctx) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx,0, i, PendingIntent.FLAG_NO_CREATE);

        return pendingIntent != null;
    }

    public static void enableBoot(Context context, boolean b) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                b?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onReceive(Context _context, Intent intent) {
        context = _context;
        createNotificationChannels();

        // when we are fired, we start checking immediately
        persistent = new Persistent(context);
        Set<String> apikeys = persistent.getStringSet(PrefFragment.PREF_API_KEY, null);

        if (apikeys.size() == 0) {
            // If not API keys are set when the alarm goes, we log this,
            // but don't do anything else.
            // The MainActivity should give enough details on how the whole thing
            // works.
            Log.e(MainActivity.TAG, "No API keys set!");

            Check c = new Check(Check.Type.NOKEY);
            persistent.putString(PrefFragment.PREF_LASTCHECK, c.toString());
        } else {
            // switch true to false to enable the checker
            if (false) {
                setAlarm(context, 5000l);
            } else {
                for (String apikey : apikeys) {
                    // We call sites(), because we want all sites connected to the
                    // API keys. onSiteFound() will be possibly called more than once
                    SolarEdge sol = new SolarEdge(this);
                    sol.sites(apikey);
                }
            }
        }
    }

    private PendingIntent getPendingIntent(Site site, int reason) {
        String apikey = site.getApikey();
        int installationId = site.getId();

        Intent intent = new Intent(context, SiteActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_API_KEY, apikey);
        intent.putExtra(EXTRA_INSTALLATION_ID, installationId);
        intent.putExtra(EXTRA_REASON, reason);

        Log.d(MainActivity.TAG, "Created intent with "+apikey+" and "+installationId);
        return PendingIntent.getActivity(context, installationId, intent, 0);
    }

    @Override
    public void onSiteFound(Site site) {
        Log.i(MainActivity.TAG, String.format("API key %s matches installation %s.", site.getApikey(), site.getName()));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);

        SolarEdge solarEdge = new SolarEdge(this);
        solarEdge.energy(site, cal.getTime(), cal.getTime());

        Check c = new Check(Check.Type.SUCCESS);
        persistent.putString(PrefFragment.PREF_LASTCHECK, c.toString());
    }

    @Override
    public void onError(Site site, SolarEdgeException exception) {
        Log.e(MainActivity.TAG, "Some error occurred: "+exception.getMessage());

        // on error, try again in 15 minutes
        setAlarm(context, INTERVAL_ERROR);

        Check c = new Check(Check.Type.FAIL);
        persistent.putString(PrefFragment.PREF_LASTCHECK, c.toString());
    }

    @Override
    public void onEnergy(Site site, Energy result) {
        Log.i(MainActivity.TAG, String.format("%s had %d Wh", site.getName(), result.getTotalEnergy()));

        // by default, we only notify when no output is generated
        long min_energy = 0;
        int reason = REASON_ALLOK;
        String option = persistent.getString(PrefFragment.PREF_OPTIONS, PrefFragment.OPT_WHENBELOW);
        if (PrefFragment.OPT_WHENBELOW.equals(option)) {
            String threshold = persistent.getString(PrefFragment.PREF_THRESHOLD, "");

            try {
                min_energy = Long.parseLong(threshold);
            } catch (NumberFormatException nfe) {
                min_energy = PrefFragment.OPT_MIN_ENERGY;
            }
        } else {
            // option set to daily, so the min_energy level is MAX
            min_energy = PrefFragment.OPT_MIN_ENERGY;
        }

        if (result.getTotalEnergy() < min_energy) {
            String message, longmessage, title;
            int icon;

            if (min_energy == Long.MAX_VALUE) {
                // when set to MAX_VALUE, we use a slightly nicer message
                title = context.getString(R.string.output);
                message = String.format(context.getString(R.string.energyoutput),
                        site.getName(), Energy.format(result.getTotalEnergy()));
                longmessage = String.format(context.getString(R.string.output_trend),
                        site.getName(), Energy.format(result.getTotalEnergy()));
                icon = R.drawable.outline_wb_sunny_24;
            } else {
                title = context.getString(R.string.outputbelowlevel);
                message = String.format(context.getString(R.string.energyoutput),
                        site.getName(), Energy.format(result.getTotalEnergy()));
                longmessage = String.format(context.getString(R.string.outputlow_long),
                        site.getName(), Energy.format(result.getTotalEnergy()), min_energy);
                icon = R.drawable.outline_wb_cloudy_24;
                reason = REASON_ERROR;
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_OUTPUT_LEVELS)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(longmessage))
                    .setContentIntent(getPendingIntent(site, reason))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // notificationId is a unique int for each notification that you must define
            // we use the installation's ID to make sure all notifications get sent
            notificationManager.notify(site.getId(), mBuilder.build());
        }

        // success, check again tomorrow
        setAlarm(context, INTERVAL_SUCCESS);
    }

    @Override
    public void onDetails(Site site) {
        // never entered, we don't call 'details()'
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
