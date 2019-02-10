package nl.friesoft.solaredgenotifier;

interface ISolarEdgeListener {
    void onInitialised(SolarEdge solarEdge);
    void onError(SolarEdge solarEdge, SolarEdgeException exception);
    void onEnergy(SolarEdge solarEdge, SolarEdgeEnergy result);
}
