/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.hardware;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import com.example.android.uamp.utils.LogHelper;

/* This Activity does nothing but receive USB_DEVICE_ATTACHED events from the
 * USB service and springboards to the main Gallery activity
 */
public final class DemoKitActivity extends Activity {

    private static final String TAG = LogHelper.makeLogTag(DemoKitActivity.class);

    private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

    private UsbManager mUsbManager;

    private boolean mPermissionRequestPending;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory[] accessories = mUsbManager.getAccessoryList();
                    if (accessories == null || accessories.length == 0) {
                        LogHelper.w(TAG, "found no accessory");
                    } else if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        onAccessoryConnected(accessories[0]);
                    } else {
                        Log.w(TAG, "permission denied for accessory "
                            + accessories[0]);
                    }
                    mPermissionRequestPending = false;
                }
            }
        }
    };

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "onCreate");

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
	}


    @Override
    public void onResume() {
        super.onResume();
        LogHelper.d(TAG, "onResume");

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                onAccessoryConnected(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                            this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        mUsbManager.requestPermission(accessory, permissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            Log.d(TAG, "mAccessory is null");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogHelper.d(TAG, "onDestroy");
        unregisterReceiver(mUsbReceiver);
    }

    private void onAccessoryConnected(UsbAccessory accessory) {
        LogHelper.d(TAG, "onAccessoryConnected, accessory=", accessory);
        Intent intent = new Intent(this, DemoKitService.class);
        intent.putExtra(DemoKitService.EXTRA_USB_ACCESSORY, accessory);
        startService(intent);
        finish();
    }

}
