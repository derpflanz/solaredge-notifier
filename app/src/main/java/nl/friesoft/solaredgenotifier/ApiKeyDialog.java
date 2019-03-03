package nl.friesoft.solaredgenotifier;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ApiKeyDialog extends DialogFragment {
    public static final String TAG = "dialogapikey";
    private ApiKeyCallbacks listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.addapikey);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_add_api_key, null);

        builder.setView(view);

        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                TextView etApiKey = view.findViewById(R.id.etApiKey);
                listener.onApiKeyAdded(etApiKey.getText().toString());
            }
        });
        builder.setNegativeButton(R.string.close, null);

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (ApiKeyCallbacks)context;
    }
}
