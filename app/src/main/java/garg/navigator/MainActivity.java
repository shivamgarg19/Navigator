package garg.navigator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
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

        mNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Destination = mDestination.getText().toString();

                if (mMyService.location != null) {
                    Location location = mMyService.location.getLastLocation();
                    mStartingPoint = String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
                }
                if (Destination.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please enter the destination", Toast.LENGTH_SHORT).show();
                } else if (!checkConnectivity()) {
                    Toast.makeText(MainActivity.this, "No Internet connection available", Toast.LENGTH_SHORT).show();
                } else if (mStartingPoint == null) {
                    Toast.makeText(MainActivity.this, "Unable to fetch Current location. Please try again", Toast.LENGTH_SHORT).show();
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
        myDatagramReceiver = new MyDatagramReceiver();
        myDatagramReceiver.start();
        doBindService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        doBindService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        doUnbindService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        myDatagramReceiver.kill();
        doUnbindService();
        stopService(new Intent(this, TrackingService.class));
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


    private class MyDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";

        public void run() {
            Log.i("broadcast-listener", "udp listener started");
            String message;
            DatagramSocket socket = null;
            byte[] lmessage = new byte[10000];
            try {
                socket = new DatagramSocket(23001);
                while (bKeepRunning) {
                    Log.i("broadcast-listener", "waiting for broadcast");
                    DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);
                    socket.receive(packet);
                    message = new String(lmessage, 0, packet.getLength());
                    String IPAddress = packet.getAddress().getHostAddress();
                    //runOnUiThread(updateTextMessage);
                    Log.i("boardcast-listener", message);

                    byte[] reply = "ack".getBytes();
                    socket.send(new DatagramPacket(reply, reply.length, packet.getSocketAddress()));

                    boolean added = mIPAddresses.add(IPAddress);
                    if (added) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDeviceConnected.setText(String.format("Device Connected: %d", mIPAddresses.size()));
                            }
                        });
                    }
                    Log.i("broadcast-listener", "Added: " + added);
                    Log.i("boardcast-listener", "Address: " + IPAddress);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (socket != null) {
                socket.close();
            }

        }

        public void kill() {
            Log.i("broadcast-listener", "killed");
            bKeepRunning = false;
            Thread.currentThread().interrupt();
        }
    }
}
