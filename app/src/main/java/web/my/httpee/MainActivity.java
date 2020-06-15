package web.my.httpee;
/**
 *  HTTPee server  by  https://github.com/Burning-machine    2020 - 2021.
 */

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity
{
    public static final int MULTIPLE_PERMISSIONS = 12;
    //Folder name containing host files on the root of the Sdcard/phone storage
    private static final String FOLDERNAME = "HTTPee";
    private WebServer server = null;
    private TextView v1 =null;
    private String[] permissions;
    public int HTTP_port;
    //Supported formats
    public String[] formats ={".md",".png",".mp4",".manifest",".js",".css",".html",""};
    public Button start = null;
    public Button stop = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        //HTTPee port used to start the server
        HTTP_port = 8080;
        //required permissions
        permissions= new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        v1 = findViewById(R.id.text1);
        checkPermissions();
        rootexec();
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!(Utils.getIPAddress(true).equals(""))) {
                    if (server != null) {
                        if (server.isAlive())
                            server.stop();
                        start();
                        showNotification("Server Started","Running on "+Utils.getIPAddress(true), MainActivity.this,false);
                    } else {
                        if (server == null) start();
                        showNotification("Server Started","Running on "+Utils.getIPAddress(true), MainActivity.this,false);
                    }
                }
                else Toast.makeText(MainActivity.this,"Turn on Hotspot/Connection", Integer.parseInt("4000")).show();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(server!=null ){
                    if(server.isAlive())
                    server.stop();
                    v1.setText("HTTP IP : "+"\t\t\t"+" Stopped");
                    showNotification("Server Stopped","HTTPee Server Stopped ", MainActivity.this,false);
                    Log.d("HTTPee","stopped :)");
                }
            }
        });
    }


    public void start(){
            Log.d("HTTPee", "Current ip address is : " + "HTTP://"+Utils.getIPAddress(true));
            v1.setText("HTTP IP :"+"\t\t\t"+Utils.getIPAddress(true));
        Thread HTTPeethread = new Thread(new Runnable(){
            @Override
            public void run(){
                Looper.prepare();
                server = new WebServer();
                try {
                    server.start();
                } catch (IOException ioe) {
                    Log.w("MainActivity", "The server could not start.");
                }
            }
        });
        HTTPeethread.start();
            Log.w("MainActivity", "Web server initialized.");
    }

    public String isSupported(String uri){
        for (int i=0;i<formats.length;i++) {
            if (uri.endsWith(formats[i])) return formats[i];
        }
        return null;
    }

    private class WebServer extends NanoHTTPD {
        public WebServer() {
            super(HTTP_port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String answer = "";
            FileInputStream fis = null;
            String uri = session.getUri();
            Response res = null;
            if(uri.equals("/")) uri="/index.html";
            String format = isSupported(uri);
            File root = Environment.getExternalStorageDirectory();
            String path = root.getAbsolutePath()+"/"+FOLDERNAME+uri;
            File f = new File(path);
            try {
                fis = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
                Log.d("HTTPee",uri+" File not found");
            }
            try {
                switch(format) {
                    case (".html"):
                        res = newChunkedResponse(Response.Status.OK, "text/html", fis);
                        break;
                    case (".css"):
                        res = newChunkedResponse(Response.Status.OK, "text/css", fis);
                        break;
                    case (".manifest"):
                        res = newChunkedResponse(Response.Status.OK,"text/cache-manifest",fis);
                        break;
                    case (".png"):
                        res = newChunkedResponse(Response.Status.OK, "image/png", fis);
                        break;
                    case (".mp4"):
                        res = newChunkedResponse(Response.Status.OK, "video/mp4", fis);
                        break;
                    default:
                        res = newFixedLengthResponse(Response.Status.OK, " application/octet-stream", fis,f.length());
                        break;
                }
                Log.d("HTTPee",uri+" requested");
            } catch (Exception e) {
                e.printStackTrace();
            }
                return res;
        }
        }

        public void rootexec(){
        try {
            Runtime.getRuntime().exec("su");
            //Bypassing Android's < 1024 port restrictions to run on port 80
            Runtime.getRuntime().exec("su -c iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port "+HTTP_port);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("App","Failed to get root");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void showNotification(String title, String message, Context ctx,Boolean t) {
        NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("CUSTOM_CHANNEL",
                    "CHANNEL_UPDATE",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("YOUR_NOTIFICATION_CHANNEL_DESCRIPTION");
            mNotificationManager.createNotificationChannel(channel);
        }
        Intent broadcast = new Intent(ctx,Breceiver.class);
        PendingIntent actionIntent = PendingIntent.getBroadcast(ctx,0,broadcast,PendingIntent.FLAG_ONE_SHOT);
        Notification.Builder mBuilder = new Notification.Builder(ctx, "CUSTOM_CHANNEL");
        mBuilder.setSmallIcon(R.drawable.ic_http) // notification icon
                .setContentTitle(title) // title for notification
                .setContentText(message)// message for notification
                .setContentIntent(actionIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_http, "Close", actionIntent);
        //Closing app
        if (t) mBuilder.setTimeoutAfter(2);

        Notification noti =  mBuilder.build();
        mNotificationManager.notify(0,noti);
    }

    private  void checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
        }
    }
}