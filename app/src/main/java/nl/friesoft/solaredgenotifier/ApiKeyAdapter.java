package nl.friesoft.solaredgenotifier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Set;

class ApiKeyAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final ApiKeyAdapterListener listener;
    private Set<String> apikeys;
    private Activity activity;

    public ApiKeyAdapter(Activity _activity, Set<String> _apikeys) {
        mInflater = LayoutInflater.from(_activity);

        listener = (ApiKeyAdapterListener)_activity;
        activity = _activity;
        apikeys = _apikeys;
    }

    @Override
    public int getCount() {
        return apikeys.size();
    }

    @Override
    public String getItem(int i) {
        return (String)apikeys.toArray()[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int listItemId, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mInflater.inflate(R.layout.api_key_item, viewGroup, false);
        }

        ((TextView) view.findViewById(R.id.tvApiKey))
                .setText(getItem(listItemId));

        ((ImageButton) view.findViewById(R.id.ibDelete)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                String msg = String.format(activity.getString(R.string.ays_deleteapikey), getItem(listItemId));

                builder.setTitle(R.string.deletekey).setMessage(msg).
                        setNegativeButton(R.string.close, null).
                        setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                listener.onApiKeyDeleted(getItem(listItemId));
                            }
                        });

                builder.show();
            }
        });

        return view;
    }

    public interface ApiKeyAdapterListener {
        void onApiKeyDeleted(String apikey);
    }
}
