package nl.friesoft.solaredgenotifier;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Set;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

public class PrefFragment extends PreferenceFragment {
    public static final String PREF_ENABLE = "enabled";
    public static final String PREF_THRESHOLD = "threshold";
    public static final String PREF_OPTIONS = "options";
    public static final String PREF_API_KEY = "pref_api_key";
    public static final String PREF_LASTCHECK = "lastcheck";
    public static final String PREF_SITES = "sites";
    private static final CharSequence PREF_EDITKEYS = "editkeys";

    public static final String OPT_WHENBELOWFIX = "whenbelowfixed";
    public static final String OPT_WHENBELOWAVG = "whenbelowaverage";

    public static final long OPT_MIN_ENERGY = Long.MAX_VALUE;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey);

        final EditTextPreference threshold = (EditTextPreference) findPreference(PREF_THRESHOLD);
        ListPreference options = (ListPreference) findPreference(PREF_OPTIONS);
        SwitchPreference enable = (SwitchPreference) findPreference(PREF_ENABLE);
        Preference editkeys = (Preference) findPreference(PREF_EDITKEYS);
        Preference lastcheck = findPreference(PREF_LASTCHECK);
        Preference sites = findPreference(PREF_SITES);

        SiteStorage siteStorage = new SiteStorage(getContext());
        Resources r = getResources();

        if (siteStorage.count() == 0) {
            sites.setIcon(R.drawable.baseline_warning_24);
            sites.setSummary(getString(R.string.nositesfound));
        } else {
            long c = siteStorage.count();
            String k = r.getQuantityString(R.plurals.found_n_sites, (int)c);
            sites.setSummary(String.format(k, c));
        }

        Persistent p = new Persistent(getActivity());
        Set<String> keys = p.getStringSet(PREF_API_KEY, null);
        if (keys == null || keys.size() == 0) {
            editkeys.setIcon(R.drawable.baseline_warning_24);
            editkeys.setSummary(R.string.contactsupplier);
        } else {
            String k = r.getQuantityString(R.plurals.configured_n_keys, keys.size());
            editkeys.setSummary(String.format(k, keys.size()));
        }
        lastcheck.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                updateLastCheck(preference);

                return true;
            }
        });
        updateLastCheck(lastcheck);

        if (OPT_WHENBELOWFIX.equals(options.getValue())) {
            threshold.setEnabled(true);
        } else {
            threshold.setEnabled(false);
        }
        threshold.setSummary(threshold.getText());

        options.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (OPT_WHENBELOWFIX.equals(newValue)) {
                    threshold.setEnabled(true);
                } else {
                    threshold.setEnabled(false);
                }
                return true;
            }
        });
        threshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        });
        enable.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean)newValue == false) {
                    // disable all alarms
                    Log.i(MainActivity.TAG, "Stopping alarms.");
                    AlarmReceiver.setAlarm(getActivity(), null);
                    AlarmReceiver.enableBoot(getContext(), false);
                } else {
                    // re-enable the alarms
                    Log.i(MainActivity.TAG, "Enabling alarms.");
                    AlarmReceiver.setAlarm(getActivity(), 0l);
                    AlarmReceiver.enableBoot(getContext(), true);
                }

                return true;
            }
        });
    }

    private void updateLastCheck(Preference lastcheck) {
        Persistent p = new Persistent(getActivity());

        String s_lastcheck = p.getString(PREF_LASTCHECK, "");
        if (!"".equals(s_lastcheck)) {
            Check c = Check.fromString(s_lastcheck);
            DateFormat f = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());

            if (c.getType() == Check.Type.SUCCESS) {
                lastcheck.setSummary(String.format(getString(R.string.success_at_s), f.format(c.getDate())));
            } else {
                lastcheck.setSummary(String.format(getString(R.string.fail_at_s_s), f.format(c.getDate()), c.getType().toString()));
            }
        } else {
            lastcheck.setSummary(R.string.never);
        }
    }
}