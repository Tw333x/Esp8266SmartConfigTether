package com.espressif.iot.demo_activity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

class EspWifiAdminSimple {

    private final Context mContext;

    
    EspWifiAdminSimple(Context context) {
        mContext = context;
    }

    String getWifiConnectedSsid() {
        WifiInfo mWifiInfo = getConnectionInfo();
        String ssid = null;
        if (mWifiInfo != null && isWifiConnected()) {
            int len = mWifiInfo.getSSID().length();
            if (mWifiInfo.getSSID().startsWith("\"")
                    && mWifiInfo.getSSID().endsWith("\"")) {
                ssid = mWifiInfo.getSSID().substring(1, len - 1);
            } else {
                ssid = mWifiInfo.getSSID();
            }
        }
        if ((ssid == null) || ssid.equals("<unknown ssid>")) {
            WifiConfiguration conf = getWifiApConfiguration(mContext);
            if (conf != null) {
                ssid = conf.SSID;
            }
        }
        return ssid;
    }

    private static WifiConfiguration getWifiApConfiguration(final Context ctx) {
        final WifiManager wifiManager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final Method m = getWifiManagerMethod("getWifiApConfiguration", wifiManager);
        if(m != null) {
            try {
                return (WifiConfiguration) m.invoke(wifiManager);
            } catch(Exception e) {
                //
            }
        }
        return null;
    }

    private static Method getWifiManagerMethod(final String methodName, final WifiManager wifiManager) {
        final Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
    
    String getWifiConnectedSsidAscii(String ssid) {
        final long timeout = 100;
        final long interval = 20;
        String ssidAscii = ssid;

        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();

        boolean isBreak = false;
        long start = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ignore) {
                break;
            }
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult scanResult : scanResults) {
                if (scanResult.SSID != null && scanResult.SSID.equals(ssid)) {
                    isBreak = true;
                    try {
                        Field wifiSsidfield = ScanResult.class
                                .getDeclaredField("wifiSsid");
                        wifiSsidfield.setAccessible(true);
                        Class<?> wifiSsidClass = wifiSsidfield.getType();
                        Object wifiSsid = wifiSsidfield.get(scanResult);
                        Method method = wifiSsidClass
                                .getDeclaredMethod("getOctets");
                        byte[] bytes = (byte[]) method.invoke(wifiSsid);
                        ssidAscii = new String(bytes, "ISO-8859-1");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        } while (System.currentTimeMillis() - start < timeout && !isBreak);

        return ssidAscii;
    }
    
    String getWifiConnectedBssid() {
        WifiInfo mWifiInfo = getConnectionInfo();
        String bssid = null;
        if (mWifiInfo != null && isWifiConnected()) {
            bssid = mWifiInfo.getBSSID();
        }
        if (bssid == null) {
            bssid = getLocalMacAddr();
        }
        return bssid;
    }

    //http://stackoverflow.com/questions/33159224/getting-mac-address-in-android-6-0
    private static String getLocalMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF)).append(":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
    }

    // get the wifi info which is "connected" in wifi-setting
    private WifiInfo getConnectionInfo() {
        WifiManager mWifiManager = (WifiManager) mContext.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        return mWifiManager.getConnectionInfo();
    }

    private boolean isWifiConnected() {
        NetworkInfo mWiFiNetworkInfo = getWifiNetworkInfo();
        boolean isWifiConnected = false;
        if (mWiFiNetworkInfo != null) {
            isWifiConnected = mWiFiNetworkInfo.isConnected();
        }
        return isWifiConnected;
    }

    private NetworkInfo getWifiNetworkInfo() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return mConnectivityManager.getActiveNetworkInfo();
    }
}
