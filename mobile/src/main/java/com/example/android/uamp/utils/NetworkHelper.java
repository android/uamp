package com.example.android.uamp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Generic reusable network methods.
 */
public class NetworkHelper {
    /**
     * Check for network connectivity.
     * @param context
     * @return true if connected, false otherwise.
     */
    public static final boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
