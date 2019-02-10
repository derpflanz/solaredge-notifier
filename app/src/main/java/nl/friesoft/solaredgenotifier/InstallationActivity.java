package nl.friesoft.solaredgenotifier;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class InstallationActivity extends AppCompatActivity {

    private TextView tvApiKey;
    private TextView tvInstallId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installation);

        tvApiKey = findViewById(R.id.tvAPI);
        tvInstallId = findViewById(R.id.tvInstallId);

        Intent i = getIntent();
        Bundle b = i.getExtras();
        if (b != null) {
            tvApiKey.setText(b.getString(AlarmReceiver.EXTRA_API_KEY));
            tvInstallId.setText(Integer.toString(b.getInt(AlarmReceiver.EXTRA_INSTALLATION_ID)));
        }
    }

}
