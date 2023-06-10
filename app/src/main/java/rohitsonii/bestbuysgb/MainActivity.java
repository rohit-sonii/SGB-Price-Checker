package rohitsonii.bestbuysgb;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.makeText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import android.Manifest;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.time.LocalTime;


public class MainActivity extends AppCompatActivity {

    // Declaration of global variables
    private static final String CHANNEL_ID = "sgbNotificationChannel";
    private static final String TAG = "mine";
    private static final Handler handler = new Handler();
    private Runnable runnable = new Runnable(){public void run() {}};
    int delay = 300000; //5 mins
    int forceCheck=0;
    Button startButton;
    Button stopButton;
    Switch toggle;
    int ifStopClicked=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setBackgroundColor(Color.rgb(250,181,181));

        forceCheck=0;
        ifStopClicked=1;
        startButton = findViewById(R.id.button);
        stopButton = findViewById(R.id.button2);
        toggle = findViewById(R.id.sgbCondition);
        startButton.setEnabled(true);
        toggle.setEnabled(true);
        stopButton.setEnabled(false);
    }

    public void onClickStart(View view) {
        Log.d(TAG,"Inside Start - forceCheck value = "+forceCheck);
        if (checkPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            // Permission granted, do desired action

            Log.d(TAG,"Inside Start - Permission Granted");
            //Check if time is in between 9:15 AM and 3:30PM ; only then check the SGB price
            LocalTime currentTime = LocalTime.now();
            LocalTime startTime = LocalTime.of(9, 15);
            LocalTime endTime = LocalTime.of(15, 30);

            boolean isBetween = currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
            if (isBetween) {
                ifStopClicked=0;
                startButton.setEnabled(false);
                toggle.setEnabled(false);
                stopButton.setEnabled(true);
                Log.d(TAG,"Inside Start - Time between valid market time");
                getWindow().getDecorView().setBackgroundColor(Color.rgb(145,250,150));
                checkSgbBestPrice();
            } else {
                Log.d(TAG,"Inside Start - Time outside valid market time");
                makeText(this, "The current time is not between 9:15 AM and 3:30 PM.", Toast.LENGTH_SHORT).show();
                makeText(this, "Double tap to force start the SGB price.", Toast.LENGTH_LONG).show();
                Log.d(TAG,"Inside Start - forceCheck value = "+forceCheck);
                //Check if user clicked twice indicating a force check on SGB Price
                if(forceCheck>0){
                    ifStopClicked=0;
                    Log.d(TAG,"Inside Start - Force Start Checking SGB Price");
                    Log.d(TAG,"Inside Start - forceCheck value = "+forceCheck);
                    startButton.setEnabled(false);
                    toggle.setEnabled(false);
                    stopButton.setEnabled(true);
                    getWindow().getDecorView().setBackgroundColor(Color.rgb(145,250,150));
                    checkSgbBestPrice();
                }
                else {
                    startButton.setEnabled(true);
                    toggle.setEnabled(true);
                    stopButton.setEnabled(false);
                }

                forceCheck++;
            }
        } else {
            Log.d(TAG,"Inside Start - Permission Not Granted");
            // Permission is not granted
            startButton.setEnabled(true);
            toggle.setEnabled(true);
            stopButton.setEnabled(false);
            makeText(this, "Permission to send notification is not provided.", Toast.LENGTH_SHORT).show();
            makeText(this, "Go to Settings -> Permissions -> Notifications to allow.", Toast.LENGTH_SHORT).show();
        }

    }

    private boolean checkPermission(String permission) {
        int result = ContextCompat.checkSelfPermission(this, permission);
        return result == PackageManager.PERMISSION_GRANTED;

    }

    private void checkSgbBestPrice() {
        //Complete code to fetch SGB percentage
        // Use RequestQueue to send request and get raw response.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://sgb.wintwealth.com";
        String TAG = "mine";

        final String[] result = {""};
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {

                    @Override

                    public void onResponse(String response) {
                        // Handle the response. - I have response of website now
                        int index = response.indexOf("highest yield available in the market right now");

                        if (index != -1 && index >= 21) {
                            result[0] = response.substring(index - 21, index);
                        } else if (index != -1 && index < 21) {
                            result[0] = response.substring(0, index);
                        }
                        result[0] = result[0].split("<")[0];
                        Log.d(TAG, "[+] Result: " + result[0]);

                        int duration = LENGTH_LONG;
                        CharSequence finalDisplay = "SGB Available at " + result[0] + "%.";

                        Switch s = (Switch) findViewById(R.id.sgbCondition);
                        if(s.isChecked()){
                            if(Double.parseDouble(result[0]) > 2.5){
                                addNotification((String) finalDisplay);
                            }
                        } else {
                            addNotification((String) finalDisplay);
                        }
                    }
                },
                error -> {
                    // Handle the error.
                    Log.e(TAG, "Error: " + error.toString());
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void addNotification(String result) {
        NotificationManager manager;
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_background) //set icon for notification
                        .setContentTitle("SGB Available") //set title of notification
                        .setContentText(result)//this is notification message
                        .setAutoCancel(true) // makes auto cancel of notification
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT); //set priority of notification


        Intent notificationIntent = new Intent(this, NotificationView.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //notification message will get at NotificationView
        notificationIntent.putExtra("message", "This is a notification message");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        // Add as notification
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = CHANNEL_ID;
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }

        manager.notify(0, builder.build());
        Log.e(TAG, "[-] At the end");

    }

    public void onClickStop(View view) {
        Log.d(TAG,"Inside Stop");
        getWindow().getDecorView().setBackgroundColor(Color.rgb(250,181,181));
        handler.removeCallbacks(runnable);
        startButton.setEnabled(true);
        toggle.setEnabled(true);
        stopButton.setEnabled(false);
        forceCheck=0;
        ifStopClicked=1;

    }
    @Override
    protected void onResume() {
            handler.postDelayed(runnable = new Runnable() {
                @Override
                public void run() {
                    handler.postDelayed(runnable, delay);
                    if(ifStopClicked==0) {
                        getWindow().getDecorView().setBackgroundColor(Color.rgb(145, 250, 150));
                        checkSgbBestPrice();
                    }
                }
            }, delay);
            super.onResume();
    }
}