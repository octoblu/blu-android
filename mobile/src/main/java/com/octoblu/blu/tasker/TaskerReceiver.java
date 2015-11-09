package com.octoblu.blu.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.octoblu.blu.TriggerService;
import com.octoblu.blu.tasker.TaskerIntent;

/**
 * Created by redaphid on 11/8/15.
 */
public class TaskerReceiver extends BroadcastReceiver {
    private static final String TAG = "TaskerReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent bluIntent;
        switch (intent.getAction()){
            case TaskerIntent.ACTION_FIRE_SETTING:
                bluIntent = getBluIntent(intent);
                onFireTriggerMessage(context, bluIntent);
            break;
        }
    }

    private Intent getBluIntent(Intent intent) {
        Log.d(TAG, "MADE IT!");
        return intent;
    }

    private void onFireTriggerMessage(Context context, Intent intent) {
        context.startService(intent);
    }
}
