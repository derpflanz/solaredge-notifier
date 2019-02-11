package nl.friesoft.solaredgenotifier;

import android.os.Bundle;
import android.util.Log;

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

    public static final String OPT_WHENBELOW = "whenbelow";
    public static final long OPT_MIN_ENERGY = Long.MAX_VALUE;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey);

        final EditTextPreference threshold = (EditTextPreference) findPreference(PREF_THRESHOLD);
        ListPreference options = (ListPreference) findPreference(PREF_OPTIONS);
        SwitchPreference enable = (SwitchPreference) findPreference(PREF_ENABLE);

        if (OPT_WHENBELOW.equals(options.getValue())) {
            threshold.setEnabled(true);
        } else {
            threshold.setEnabled(false);
        }
        threshold.setSummary(threshold.getText());

        options.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (OPT_WHENBELOW.equals(newValue)) {
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
                } else {
                    // re-enable the alarms
                    Log.i(MainActivity.TAG, "Enabling alarms.");
                    AlarmReceiver.setAlarm(getActivity(), 0l);
                }

                return true;
            }
        });
    }
}