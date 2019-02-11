package nl.friesoft.solaredgenotifier;

class SolarEdgeEnergy {
    private String energyUnit;
    private int totalEnergy;

    public void setEnergyUnit(String energyUnit) {
        this.energyUnit = energyUnit;
    }

    public String getEnergyUnit() {
        return energyUnit;
    }

    public void setTotalEnergy(int totalEnergy) {
        this.totalEnergy = totalEnergy;
    }

    public int getTotalEnergy() {
        return totalEnergy;
    }

    public String getFormattedEnergy() {
        String r;
        if ("Wh".equals(energyUnit) && totalEnergy > 1000) {
            r = String.format("%d kWh", totalEnergy / 1000);
        } else {
            r = String.format("%d %s", totalEnergy, energyUnit);
        }

        return r;
    }
}
