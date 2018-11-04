package garg.navigator;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

interface JsonCallback {
    void jsonDataCallback(String result);
}

public class Navigation extends AppCompatActivity implements JsonCallback {

    private String mDestination, mOrigin;
    private TextView mDestinationText, mTimeToReach, mEmptyStateTextView;
    private String BASE_URL = "https://maps.googleapis.com/maps/api/directions/json?",
            API_KEY = "AIzaSyCP0gezR6_JdbV2FphQIaQCRPQ2oOLaVqk";
    ProgressDialog pd;
    private JSONObject jsonObject;
    private Adapter adapter;
    private ListView listView;
    TrackingService mMyService;
    private ArrayList<String> mIPAddress;
    ArrayList<Model> mDirections;
    int currentDirectionIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        Intent i = getIntent();
        mDestination = i.getStringExtra("Destination");
        mOrigin = i.getStringExtra("Origin");
        mIPAddress = i.getStringArrayListExtra("IPAddress");
        Log.e("origin", mOrigin);
        mDestinationText = (TextView) findViewById(R.id.text_destination);
        mTimeToReach = (TextView) findViewById(R.id.time_to_reach);
        mDestinationText.setText("Destination:- " + mDestination);
        Uri uri = Uri.parse(BASE_URL)
                .buildUpon()
                .appendQueryParameter("origin", mOrigin)
                .appendQueryParameter("destination", mDestination)
                .appendQueryParameter("key", API_KEY)
                .build();
        String url = uri.toString();
        Log.e("url", url);

        AsyncTask task = new JsonTask(this).execute(url);
        // Use below for testing
        //AsyncTask task = new JsonTask(this).execute("https://pastebin.com/raw/a5nkgkjm");

        doBindService();
    }

    private void checkLocation() {
        Thread t = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(10000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Location currentLocation = mMyService.location.getLastLocation();
                                Log.e("location", currentLocation.toString());
                                if (currentLocation.distanceTo(mDirections.get(currentDirectionIndex + 1).getLocation()) < 20) {
                                    currentDirectionIndex++;
                                    Toast.makeText(Navigation.this, "Reached naer destination", Toast.LENGTH_SHORT).show();
                                    for (String ip : mIPAddress) {
                                        new UpdateTask().execute("http://" + ip + "/update", mDirections.get(currentDirectionIndex).getDirection().toString());
                                    }
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        t.start();
    }

    private static String getDuration(JSONObject json) {
        String distance = "unknown";
        try {
            distance = (String) json
                    .getJSONArray("routes").getJSONObject(0)
                    .getJSONArray("legs").getJSONObject(0)
                    .getJSONObject("duration")
                    .get("text");
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }

        return Html.fromHtml(distance).toString();
    }

    private static ArrayList<Model> getDirections(JSONObject json) {
        ArrayList<Model> ret = new ArrayList<>();
        try {
            Location location = new Location("location");
            JSONArray array = json
                    .getJSONArray("routes").getJSONObject(0)
                    .getJSONArray("legs").getJSONObject(0)
                    .getJSONArray("steps");

            for (int i = 0; i < array.length(); ++i) {
                JSONObject step = array.getJSONObject(i);
                location.setLatitude((Double) step.getJSONObject("end_location").get("lat"));
                location.setLongitude((Double) step.getJSONObject("end_location").get("lat"));
                Log.e("loc", location.toString());
                ret.add(new Model(html2text(step.get("html_instructions").toString()),
                        html2text(step.getJSONObject("duration").get("text").toString()),
                        Directions.fromString(step.optString("maneuver", "straight")),
                        location
                ));

            }
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    public void jsonDataCallback(String result) {
        if (result == null)
            return;

        try {
            jsonObject = new JSONObject(result);
            mDirections = getDirections(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("string", result);

        mTimeToReach.setText("Total time:- " + getDuration(jsonObject));
        listView = (ListView) findViewById(R.id.list);
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(mEmptyStateTextView);

        adapter = new Adapter(Navigation.this, new ArrayList<Model>());
        listView.setAdapter(adapter);
        adapter.addAll(mDirections);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Model m = (Model) listView.getItemAtPosition(position);
                for (String ip : mIPAddress) {
                    new UpdateTask().execute("http://" + ip + "/update", m.getDirection().toString());
                }
            }
        });
        checkLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Thread.interrupted();
        //t.interrupted();
    }

    private class JsonTask extends AsyncTask<String, String, String> {
        private JsonCallback cb;

        public JsonTask(JsonCallback callback) {
            cb = callback;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(Navigation.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                    //Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)
                }
                return buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()) {
                pd.dismiss();
            }
            cb.jsonDataCallback(result);
        }
    }

    private class UpdateTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                Log.i("update-task", params[0]);
                URL url = new URL(params[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                //urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("maneuver", params[1]);

                String query = builder.build().getEncodedQuery();
                Log.i("update-task", query);
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();


                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                Log.i("update-task", "response code" + responseCode);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            return "ok";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null && s.equals("ok")) {
                Toast.makeText(Navigation.this, "Sent update to device", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Navigation.this, "Failed to send update to device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mMyService = ((TrackingService.ServiceBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mMyService = null;
        }
    };

    void doBindService() {
        bindService(new Intent(this, TrackingService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        // Detach our existing connection.
        //        unbindService(mConnection);
    }

}
