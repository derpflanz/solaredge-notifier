package nl.friesoft.solaredgenotifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SolarEdge implements RESTTask.RESTListener {
    // We will be using the NREL API to get info on max power
    // API KEY = kY9ZgQd5eEimTxw09rfjwRK2bP4JcXvKtvA2D1f7
    // Example URL = https://developer.nrel.gov/api/pvwatts/v6.json?api_key=kY9ZgQd5eEimTxw09rfjwRK2bP4JcXvKtvA2D1f7&lat=40&lon=-105&system_capacity=4&azimuth=180&tilt=40&array_type=1&module_type=1&losses=10
    // Documentation = https://developer.nrel.gov/docs/solar/pvwatts/v6/

    private final static String API_BASE = "https://monitoringapi.solaredge.com/";

    private final static String TASK_SITES = "sites";
    private final static String TASK_SITE = "info";
    private final static String TASK_ENERGY = "energy";

    private final static String PATH_SITE = "info/%d/details";
    private final static String PATH_SITES = "sites/list";
    private final static String PATH_ENERGY = "info/%d/energy";

    private String apikey = "";
    private ISolarEdgeListener listener;
    private SolarEdgeInfo info;

    public SolarEdge(ISolarEdgeListener _listener, String _apikey) {
        apikey = _apikey;
        listener = _listener;
    }

    private void runTask(String task, String path, HashMap<String, String> query) {
        RESTTask restTask = new RESTTask();
        restTask.setUrl(API_BASE);
        restTask.setVerb("GET");
        restTask.setListener(this);

        if (query == null) {
            query = new HashMap<String, String>();
        }
        query.put("api_key", apikey);

        path += "?";
        for (Map.Entry<String, String> p: query.entrySet()) {
            path += "&" + p.getKey() + "=" + p.getValue();
        }

        restTask.execute(task, path);
    }

    public void sites() {
        runTask(TASK_SITES, PATH_SITES, null);
    }

    public void info(int siteId) {
        runTask(TASK_SITE, String.format(PATH_SITE, siteId), null);
    }

    public void energy(int siteId, Date startDate, Date endDate) {
        String path = String.format(PATH_ENERGY, siteId);

        HashMap<String, String> q = new HashMap<>();
        q.put("startDate", formatDate(startDate));
        q.put("endDate", formatDate(endDate));

        runTask(TASK_ENERGY, path, q);
    }

    private String formatDate(Date d) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(d);
    }

    @Override
    public void onError(String task, String m) {
        listener.onError(this, new SolarEdgeException(m));
    }

    @Override
    public void onResult(String task, JSONObject r) {
        SolarEdgeException exception = null;

        try {
            if (TASK_SITES.equals(task)) {
                exception = processSites(r);
            } else if (TASK_ENERGY.equals(task)) {
                exception = processEnergy(r);
            } else if (TASK_SITE.equals(task)) {
                exception = processSite(r);
            }
        } catch (JSONException e) {
            exception = new SolarEdgeException("Failed to parse JSON: "+e.getMessage());
        }

        if (exception != null) {
            listener.onError(this, exception);
        }
    }



    private SolarEdgeException processEnergy(JSONObject r) throws JSONException {
        SolarEdgeEnergy result;
        JSONObject energy_meta = r.getJSONObject("energy");

        result = new SolarEdgeEnergy();
        result.setEnergyUnit(energy_meta.getString("unit"));

        JSONArray values = energy_meta.getJSONArray("values");

        int totalEnergy = 0;
        for (int i = 0; i < values.length(); i++) {
            JSONObject value = values.getJSONObject(i);
            if (!value.isNull("value") && !value.isNull("date")) {
                result.addEnergy(value.getString("date"), value.getInt("value"));
            }
        }

        listener.onEnergy(this, result);

        return null;
    }

    private SolarEdgeException processSite(JSONObject r) throws JSONException {
        SolarEdgeInfo result;

        result = new SolarEdgeInfo();
        result.setId(r.getInt("id"));
        result.setName(r.getString("name"));

        listener.onInfo(this);

        return null;
    }

    private SolarEdgeException processSites(JSONObject r) throws JSONException {
        SolarEdgeException exception = null;
        SolarEdgeInfo result;
        JSONObject sites_meta = r.getJSONObject("sites");
        int count = sites_meta.getInt("count");
        if (count == 0) {
            exception = new SolarEdgeException("No sites found for API key "+apikey);
        } else {
            JSONArray sites = sites_meta.getJSONArray("info");
            for (int i = 0; i < sites.length(); i++) {
                JSONObject theSite = sites.getJSONObject(i);

                result = new SolarEdgeInfo();
                result.setName(theSite.getString("name"));
                result.setId(theSite.getInt("id"));

                setInfo(result);

                listener.onSiteFound(this);
            }
        }

        return exception;
    }

    public String getApikey() {
        return apikey;
    }


    public void setInfo(SolarEdgeInfo info) {
        this.info = info;
    }

    public SolarEdgeInfo getInfo() {
        return info;
    }
}
