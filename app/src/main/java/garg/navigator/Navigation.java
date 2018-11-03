package garg.navigator;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class Navigation extends AppCompatActivity {

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

        try {
            Uri uri = Uri.parse(BASE_URL)
                    .buildUpon()
                    .appendQueryParameter("origin", mOrigin)
                    .appendQueryParameter("destination", mDestination)
                    .appendQueryParameter("key", API_KEY)
                    .build();
            String url = uri.toString();
            Log.e("url", url);
            String string = new JsonTask().execute(url).get();
            jsonObject = new JSONObject(string);
            Log.e("string", string);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mTimeToReach.setText("Total time:- " + getDuration(jsonObject));

        listView = (ListView) findViewById(R.id.list);
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(mEmptyStateTextView);
        adapter = new Adapter(Navigation.this, new ArrayList<Model>());
        listView.setAdapter(adapter);
        adapter.addAll(getDirections(jsonObject));

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
        }

        return Html.fromHtml(distance).toString();
    }

    private static ArrayList<Model> getDirections(JSONObject json) {
        ArrayList<Model> ret = new ArrayList<>();
        try {
            JSONArray array = json
                    .getJSONArray("routes").getJSONObject(0)
                    .getJSONArray("legs").getJSONObject(0)
                    .getJSONArray("steps");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject step = array.getJSONObject(i);
                ret.add(new Model(html2text(step.get("html_instructions").toString()), html2text(step.getJSONObject("duration").get("text").toString())));
            }
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    private class JsonTask extends AsyncTask<String, String, String> {
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
            } catch (MalformedURLException e) {
                e.printStackTrace();
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
            try {
                jsonObject = new JSONObject(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
/**
 public class ClientSend extends Thread {

 //@Override
 public void onCreate() {
 // super.onCreate();
 Log.e("service", "started");
 }

 Handler handler = new Handler();
 private Runnable periodicUpdate = new Runnable() {
@Override public void run() {
handler.postDelayed(periodicUpdate, 10*1000 - SystemClock.elapsedRealtime()%1000);
Log.e("run","run");
// TODO whatever you want to do below

}
};

 public void sendMessage(String message) {
 try {
 DatagramSocket udpSocket = new DatagramSocket(23001);
 byte[] buf = message.getBytes();
 for (String ip : mIPAddress) {
 InetAddress serverAddr = InetAddress.getByName(ip);
 DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, 23001);
 udpSocket.send(packet);
 }
 } catch (IOException e) {
 e.printStackTrace();
 }
 }

 private String getLatitude() {
 return String.valueOf(mMyService.location.getLastLocation().getLatitude());
 }

 private String getLongitude() {
 return String.valueOf(mMyService.location.getLastLocation().getLongitude());
 }

 @Nullable
 //@Override
 public IBinder onBind(Intent intent) {
 return null;
 }
 }



 @Override protected void onStart() {
 super.onStart();
 startService(new Intent(this, TrackingService.class));
 doBindService();
 }

 @Override protected void onResume() {
 super.onResume();
 doBindService();
 }

 @Override protected void onPause() {
 super.onPause();
 doUnbindService();
 }

 @Override protected void onStop() {
 super.onStop();
 doUnbindService();
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
 }**/
}
