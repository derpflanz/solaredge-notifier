package nl.friesoft.solaredgenotifier;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashSet;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ManageApiKeysActivity extends AppCompatActivity implements ApiKeyCallbacks, ISolarEdgeListener {

    private ListView lvApiKeys;
    private Persistent persistent;
    private ApiKeyAdapter apiKeyAdapter;
    private Set<String> apikeys;
    private SiteStorage siteStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_api_keys);
        setTitle(R.string.manageapikeys);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        siteStorage = new SiteStorage(this);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showApiKeyDialog();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        persistent = new Persistent(this);

        apikeys = persistent.getStringSet(PrefFragment.PREF_API_KEY, new HashSet<String>());
        apiKeyAdapter = new ApiKeyAdapter(this, apikeys);

        lvApiKeys = findViewById(R.id.lvApiKeys);
        lvApiKeys.setAdapter(apiKeyAdapter);
    }

    private void showApiKeyDialog() {
        ApiKeyDialog dialog = new ApiKeyDialog();
        dialog.show(getFragmentManager(), ApiKeyDialog.TAG);
    }

    @Override
    public void onApiKeyAdded(String apikey) {
        persistent.putStringToSet(PrefFragment.PREF_API_KEY, apikey);
        apikeys.add(apikey);
        apiKeyAdapter.notifyDataSetChanged();

        siteStorage.delete(apikey);

        SolarEdge solarEdge = new SolarEdge(this, apikey);
        solarEdge.sites();
    }

    @Override
    public void onApiKeyDeleted(String apikey) {
        persistent.removeFromSet(PrefFragment.PREF_API_KEY, apikey);
        apikeys.remove(apikey);
        apiKeyAdapter.notifyDataSetChanged();

        siteStorage.delete(apikey);
    }

    @Override
    public void onSiteFound(SolarEdge solarEdge) {
        Site s = new Site(solarEdge.getApikey(), solarEdge.getInfo().getId());
        s.setName(solarEdge.getInfo().getName());
        siteStorage.add(s);
    }

    @Override
    public void onError(SolarEdge solarEdge, SolarEdgeException exception) { }

    @Override
    public void onEnergy(SolarEdge solarEdge, SolarEdgeEnergy result) { }

    @Override
    public void onInfo(SolarEdge solarEdge) { }
}
