package nl.friesoft.solaredgenotifier;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;

public class InstallationActivity extends AppCompatActivity implements ISolarEdgeListener {

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

            if (reason == AlarmReceiver.REASON_ERROR) {
                ivStatus.setImageResource(R.drawable.outline_wb_cloudy_black_48);
            } else {
                ivStatus.setImageResource(R.drawable.outline_wb_sunny_black_48);
            }

            tvApiId.setText(String.format("API %s / ID %d", apikey, installId));

            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);

            Calendar aweekago = Calendar.getInstance();
            aweekago.add(Calendar.DATE, -7);

            SolarEdge edge = new SolarEdge(this, apikey);
            edge.info(installId);
            edge.energy(installId, aweekago.getTime(), yesterday.getTime());
        }
    }

    @Override
    public void onSiteFound(SolarEdge solarEdge) {
        // never called, we don't call sites()
    }

    @Override
    public void onError(SolarEdge solarEdge, SolarEdgeException exception) {

    }

    @Override
    public void onEnergy(SolarEdge solarEdge, SolarEdgeEnergy result) {
        if (solarEdge.getInfo().getId() == installId) {
            tvEnergyYesterday.setText(SolarEdgeEnergy.format(result.getLastEnergy()));
            tvAvgLastWeek.setText(SolarEdgeEnergy.format(result.getAverageEnergy()));
        }
    }

    @Override
    public void onInfo(SolarEdge solarEdge) {
        tvInstallName.setText(solarEdge.getInfo().getName());
    }
}
