package nl.friesoft.solaredgenotifier;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
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
    public static final String EXTRA_STATUS = "EXTRA_STATUS";

    Persistent persistent;
    Context context;

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
        intent.putExtra(EXTRA_STATUS, reason);

        Log.d(MainActivity.TAG, "Created intent with "+apikey+" and "+installationId);
        return PendingIntent.getActivity(context, installationId, intent, 0);
    }

    @Override
    public void onSiteFound(Site site) {
        Log.i(MainActivity.TAG, String.format("API key %s matches installation %s.", site.getApikey(), site.getName()));

        Calendar aweekago = Calendar.getInstance();
        aweekago.add(Calendar.DATE, -8);

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        SolarEdge solarEdge = new SolarEdge(this);
        solarEdge.energy(site, aweekago.getTime(), yesterday.getTime());

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

        String title;
        long energy_threshold;
        int status;

        // read the options
        String option = persistent.getString(PrefFragment.PREF_OPTIONS, PrefFragment.OPT_DAILY);
        String set_threshold = persistent.getString(PrefFragment.PREF_THRESHOLD, "0");

        switch (option) {
            default:
            case PrefFragment.OPT_DAILY:
                energy_threshold = PrefFragment.OPT_MAX_ENERGY;
                status = Site.STATUS_OK;
                title = context.getString(R.string.output);
                break;
            case PrefFragment.OPT_WHENBELOWAVG:
                energy_threshold = result.getAverageEnergy();
                status = Site.STATUS_BELOWAVG;
                title = context.getString(R.string.outputbelowavg);
                break;
            case PrefFragment.OPT_WHENBELOWFIX:
                energy_threshold = Long.parseLong(set_threshold);
                status = Site.STATUS_BELOWFIXED;
                title = context.getString(R.string.outputbelowlevel);
                break;
        }

        int update_status = Site.STATUS_OK;
        if (result.getDailyEnergy(-1) < energy_threshold) {
            update_status = status;
            createNotification(site, result, title, energy_threshold, status);
        }

        // update the status of the site
        ContentValues cv = new ContentValues();
        SiteStorage storage = new SiteStorage(context);
        cv.put(SiteStorage.COL_STATUS, update_status);
        storage.update(site, cv);

        // success, check again tomorrow
        setAlarm(context, INTERVAL_SUCCESS);
    }

    void createNotification(Site site, Energy result, String title, long energy_threshold, int status) {
        String message, longmessage;
        int icon;

        if (energy_threshold == Long.MAX_VALUE) {
            // when set to MAX_VALUE, we use a slightly nicer message
            message = String.format(context.getString(R.string.energyoutput),
                    site.getName(), Energy.format(result.getTotalEnergy()));
            longmessage = String.format(context.getString(R.string.output_trend),
                    site.getName(), Energy.format(result.getTotalEnergy()));
            icon = R.drawable.outline_wb_sunny_24;
        } else {
            message = String.format(context.getString(R.string.energyoutput),
                    site.getName(), Energy.format(result.getTotalEnergy()));
            longmessage = String.format(context.getString(R.string.outputlow_long),
                    site.getName(), Energy.format(result.getTotalEnergy()), energy_threshold);
            icon = R.drawable.outline_wb_cloudy_24;
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_OUTPUT_LEVELS)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(longmessage))
                .setContentIntent(getPendingIntent(site, status))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        // we use the installation's ID to make sure all notifications get sent
        notificationManager.notify(site.getId(), mBuilder.build());
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
