package com.nomenas.findphone;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

import static android.view.WindowManager.*;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class RingActivity extends AppCompatActivity {

    private View mContentView;
    public static RingActivity instance;

    private int mRingingMode = -1;
    private int mStreamVolume = -1;
    private Vibrator mVibrator = null;
    private Ringtone mRingtone = null;
    private MediaPlayer mMediaPlayer = null;

    private FindMyPhoneService mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = ((FindMyPhoneService.MyBinder) binder).getService();
            if (mService != null) {
                mService.setTileIsOpened(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (mService != null) {
                mService = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;
        setContentView(R.layout.activity_ring);

        // Set up the user interaction to manually show or hide the system UI.
        findViewById(R.id.fullscreen_content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON |
                LayoutParams.FLAG_DISMISS_KEYGUARD |
                LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                LayoutParams.FLAG_TURN_SCREEN_ON);

        LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(layout);

        Intent serviceIntent= new Intent(getBaseContext(), FindMyPhoneService.class);
        getBaseContext().bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        startAlarm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
        instance = null;

        if (mService != null) {
            mService.setTileIsOpened(false);
        }
        getBaseContext().unbindService(mConnection);
    }


    private void startAlarm() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);

        mRingingMode = audioManager.getRingerMode();
        mStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);

        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0);

        mRingtone = RingtoneManager.getRingtone(getApplicationContext(),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        if (mRingtone == null) {
            mRingtone = RingtoneManager.getRingtone(getApplicationContext(),
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        }
        if (mRingtone != null) {
            mRingtone.play();
        } else {
            mMediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.ringtone);
            if (mMediaPlayer != null) {
                mMediaPlayer.start();
            }
        }

        // Get instance of Vibrator from current Context
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (mVibrator != null) {
            long[] pattern = {0, 100, 100};
            mVibrator.vibrate(pattern, 0);
        }

        Animation animation = new AlphaAnimation(1, 0); // Change alpha
        animation.setDuration(500); // duration - half a second
        animation.setInterpolator(new LinearInterpolator()); // do not alter
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation
        animation.setRepeatMode(Animation.REVERSE); // Reverse animation at

        findViewById(R.id.fullscreen_content).startAnimation(animation);
    }

    private void stopAlarm() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        audioManager.setRingerMode(mRingingMode);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, mStreamVolume, 0);

        mVibrator.cancel();

        if(mRingtone != null) {
            mRingtone.stop();
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }

    }
}
