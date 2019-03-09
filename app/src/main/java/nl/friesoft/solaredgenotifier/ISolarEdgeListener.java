package nl.friesoft.solaredgenotifier;

interface ISolarEdgeListener {
    void onSiteFound(Site site);
    void onError(Site site, SolarEdgeException exception);
    void onEnergy(Site site, Energy result);

    void onDetails(Site site);
}
