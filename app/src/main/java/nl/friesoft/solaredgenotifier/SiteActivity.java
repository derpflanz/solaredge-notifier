package nl.friesoft.solaredgenotifier;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SiteActivity extends AppCompatActivity implements ISolarEdgeListener {

    private TextView tvInstallName;
    private TextView tvEnergyYesterday;
    private TextView tvAvgLastWeek;
    private TextView tvApiId;

    private String apikey;
    private int installId;
    private int reason;
    private ImageView ivStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvApiId = findViewById(R.id.tvApiId);
        tvInstallName = findViewById(R.id.tvInstallName);
        tvEnergyYesterday = findViewById(R.id.tvEnergyYesterday);
        tvAvgLastWeek = findViewById(R.id.tvAvgLastWeek);
        ivStatus = findViewById(R.id.ivStatus);

        Intent i = getIntent();
        Bundle b = i.getExtras();
        if (b != null) {
            apikey = b.getString(AlarmReceiver.EXTRA_API_KEY);
            installId = b.getInt(AlarmReceiver.EXTRA_INSTALLATION_ID);
            reason = b.getInt(AlarmReceiver.EXTRA_REASON);

            if (reason == AlarmReceiver.REASON_BELOWFIXED) {
                ivStatus.setImageResource(R.drawable.outline_wb_cloudy_black_48);
            } else {
                ivStatus.setImageResource(R.drawable.outline_wb_sunny_black_48);
            }

            tvApiId.setText(String.format("API %s / ID %d", apikey, installId));

            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);

            Calendar aweekago = Calendar.getInstance();
            aweekago.add(Calendar.DATE, -7);

            SolarEdge edge = new SolarEdge(this);
            Site s = new Site(apikey, installId);
            edge.details(s);
            edge.energy(s, aweekago.getTime(), yesterday.getTime());
        }
    }

    @Override
    public void onSiteFound(Site site) {
        // never called, we don't call sites()
    }

    @Override
    public void onError(Site site, SolarEdgeException exception) {

    }

    @Override
    public void onEnergy(Site site, Energy result) {
        if (site.getId() == installId) {
            tvEnergyYesterday.setText(Energy.format(result.getDailyEnergy(-1)));
            tvAvgLastWeek.setText(Energy.format(result.getAverageEnergy()));
        }
    }

    @Override
    public void onDetails(Site site) {
        tvInstallName.setText(site.getName());
    }
}
