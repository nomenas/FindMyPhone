package com.nomenas.findphone;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandErrorType;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandResultCallback;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.pages.FlowPanel;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.WrappedTextBlock;
import com.microsoft.band.tiles.pages.WrappedTextBlockData;
import com.microsoft.band.tiles.pages.WrappedTextBlockFont;

import java.util.Observable;
import java.util.UUID;

public class FindMyPhoneService extends android.app.Service {

    private final IBinder mBinder = new MyBinder();

    private final Observable mObservable = new Observable(){
        @Override
        public boolean hasChanged() { return true;}
    };
    private String mConnectionError;
    private boolean mIsTileOpened = false;

    public class MyBinder extends Binder {
        FindMyPhoneService getService() {
            return FindMyPhoneService.this;
        }
    }

    public enum NotificationType {
        ConnectionChanged,
        TileChanged
    }

    public Observable observable() {
        return mObservable;
    }

    boolean isConnected() {
        return (mClient != null) && (mClient.getConnectionState() == ConnectionState.CONNECTED);
    }

    String connectionError() {
        return mConnectionError;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        connect();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        connect();
    }

    public int hardwareInfo() {
        int hwVersion = 19;

        try {
            hwVersion = Integer.valueOf(mClient.getHardwareVersion().await());
        } catch (Exception ex) {
            // handle InterruptedException
        }

        return hwVersion;
    }

    public void addRemoveTile(Activity activity) {
        new AddRemoveTileTask().execute(activity);
    }

    public void setTileIsOpened(boolean isOpened) {
        mIsTileOpened = isOpened;
        updatePages();
    }

    private class AddRemoveTileTask extends AsyncTask<Activity, Void, Void> {
        @Override
        protected Void doInBackground(Activity... params) {
            try {
                if (doesHasFindMyPhoneTile()) {
                    mClient.getTileManager().removeTile(tileId).registerResultCallback(new BandResultCallback<Boolean>() {
                        @Override
                        public void onResult(Boolean aBoolean, Throwable throwable) {
                            mObservable.notifyObservers(NotificationType.TileChanged);
                        }
                    });
                    mObservable.notifyObservers(NotificationType.TileChanged);
                } else if (hasSpaceForOneMoreTile()) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inScaled = false;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap tileIcon = BitmapFactory.decodeResource(getBaseContext().getResources(), R.raw.b_icon, options);

                    BandTile tile = new BandTile.Builder(tileId, "Find My Phone", tileIcon)
                            .setPageLayouts(createButtonLayout())
                            .build();

                    mClient.getTileManager().addTile(params[0], tile).registerResultCallback(new BandResultCallback<Boolean>() {
                        @Override
                        public void onResult(Boolean aBoolean, Throwable throwable) {
                            mObservable.notifyObservers(NotificationType.TileChanged);
                            updatePages();
                        }
                    });
                }
                else {
                    Toast.makeText(params[0], "There is no room for one more tile. Please make space on your band and try to add tile again!",
                            Toast.LENGTH_LONG).show();
                }
            }
            catch (Exception ex) {
                mObservable.notifyObservers(NotificationType.TileChanged);
            }

            return null;
        }
    }

    private class ConnectBand extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            if (!isConnected()) {
                try {

                    if (mClient == null) {
                        BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
                        if (devices.length != 0) {
                            mClient = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
                        } else {
                            mConnectionError = "Your droid is not paired with any band!";
                        }
                    }


                    BandClient client = mClient;
                    if (client != null && ConnectionState.CONNECTED != client.getConnectionState()) {
                        client.connect().await();
                        updatePages();

                        if (ConnectionState.CONNECTED != client.getConnectionState()) {
                            mConnectionError = "Unable to connect!";
                        }
                    }

                    if (mClient == null) {
                        if (client != null) {
                            mConnectionError = "Please make sure bluetooth is on and the band is in range.";
                            client.disconnect();
                        } else {
                            mConnectionError = "Client is not valid";
                        }
                    } else if (client.getConnectionState() != ConnectionState.CONNECTED) {
                        mConnectionError = "Please make sure bluetooth is on and the band is in range.";
                    }
                } catch (BandException e) {
                    switch (e.getErrorType()) {
                        case DEVICE_ERROR:
                            mConnectionError = "Please make sure bluetooth is on and the band is in range.";
                            break;
                        case UNSUPPORTED_SDK_VERSION_ERROR:
                            mConnectionError = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                            break;
                        case SERVICE_ERROR:
                            mConnectionError = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                            break;
                        default:
                            mConnectionError = "Error: " + e.getErrorType().toString();
                            break;
                    }
                } catch (Exception e) {
                    mConnectionError = "Please make sure bluetooth is on and the band is in range.";
                }
            }

            mObservable.notifyObservers(NotificationType.ConnectionChanged);

            return null;
        }
    }

    private void connect() {
        mConnectionError = null;
        new ConnectBand().execute();

    }
    private void disconnect() {
        if (mClient != null) {
            mClient.disconnect();
            mClient = null;

            mObservable.notifyObservers(NotificationType.ConnectionChanged);
        }
    }

    public boolean doesHasFindMyPhoneTile() {
        try {
            for (BandTile tile : mClient.getTileManager().getTiles().await()) {
                if (tile.getTileId().equals(tileId)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            // do nothing
        }

        return false;

    }
    private static BandClient mClient = null;
    private static final UUID tileId = UUID.fromString("568e24c6-b924-4511-a6a9-a906298294d7");
    private static final UUID pageId1 = UUID.fromString("665d1b14-436b-40ae-ab8d-59806881f31c");


    private PageLayout createButtonLayout() {
        return new PageLayout(
                new FlowPanel(15, 0, 260, 105, FlowPanelOrientation.VERTICAL)
                        .addElements(new WrappedTextBlock(0, 5, 210, 90, WrappedTextBlockFont.SMALL).setMargins(0, 5, 0, 0).setId(12))
        );
    }

    public void updatePages() {
        try {
            String message = mIsTileOpened ? "Phone is ringing, FIND IT!" : "Connecting ...";
            mClient.getTileManager().setPages(tileId,
                    new PageData(pageId1, 0).update(new WrappedTextBlockData(12, message))).await();
        } catch (Exception ex) {
            // do nothing
        }
    }

    private boolean hasSpaceForOneMoreTile() {
        boolean returnValue = false;

        try {
            returnValue = mClient.getTileManager().getRemainingTileCapacity().await() > 0;
        } catch (Exception e) {
            // do nothing
        }

        return returnValue;
    }
}
