package nl.friesoft.solaredgenotifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bart on 3/2/16.
 */
public class Persistent {
    Context ctx;
    SharedPreferences prefs;

    public Persistent(Context _ctx) {
        ctx = _ctx;
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public void remove(String key) {
        SharedPreferences.Editor e = prefs.edit();
        e.remove(key);
        e.commit();
    }

    public void putLong(String key, long value) {
        SharedPreferences.Editor e = prefs.edit();
        e.putLong(key, value);
        e.commit();
    }

    public boolean getBoolean(String key, boolean def) { return prefs.getBoolean(key, def); }

    public long getLong(String key, long def) {
        return prefs.getLong(key, def);
    }

    public String getString(String key, String def) {
        return prefs.getString(key, def);
    }

    public void putString(String key, String value) {
        SharedPreferences.Editor e = prefs.edit();
        e.putString(key, value);
        e.commit();
    }

    public Set<String> getStringSet(String key, Set<String> def) {
        return prefs.getStringSet(key, def);
    }

    private Set<String> getCopyOfSet(String key) {
        Set<String> theSet = prefs.getStringSet(key, null);
        HashSet<String> newSet;
        if (theSet == null) {
            // we didn't have a list yet, create a new one
            newSet = new HashSet<>();
        } else {
            // we had a set, make a copy, so we can store it correctly
            newSet = new HashSet<>(theSet);
        }

        return newSet;
    }

    public void putStringToSet(String key, String value) {
        Set<String> newSet = getCopyOfSet(key);
        newSet.add(value);

        SharedPreferences.Editor e = prefs.edit();
        e.putStringSet(key, newSet);
        e.commit();
    }

    public void removeFromSet(String key, String value) {
        Set<String> newSet = getCopyOfSet(key);
        newSet.remove(value);

        SharedPreferences.Editor e = prefs.edit();
        e.putStringSet(key, newSet);
        e.commit();
    }
}
