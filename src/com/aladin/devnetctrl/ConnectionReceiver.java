package com.aladin.devnetctrl;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ConnectionReceiver extends BroadcastReceiver {
	private static String TAG = "DevNetCtrl";

	private int mIp = 0;
	private int mBc = 0;
	private int mNm = 0;

	@Override
	public void onReceive(Context context, Intent intent) {
		clear();
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni != null) {
			int type = ni.getType();
			State state = ni.getState();
			Log.d(TAG, this.getClass().toString() + " onReceive type " + type);
			switch (type) {
			case ConnectivityManager.TYPE_WIFI:
				Log.d(TAG, this.getClass().toString() + " onReceive wifi " + state.name());
				getWifiService(context);
				break;
			case ConnectivityManager.TYPE_MOBILE:
				Log.d(TAG, this.getClass().toString() + " onReceive mobile " + state.name());
				getMobileService(context);
				break;
			default:
				break;
				
			}
			
		} else {
			Log.d(TAG, this.getClass().toString() + " onReceive " + " no network interface");
		}
	}
	
	private void clear() {
		mIp = 0;
		mBc = 0;
		mNm = 0;
	}
	
	public int getLocalIP() {
		return mIp;
	}
	
	public int getBroadcastIP() {
		return mBc;
	}
	
	private int getPhoneIP() {
		int ip = 0;
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress ia = enumIpAddr.nextElement();
					if (!ia.isLoopbackAddress() &&  (ia instanceof Inet4Address)) {
						Log.d(TAG, this.getClass().toString() + " getPhoneIP() " +  ia.getHostAddress().toString());
						ip = Utility.ip2Int(ia.getHostAddress().toString());
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return ip;
	}
	
	private void getWifiService(Context context) {
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wm != null) {
		    DhcpInfo d = wm.getDhcpInfo();
		    if (mIp != d.ipAddress || mNm != d.netmask) {
		    	int ip = d.ipAddress; //(int)0x6701A8C0;
				int nm = d.netmask; //(int)0x00FFFFFF;
				int nw = (int)(ip & nm);
				int bc = (int)(~nm + nw);
				mIp = ip;
				mBc = bc;
				mNm = nm;
				String strLog = "ip:" + Utility.int2Ip(ip) + " nm:" + Utility.int2Ip(nm) + " nw:" + Utility.int2Ip(nw) + " bc:" + Utility.int2Ip(bc);
				Log.d(TAG, this.getClass().toString() + " getWifiService " + strLog);
		    }
		    //Log.d(TAG, this.getClass().toString() + " getWifiService " +  " " + d.toString());
		}
	}
	
	private void getMobileService(Context context) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm != null) {
			mIp = getPhoneIP();
			String non = tm.getNetworkOperatorName();
			Log.d(TAG, this.getClass().toString() + " onReceive Network Operator Name " + non);
			/*
			int subType = ni.getSubtype();
			switch (subType) {
			//2G
			case TelephonyManager.NETWORK_TYPE_CDMA:
				//CTCC
				Log.d(TAG, this.getClass().toString() + " onReceive CTCC 2G CDMA");
				break;
			case TelephonyManager.NETWORK_TYPE_GPRS:
				Log.d(TAG, this.getClass().toString() + " onReceive CUCC 2G GPRS");
				break;
			case TelephonyManager.NETWORK_TYPE_EDGE:
				Log.d(TAG, this.getClass().toString() + " onReceive CMCC 2G EDGE");
				break;
			//3G
			case TelephonyManager.NETWORK_TYPE_UMTS:
				Log.d(TAG, this.getClass().toString() + " onReceive CUCC 3G UMTS");
				break;
			case TelephonyManager.NETWORK_TYPE_HSDPA:
				Log.d(TAG, this.getClass().toString() + " onReceive CUCC 3G HSDPA");
				break;
			case TelephonyManager.NETWORK_TYPE_EVDO_A:
				Log.d(TAG, this.getClass().toString() + " onReceive CTCC 3G EVDO_A");
				break;
			case TelephonyManager.NETWORK_TYPE_EVDO_B:
				Log.d(TAG, this.getClass().toString() + " onReceive CTCC 3G EVDO_B");
				break;
			case TelephonyManager.NETWORK_TYPE_EVDO_0:
				Log.d(TAG, this.getClass().toString() + " onReceive CTCC 3G EVDO_0");
				break;
			//4G
			case TelephonyManager.NETWORK_TYPE_LTE:
				Log.d(TAG, this.getClass().toString() + " onReceive LTE");
				break;
			}
			*/
		}
	}
}
