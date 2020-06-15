package web.my.httpee;
/**
 *  HTTPee server  by  https://github.com/Burning-machine    2020 - 2021.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

import static web.my.httpee.MainActivity.showNotification;

public class Breceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification("Server","Closing app",context,true);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
