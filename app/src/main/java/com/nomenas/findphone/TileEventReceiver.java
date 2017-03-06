package com.nomenas.findphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TileEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("com.microsoft.band.action.ACTION_TILE_OPENED")) {
            Intent intents = new Intent(context, RingActivity.class);
            intents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intents);
        }
        else if (intent.getAction().equals("com.microsoft.band.action.ACTION_TILE_CLOSED")) {
            if (RingActivity.instance != null) {
                RingActivity.instance.finish();
            }
        }
    }
}