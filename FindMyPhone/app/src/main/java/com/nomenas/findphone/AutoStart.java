package com.nomenas.findphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, FindMyPhoneService.class));
    }
}
