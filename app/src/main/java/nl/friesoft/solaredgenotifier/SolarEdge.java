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
    // We will be using the NREL API to get site on max power
    // API KEY = kY9ZgQd5eEimTxw09rfjwRK2bP4JcXvKtvA2D1f7
    // Example URL = https://developer.nrel.gov/api/pvwatts/v6.json?api_key=kY9ZgQd5eEimTxw09rfjwRK2bP4JcXvKtvA2D1f7&lat=40&lon=-105&system_capacity=4&azimuth=180&tilt=40&array_type=1&module_type=1&losses=10
    // Documentation = https://developer.nrel.gov/docs/solar/pvwatts/v6/

    private final static String API_BASE = "https://monitoringapi.solaredge.com/";

    private final static String TASK_SITELIST = "sites";
    private final static String TASK_DETAILS = "details";
    private final static String TASK_ENERGY = "energy";

    private final static String PATH_DETAILS = "site/%d/details";
    private final static String PATH_SITELIST = "sites/list";
    private final static String PATH_ENERGY = "site/%d/energy";

    private ISolarEdgeListener listener;

    public SolarEdge(ISolarEdgeListener _listener) {
        listener = _listener;
    }

    private void runTask(String task, String path, Site site, HashMap<String, String> query) {
        RESTTask restTask = new RESTTask();
        restTask.setUrl(API_BASE);
        restTask.setVerb("GET");
        restTask.setListener(this);
        restTask.setSite(site);

        if (query == null) {
            query = new HashMap<String, String>();
        }
        query.put("api_key", site.getApikey());

        path += "?";
        for (Map.Entry<String, String> p: query.entrySet()) {
            path += "&" + p.getKey() + "=" + p.getValue();
        }

        restTask.execute(task, path);
    }

    public void sites(String apikey) {
        runTask(TASK_SITELIST, PATH_SITELIST, new Site(apikey, 0),null);
    }

    public void details(Site site) {
        runTask(TASK_DETAILS, String.format(PATH_DETAILS, site.getId()), site,null);
    }

    public void energy(Site site, Date startDate, Date endDate) {
        String path = String.format(PATH_ENERGY, site.getId());

        HashMap<String, String> q = new HashMap<>();
        q.put("startDate", formatDate(startDate));
        q.put("endDate", formatDate(endDate));

        runTask(TASK_ENERGY, path, site, q);
    }

    private String formatDate(Date d) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(d);
    }

    @Override
    public void onError(String task, Site site, String m) {
        listener.onError(site, new SolarEdgeException(m));
    }

    @Override
    public void onResult(String task, Site site, JSONObject r) {
        SolarEdgeException exception = null;

        try {
            if (TASK_SITELIST.equals(task)) {
                exception = processSites(site, r);
            } else if (TASK_ENERGY.equals(task)) {
                exception = processEnergy(site, r);
            } else if (TASK_DETAILS.equals(task)) {
                exception = processDetails(site, r);
            }
        } catch (JSONException e) {
            exception = new SolarEdgeException("Failed to parse JSON: "+e.getMessage());
        } catch (Exception e) {
            exception = new SolarEdgeException("Unknown error: "+e.getMessage());
        }

        if (exception != null) {
            listener.onError(site, exception);
        }
    }

    private SolarEdgeException processEnergy(Site site, JSONObject r) throws JSONException {
        Energy result;
        JSONObject energy_meta = r.getJSONObject("energy");

        result = new Energy();
        result.setEnergyUnit(energy_meta.getString("unit"));

        JSONArray values = energy_meta.getJSONArray("values");

        int totalEnergy = 0;
        for (int i = 0; i < values.length(); i++) {
            JSONObject value = values.getJSONObject(i);
            if (!value.isNull("value") && !value.isNull("date")) {
                result.addEnergy(value.getString("date"), value.getInt("value"));
            }
        }

        listener.onEnergy(site, result);

        return null;
    }

    private SolarEdgeException processDetails(Site abstract_site, JSONObject r) throws JSONException {
        JSONObject details = r.getJSONObject("details");
        Site site = new Site(abstract_site.getApikey(), abstract_site.getId());
        site.setName(details.getString("name"));

        JSONObject location = details.getJSONObject("location");
        site.setCity(location.getString("city"));
        site.setCountry(location.getString("country"));

        listener.onDetails(site);

        return null;
    }

    private SolarEdgeException processSites(Site abstract_site, JSONObject r) throws JSONException {
        SolarEdgeException exception = null;

        JSONObject sites_meta = r.getJSONObject("sites");
        int count = sites_meta.getInt("count");
        if (count == 0) {
            exception = new SolarEdgeException("No sites found for API key "+abstract_site.getApikey());
        } else {
            JSONArray sites = sites_meta.getJSONArray("site");
            for (int i = 0; i < sites.length(); i++) {
                JSONObject jsonSite = sites.getJSONObject(i);

                Site site = new Site(abstract_site.getApikey(), jsonSite.getInt("id"));
                site.setName(jsonSite.getString("name"));

                JSONObject location = jsonSite.getJSONObject("location");
                site.setCity(location.getString("city"));
                site.setCountry(location.getString("country"));

                listener.onSiteFound(site);
            }
        }

        return exception;
    }
}
