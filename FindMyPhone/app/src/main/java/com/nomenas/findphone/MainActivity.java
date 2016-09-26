package com.nomenas.findphone;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity {

    private FindMyPhoneService mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = ((FindMyPhoneService.MyBinder) binder).getService();
            if (mService != null) {
                if (mService.connectionError() != null || mService.isConnected()) {
                    updateStatus();
                }
                mService.observable().addObserver(mFindMyPhoneServiceObserver);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (mService != null) {
                mService.observable().deleteObserver(mFindMyPhoneServiceObserver);
                mService = null;
            }
        }
    };

    private Observer mFindMyPhoneServiceObserver = new Observer() {
        @Override
        public void update(Observable observable, Object data) {
            if (mService != null && observable == mService.observable()) {
                if (data == FindMyPhoneService.NotificationType.ConnectionChanged) {
                    updateStatus();

                    if (mService != null && mService.isConnected() && mService.hardwareInfo() > 19) {
                        updateImage(R.drawable.band2);
                    }
                } else if (data == FindMyPhoneService.NotificationType.TileChanged) {
                    updateAddTileButton();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent= new Intent(this, FindMyPhoneService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        if (mService != null && mService.isConnected()) {
            updateStatus();
        } else {
            Button addTileButton = (Button) findViewById(R.id.btnIAddRemoveTile);
            addTileButton.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    private void updateStatus() {
        if (mService != null) {
            if (mService.isConnected()) {
                updateStatus("Connected", "");
            } else {
                updateStatus("Disconnected", mService.connectionError());
            }
        } else {
            updateStatus("Disconnected", "Service Unavalible!");
        }

        updateAddTileButton();
    }

    private void updateAddTileButton() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button addTileButton = (Button) findViewById(R.id.btnIAddRemoveTile);
                addTileButton.setEnabled(true);
                addTileButton.setVisibility(View.VISIBLE);

                if (mService == null || (mService != null && mService.connectionError() != null)) {
                    addTileButton.setVisibility(View.INVISIBLE);
                } else if (!mService.isConnected()) {
                    addTileButton.setText("Wait For Moment ...");
                    addTileButton.setEnabled(false);
                } else if (mService.doesHasFindMyPhoneTile()) {
                    addTileButton.setText("Remove 'Find My Phone' tile on my band");
                } else {
                    addTileButton.setText("Add 'Find My Phone' tile on my band");
                }
            }
        });
    }

    public void sendFeedback(View v) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:naum.puroski@gmail.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for FindMyPhone");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        try {
            startActivity(Intent.createChooser(emailIntent, "Send Feedback:"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public void buyMeABeer(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=8W8ZZTKVWELH6"));
        startActivity(browserIntent);
    }

    public void addRemoveTile(View v) {
        Button addTileButton = (Button) findViewById(R.id.btnIAddRemoveTile);
        addTileButton.setText("Wait For Moment ...");
        addTileButton.setEnabled(false);

        if (mService != null) {
            mService.addRemoveTile(this);
        }
    }

    private void updateStatus(final String status, final String description) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView statusLabel = (TextView) findViewById(R.id.lblStatus);
                statusLabel.setText(status);
                TextView descriptionLabel = (TextView) findViewById(R.id.lblDescription);
                descriptionLabel.setText(description);
            }
        });
    }

    private void updateImage(final int id) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                imageView.setImageResource(id);
            }
        });
    }
}
