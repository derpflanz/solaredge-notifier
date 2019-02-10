package nl.friesoft.solaredgenotifier;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.HashSet;
import java.util.Set;

public class ManageApiKeysActivity extends AppCompatActivity implements
        ApiKeyDialog.ApiKeyDialogListener, ApiKeyAdapter.ApiKeyAdapterListener {

    private ListView lvApiKeys;
    private Persistent persistent;
    private ApiKeyAdapter apiKeyAdapter;
    private Set<String> apikeys;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_api_keys);
        setTitle(R.string.manageapikeys);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showApiKeyDialog();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        persistent = new Persistent(this, getApplicationContext().getPackageName());

        apikeys = persistent.getStringSet(MainActivity.PREF_API_KEY, new HashSet<String>());
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
        persistent.putStringToSet(MainActivity.PREF_API_KEY, apikey);
        apikeys.add(apikey);
        apiKeyAdapter.notifyDataSetChanged();

        Log.d(MainActivity.TAG, "add apikey: "+apikey);
    }

    @Override
    public void onApiKeyDeleted(String apikey) {
        persistent.removeFromSet(MainActivity.PREF_API_KEY, apikey);
        apikeys.remove(apikey);
        apiKeyAdapter.notifyDataSetChanged();

        Log.d(MainActivity.TAG, "delete apikey: "+apikey);
    }
}
