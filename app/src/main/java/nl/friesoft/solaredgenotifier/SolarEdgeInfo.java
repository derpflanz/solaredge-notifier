package nl.friesoft.solaredgenotifier;

class SolarEdgeInfo {
    public static final SolarEdgeInfo NONE = new SolarEdgeInfo();

    private String name;
    private int id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
