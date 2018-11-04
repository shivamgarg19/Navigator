package garg.navigator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashSet;


public class MainActivity extends AppCompatActivity {

    private Button mNavigationButton;
    private EditText mDestination;
    private TextView mDeviceConnected;
//    private MyLocation mLocation = new MyLocation();
    private String mStartingPoint;
    private MyDatagramReceiver myDatagramReceiver = null;
    TrackingService mMyService;
    HashSet<String> mIPAddresses = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationButton = (Button) findViewById(R.id.navigate);
        mDestination = (EditText) findViewById(R.id.destination);
        mDeviceConnected = (TextView) findViewById(R.id.device_connected);

        /**
         MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
        @Override public void gotLocation(Location location) {
        mStartingPoint = String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
        Log.e("location", String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude()));
        }
        };
         mLocation.getLocation(this, locationResult);
         **/

        mNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Destination = mDestination.getText().toString();
                mStartingPoint = String.valueOf(mMyService.location.getLastLocation().getLatitude()) + "," + String.valueOf(mMyService.location.getLastLocation().getLongitude());
                if (Destination.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please enter the destination", Toast.LENGTH_SHORT).show();
                } else if (!checkConnectivity()) {
                    Toast.makeText(MainActivity.this, "No Internet connection available", Toast.LENGTH_SHORT).show();
                } else if (mStartingPoint == null) {
                    Toast.makeText(MainActivity.this, "Unable to fetch Current location", Toast.LENGTH_SHORT).show();
                } else {
                    Intent i = new Intent(MainActivity.this, Navigation.class);
                    i.putExtra("Destination", Destination);
                    i.putExtra("Origin", mStartingPoint);
                    i.putExtra("IPAddress", new ArrayList<String>(mIPAddresses));
                    startActivity(i);
                }
            }
        });
    }

    public boolean checkConnectivity() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, TrackingService.class));
        doBindService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myDatagramReceiver = new MyDatagramReceiver();
        myDatagramReceiver.start();
        doBindService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myDatagramReceiver.kill();
        doUnbindService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        myDatagramReceiver.kill();
        doUnbindService();
    }

    private Runnable updateTextMessage = new Runnable() {
        public void run() {
            if (myDatagramReceiver == null) return;
            //textMessage.setText(myDatagramReceiver.getLastMessage());
        }
    };

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



    private class MyDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";

        public void run() {
            Log.i("broadcast-listener", "udp listener started");
            String message;
            byte[] lmessage = new byte[10000];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(23001);

                while (bKeepRunning) {
                    socket.receive(packet);
                    message = new String(lmessage, 0, packet.getLength());
                    String IPAddress = packet.getAddress().getHostAddress();
                    //runOnUiThread(updateTextMessage);
                    Log.i("boardcast-listener", message);

                    byte[] reply = "ack".getBytes();
                    socket.send(new DatagramPacket(reply, reply.length, packet.getSocketAddress()));

                    mIPAddresses.add(IPAddress);
                    mDeviceConnected.setText(String.format("Device Connected: %d", mIPAddresses.size()));
                    Log.i("boardcast-listener", "Added address: " + IPAddress);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (socket != null) {
                socket.close();
            }
        }

        public void kill() {
            bKeepRunning = false;
            Thread.currentThread().interrupt();
        }

        public String getLastMessage() {
            return lastMessage;
        }
    }
}
