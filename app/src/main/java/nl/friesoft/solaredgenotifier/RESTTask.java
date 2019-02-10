package nl.friesoft.solaredgenotifier;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by bart on 2/24/16.
 */
public class RESTTask extends AsyncTask<String, String, JSONObject> {
    private String url = null;
    private String verb;
    private RESTListener listener = new DummyListener();
    private String lastError;
    private JSONObject result = null;
    private String task;

    public void setUrl(String url) {
        this.url = url;
    }
    public void setVerb(String verb) {
        this.verb = verb;
    }

    private class DummyListener implements RESTListener {
        @Override
        public void onError(String task, String m) { }

        @Override
        public void onResult(String task, JSONObject r) { }

    }
    public interface RESTListener {
        void onError(String task, String m);
        void onResult(String task, JSONObject r);
    }

    public void setListener(RESTListener listener) {
        if (listener != null) {
            this.listener = listener;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        setSSL();
    }

    @Override
    protected void onPostExecute(JSONObject s) {
        super.onPostExecute(s);

        if (lastError != null) {
            listener.onError(task, lastError);
        } else {
            listener.onResult(task, s);
        }
    }

    private void setSSL() {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Log.e(MainActivity.TAG, "Error: "+e.getMessage());
        }

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    // parameters:
    // strings[0] = a TASK identifier to be able to figure out how to parse the result
    // strings[1] = the path to add to the URL (complete, so could be 'project?q=search')
    // strings[2] = [optional] payload to POST
    @Override
    protected JSONObject doInBackground(String... strings) {
        lastError = null;
        HttpURLConnection conn = null;
        try {
            task = strings[0];
            byte[] payload = null;
            if (url == null) {
                throw new Exception("URL not set.");
            }

            if (!url.endsWith("/")) {
                url += "/";
            }
            url += strings[1];

            URL u = new URL(url);
            conn = (HttpURLConnection)u.openConnection();
            Log.d(MainActivity.TAG, "Connect to "+url);

            // send data if we wanted to POST
            if ("POST".equals(verb)) {
                payload = strings[2].getBytes("UTF-8");
                Log.d(MainActivity.TAG, "Sending payload: "+payload);
                conn.setDoOutput(true);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setFixedLengthStreamingMode(payload.length);

                OutputStream out = new BufferedOutputStream(conn.getOutputStream());
                out.write(payload);
                out.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream in = null;
            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                    in = conn.getInputStream();
                    break;
                default:
                    String m = "Request failed: response code="+responseCode;
                    Log.e(MainActivity.TAG, m);
                    in = conn.getErrorStream();
                    break;
            }

            if (in != null) {
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);
                Log.d(MainActivity.TAG, "Got result: "+responseStrBuilder.toString());
                result = new JSONObject(responseStrBuilder.toString());
            } else {
                throw new Exception("No data received, responseCode="+responseCode);
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            Log.d(MainActivity.TAG, "Request failed: "+lastError);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return result;
    }
}
