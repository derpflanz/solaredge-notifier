package nl.friesoft.solaredgenotifier;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;

public class InstallationActivity extends AppCompatActivity implements ISolarEdgeListener {

    private TextView tvApiKey;
    private TextView tvInstallId;

    private String apikey;
    private int installId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installation);

        tvApiKey = findViewById(R.id.tvAPI);
        tvInstallId = findViewById(R.id.tvInstallId);

        Intent i = getIntent();
        Bundle b = i.getExtras();
        if (b != null) {
            apikey = b.getString(AlarmReceiver.EXTRA_API_KEY);
            tvApiKey.setText(apikey);

            installId = b.getInt(AlarmReceiver.EXTRA_INSTALLATION_ID);
            tvInstallId.setText(Integer.toString(installId));

            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);

            Calendar aweekago = Calendar.getInstance();
            aweekago.add(Calendar.DATE, -7);

            SolarEdge edge = new SolarEdge(this, apikey);
            edge.energy(installId, aweekago.getTime(), yesterday.getTime());
        }
    }

    @Override
    public void onSites(SolarEdge solarEdge) {

    }

    @Override
    public void onError(SolarEdge solarEdge, SolarEdgeException exception) {

    }

    @Override
    public void onEnergy(SolarEdge solarEdge, SolarEdgeEnergy result) {
        TextView tvYesterday = findViewById(R.id.tvYesterday);
        tvYesterday.setText(result.getFormattedEnergy());
    }
}
