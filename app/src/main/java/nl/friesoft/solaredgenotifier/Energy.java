package nl.friesoft.solaredgenotifier;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Energy {
    private String energyUnit;

    private SortedMap<Date, Integer> energy;

    public Energy() {
        energy = new TreeMap<>();
    }

    // the energy unit is only used to calculate back to Wh; we store
    // all data in Wh and calc back when create a human readable string
    public void setEnergyUnit(String energyUnit) {
        this.energyUnit = energyUnit;
    }
    private String getEnergyUnit() { return energyUnit; }

    public static String format(long energy) {
        String r;
        if (energy > 1000) {
            r = String.format("%.02f kWh", energy / 1000.0);
        } else {
            r = String.format("%d Wh", energy);
        }

        return r;
    }

    public void addEnergy(String date, int value) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(date);
            if ("kWh".equals(getEnergyUnit())) {
                value *= 1000;
            }
            energy.put(d, value);
        } catch (ParseException pe) {
            Log.e(MainActivity.TAG, "Could not parse date: "+date);
        }
    }

    // energy result methods
    public int getTotalEnergy() {
        int totalEnergy = 0;

        for (Map.Entry<Date, Integer> entry: energy.entrySet()) {
            totalEnergy += entry.getValue();
        }

        return totalEnergy;
    }

    public int getAverageEnergy() {
        int totalEnergy = 0;
        int days = 0;

        for (Map.Entry<Date, Integer> entry: energy.entrySet()) {
            totalEnergy += entry.getValue();
            days++;
        }

        return totalEnergy / days;
    }

    // return value of last day in the HashMap
//    public int getLastEnergy() {
//        Calendar c = Calendar.getInstance();
//        c.add(Calendar.YEAR, -100);
//        Date max = c.getTime();
//
//        for (Map.Entry<Date, Integer> entry: energy.entrySet()) {
//            Date d = entry.getKey();
//            if (d.compareTo(max) > 0) {
//                max = d;
//            }
//        }
//
//        return energy.get(max);
//    }

    // 0, 1, 2, 3, 4: count from the start (0 being the oldest date)
    // -1, -2, -3, -4: count from the end (-1 being the newest date)
    public int getDailyEnergy(int i) {
        int index = i;

        if (i < 0) {
            // use negative values to count from the last one
            index = energy.size() + i;
        }

        return ((Integer)energy.values().toArray()[index]).intValue();
    }
}
