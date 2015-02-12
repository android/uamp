package com.example.android.uamp.hardware;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.SeekBar;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.utils.LogHelper;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DemoKitService extends Service {
    private static final String TAG = LogHelper.makeLogTag(DemoKitService.class);

    public static final String EXTRA_USB_ACCESSORY =
        "com.example.android.uamp.hardware.USB_ACCESSORY";

    private DemoKitManager mManager;
    private MediaBrowser mMediaBrowser;
    private MediaController mMediaController;
    private PlaybackState mState;
    private ScheduledExecutorService mScheduler;
    private ScheduledFuture<?> mScheduledFuture;

    private long mCurrentMusicDuration = -1;

    private byte mCurrentLed = 0;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                Log.d(TAG, "Accessory detached");
                stopSelf();
            }
        }
    };

    public DemoKitService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = new DemoKitManager(this);
        mManager.setListener(new DemoKitManager.Listener() {
            @Override
            public void handleJoyMessage(DemoKitManager.JoyMsg j) {

            }

            @Override
            public void handleLightMessage(DemoKitManager.LightMsg l) {

            }

            @Override
            public void handleTemperatureMessage(DemoKitManager.TemperatureMsg t) {

            }

            @Override
            public void handleSwitchMessage(DemoKitManager.SwitchMsg o) {
                if (o.getState() != 0 && mMediaController != null) {
                    if (mState == null || mState.getState() != PlaybackState.STATE_PLAYING) {
                        mMediaController.getTransportControls().play();
                    } else {
                        mMediaController.getTransportControls().pause();
                    }
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogHelper.d(TAG, "onStartCommand: intent=", intent);
        LogHelper.d(TAG, "onStartCommand: extras=", intent==null?"null":intent.getExtras());
        if (intent != null && intent.getParcelableExtra(EXTRA_USB_ACCESSORY) != null) {
            UsbAccessory accessory = intent.getParcelableExtra(EXTRA_USB_ACCESSORY);
            LogHelper.d(TAG, "got Accessory! accessory=", accessory);
            mManager.openAccessory(accessory);
            startForegroundService();
            connectToMediaSession();
        } else {
            LogHelper.w(TAG, "Ignoring call without an accessory");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startForegroundService() {
        Notification notification = new Notification.Builder(this)
            .setContentTitle("USB Accessory kit connected")
            .build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.d(TAG, "Destroying service");
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaCallback);
        }
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
        }
        if (mScheduler != null && !mScheduler.isShutdown()) {
            mScheduler.shutdownNow();
        }
        mManager.setListener(null);
        mManager.closeAccessory();
        unregisterReceiver(mUsbReceiver);
    }

    private void connectToMediaSession() {
        mMediaBrowser = new MediaBrowser(this,
            new ComponentName(this, MusicService.class),
            mConnectionCallback, null);
        mMediaBrowser.connect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void startPositionTrackerTask() {
        if (mScheduler == null || mScheduler.isShutdown()) {
            mScheduler = Executors.newSingleThreadScheduledExecutor();
        }
        if (mScheduledFuture != null) {
            mScheduledFuture.cancel(true);
        }

        mScheduledFuture = mScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(TAG, "positionTrackerTask, ",
                    " mState = ", mState, " mCurrentMusicDuration=", mCurrentMusicDuration);
                // cycle over LEDs:
                mCurrentLed = (byte) ((mCurrentLed + 1) % 3);
                setLed();

                if (mState != null && mState.getState() == PlaybackState.STATE_PAUSED) {
                    return;
                }

                // set servo to track music position:
                double value = 0;
                if (mCurrentMusicDuration > 0) {

                    long deltaTime = 0;
                    long elapsedRealTime = SystemClock.elapsedRealtime();
                    if (mState.getLastPositionUpdateTime() > 0) {
                        deltaTime = elapsedRealTime - mState.getLastPositionUpdateTime();
                    }
                    value = (double) (mState.getPosition() + deltaTime) / mCurrentMusicDuration;
                    LogHelper.d(TAG, "positionTrackerTask valid," +
                        " position=", mState.getPosition(),
                        " lastPositionUpdateTime=", mState.getLastPositionUpdateTime(),
                        " elapsedRealTime=", elapsedRealTime,
                        " deltaTime = ", deltaTime,
                        " mCurrentMusicDuration=", mCurrentMusicDuration,
                        " value=", value);
                }

                setPlaybackPosition(value);

            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void setPlaybackPosition(double value) {
        if (mManager == null) {
            return;
        }
        LogHelper.d(TAG, "setPlaybackPosition, value=", value);
        byte v = (byte) (value * 255);
        int servoNumber = 0;  // 0, 1 or 2
        byte commandTarget = (byte) (servoNumber + 0x10);
        try {
            mManager.sendCommand(mManager.LED_SERVO_COMMAND, commandTarget, v);
        } catch (IOException ex) {
            LogHelper.w(TAG, "could not send command to UsbAccessory device", ex);
        }
    }

    private void setLed() {
        if (mManager == null) {
            return;
        }
        int currentLed = mCurrentLed;
        int state = mState != null ? mState.getState() : PlaybackState.STATE_NONE;


        byte[] maxValue, value1, value2;

        switch (state) {
            case PlaybackState.STATE_PAUSED:
                maxValue = new byte[]{0, 0, 40};
                value1 = value2 = new byte[]{0, 0, 40};
                break;
            case PlaybackState.STATE_BUFFERING:
            case PlaybackState.STATE_CONNECTING:
                maxValue = new byte[]{15, 0, 0};
                value1 = new byte[]{15, 0, 0};
                value2 = new byte[]{15, 0, 0};
                break;
            case PlaybackState.STATE_PLAYING:
                maxValue = new byte[]{0, 80, 0};
                value1 = new byte[]{0, 15, 0};
                value2 = new byte[]{0, 3, 0};
                break;
            default:
                maxValue = value1 = value2 = new byte[]{0, 0, 0};
        }

        LogHelper.d(TAG, "setLed, currentLed=", currentLed);

        for (int color = 0; color < 3; color++) {
            try {
                mManager.sendCommand(DemoKitManager.LED_SERVO_COMMAND,
                    (byte) (((currentLed + 1) % 3) * 3 + color), value2[color]);
                mManager.sendCommand(DemoKitManager.LED_SERVO_COMMAND,
                    (byte) (((currentLed + 2) % 3) * 3 + color), value1[color]);
                mManager.sendCommand(DemoKitManager.LED_SERVO_COMMAND,
                    (byte) (currentLed * 3 + color), maxValue[color]);
            } catch (IOException ex) {
                LogHelper.i(TAG, "Temporarily unable to send command to UsbAccessory device. ",
                    ex.getMessage());
            }
        }

    }

    private final MediaController.Callback mMediaCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            LogHelper.d(TAG, "onPlaybackStateChanged: state=", state);
            mState = state;
            setLed();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            LogHelper.d(TAG, "onMetadataChanged: metadata=", metadata);
            mCurrentMusicDuration = metadata.getLong(
                MediaMetadata.METADATA_KEY_DURATION);
        }
    };

    private MediaBrowser.ConnectionCallback mConnectionCallback =
        new MediaBrowser.ConnectionCallback() {
            @Override
            public void onConnected() {
                LogHelper.d(TAG, "onConnected: session token ", mMediaBrowser.getSessionToken());

                if (mMediaBrowser.getSessionToken() == null) {
                    throw new IllegalArgumentException("No Session token");
                }

                mMediaController = new MediaController(
                    DemoKitService.this, mMediaBrowser.getSessionToken());

                mState = mMediaController.getPlaybackState();
                mCurrentMusicDuration = mMediaController.getMetadata().getLong(
                    MediaMetadata.METADATA_KEY_DURATION);

                mMediaController.registerCallback(mMediaCallback);

                startPositionTrackerTask();

            }

            @Override
            public void onConnectionFailed() {
                LogHelper.d(TAG, "onConnectionFailed");
            }

            @Override
            public void onConnectionSuspended() {
                LogHelper.d(TAG, "onConnectionSuspended");
                mMediaController = null;
            }
        };


}
