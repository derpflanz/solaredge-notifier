package nl.friesoft.solaredgenotifier;

import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;

public class Check {
    private Type type;
    private Date date;

    public Check(Type _type) {
        date = Calendar.getInstance().getTime();
        type = _type;
    }

    public Type getType() {
        return type;
    }

    public Date getDate() {
        return date;
    }

    public enum Type {
        NOKEY,
        FAIL,
        SUCCESS;
    }

    @NonNull
    @Override
    public String toString() {
        Calendar c = Calendar.getInstance();
        c.setTime(date);

        return String.format("%d|%s", c.getTimeInMillis(), type.toString());
    }
    public static Check fromString(String s_lastcheck) {
        String[] s = s_lastcheck.split("\\|");
        Type t = Type.valueOf(s[1]);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Long.parseLong(s[0]));

        Check check = new Check(t);
        check.date = c.getTime();

        return check;
    }
}
