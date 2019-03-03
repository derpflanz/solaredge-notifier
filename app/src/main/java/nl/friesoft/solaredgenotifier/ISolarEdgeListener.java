package nl.friesoft.solaredgenotifier;

interface ISolarEdgeListener {
    void onSiteFound(SolarEdge solarEdge);
    void onError(SolarEdge solarEdge, SolarEdgeException exception);
    void onEnergy(SolarEdge solarEdge, SolarEdgeEnergy result);

    void onInfo(SolarEdge solarEdge);
}
