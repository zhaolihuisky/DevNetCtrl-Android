package com.aladin.devnetctrl;

import java.util.List;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class NetctrlService extends Service {
	private static String TAG = "DevNetCtrl";
	private ServiceHandler mHandler = null;
	private HandlerThread mHandlerThread = null;
	private Handler mForeignHandler = null;
	private NetctrlBinder mBinder = null;
	private UdpCom mUdpCom = null;
	private User mUser = null;
	private DeviceMgr mDevMgr = null;
	private String mDevsDir = "";
	private ConnectionReceiver mNetworkReceiver;
	private boolean mHeartBeatExit = false;
	private HeartBeatThread mHeartBeatThread = null;
	private boolean mUserUnregister = false;
	private static int HART_BEAT_TIMEOUT = 1800;
	
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		//Log.d(TAG, this.getClass().toString() + " onCreate ");
		mNetworkReceiver = new ConnectionReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mNetworkReceiver, filter);
		//mHandler = new ServiceHandler();
		startHandler();
		mBinder = new NetctrlBinder();
		mUdpCom = new UdpCom();
		mUdpCom.setServiceHandler(mHandler);
		mUdpCom.createUdp();
		mUdpCom.startRecv();
		mUdpCom.startSend();
		mUser = new User();
		mDevMgr = new DeviceMgr();
		mDevsDir = getFilesDir().getAbsolutePath() + "/devices";
		Log.d(TAG, this.getClass().toString() + " onCreate() sDevsDir:" + mDevsDir);
		mDevMgr.setDevsDir(mDevsDir);
		new Thread() {
			public void run() {
				mDevMgr.loadDevices();
			}
		}.start();
		startHeartBeat();
		/*
		new Thread() {
			public void run() {
				mDevMgr.testLargeDevs();
			}
		}.start();
		*/
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopHeartBeat();
		mUdpCom.stopSend();
		mUdpCom.stopRecv();
		mUdpCom.destroyUdp();
		unregisterReceiver(mNetworkReceiver);
		if (mUserUnregister == true) {
			mDevMgr.delAllDevices();
		}
		mHandler.removeCallbacks(Thread.currentThread());
		stopHandler();
		Log.d(TAG, this.getClass().toString() + " onDestroy ");
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	private void startHandler() {
		//android application supply the handler thread
		mHandlerThread = new HandlerThread("ServiceHandlerThread");
		//the handler thread's start function must call before handler thread's getLooper function
		mHandlerThread.start();
		mHandler = new ServiceHandler(mHandlerThread.getLooper());
		Log.d(TAG, this.getClass().toString() + " startHandler() ServiceHandlerThread thrdID:" + mHandlerThread.getId());
	}
	
	private void stopHandler() {
		Log.d(TAG, this.getClass().toString() + " stopHandler()");
		mHandlerThread.quit();
	}
	
	private int getLocalIP() {
		int ip = mNetworkReceiver.getLocalIP();
		return ip;
	}
	
	private int getBroadcastIP() {
		int bc = mNetworkReceiver.getBroadcastIP();
		return bc;
	}
	
	class HeartBeatThread extends Thread {
		int mCount = 0;
		@Override
		public void run() {
			while (mHeartBeatExit == false) {
				mCount++;
				if (mCount < HART_BEAT_TIMEOUT) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
				mCount = 0;
				
				Message msg = Message.obtain();
				msg.what = Protocol.USR_HEART_BEAT_REQ;
				Bundle b = new Bundle();
				b.putString("name", mUser.getName());
				b.putString("pwd", mUser.getPwd());
				msg.setData(b);
				mHandler.sendMessage(msg);
			}
		}
	}
	
	private void startHeartBeat() {
		Log.d(TAG, this.getClass().toString() + " startHeartBeat");
		mHeartBeatThread = new HeartBeatThread();
		mHeartBeatThread.start();
	}
	
	private void stopHeartBeat() {
		Log.d(TAG, this.getClass().toString() + " stopHeartBeat");
		mHeartBeatExit = true;
		try {
			if (mHeartBeatThread != null) {
				mHeartBeatThread.join();
				mHeartBeatThread = null;
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e); 
		}
	}
	
	class ServiceHandler extends Handler {
		public ServiceHandler() {
			super();
		}

		public ServiceHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			Log.d(TAG, this.getClass().toString() + " handleMessage thrdID:" + Thread.currentThread().getId() + " msg:" + msg.toString());
			int what = msg.what;
			switch (what) {
			case Protocol.USR_SET_REQ:
				userSetRequest(msg);
				break;
			case Protocol.USR_INFO_REQ:
				userInfoRequest(msg);
				break;
			case Protocol.USR_REGISTER_REQ:
				userRegisterRequest(msg);
				break;
			case Protocol.USR_UNREGISTER_REQ:
				userUnregisterRequest(msg);
				break;
			case Protocol.USR_LOGON_REQ:
				userLogonRequest(msg);
				break;
			case Protocol.USR_LOGOFF_REQ:
				userLogoffRequest(msg);
				break;
			case Protocol.USR_HEART_BEAT_REQ:
				userHeartBeatRequest(msg);
				break;
			case Protocol.USR_DEV_REGISTER_REQ:
				userDevRegisterRequest(msg);
				break;
			case Protocol.USR_DEV_UNREGISTER_REQ:
				userDevUnregisterRequest(msg);
				break;
			case Protocol.USR_DEV_REGLIST_REQ:
				userDevRegListRequest(msg);
				break;
			case Protocol.LIGHT_COLOR_REQ:
				lightColorRequest(msg);
				break;
			case Protocol.LIGHT_ONOFF_REQ:
				lightOnOffRequest(msg);
				break;
			case Protocol.LIGHT_AUTO_REQ:
				lightAutoRequest(msg);
				break;
			case Protocol.LIGHT_INCREASE_REQ:
				lightIncreaseRequest(msg);
				break;
			case Protocol.LIGHT_DECREASE_REQ:
				lightDecreaseRequest(msg);
				break;
			case Protocol.LIGHT_RANDOM_REQ:
				lightRandomRequest(msg);
				break;
			case Protocol.LIGHT_BLUETOOTH_REQ:
				lightBluetoothRequest(msg);
				break;
			case Protocol.LIGHT_MICPHONE_REQ:
				lightMicphoneRequest(msg);
				break;
			case Protocol.LIGHT_LANSCAN_REQ:
				lightLanscanRequest(msg);
				break;
			case Protocol.DATA_RECEIVE:
				dataReceive(msg);
				break;
			case Protocol.USR_DEV_ADD_REQ:
				userDevAddRequest(msg);
				break;
			case Protocol.USR_DEV_EDT_REQ:
				userDevEdtRequest(msg);
				break;
			case Protocol.USR_DEV_DEL_REQ:
				userDevDelRequest(msg);
				break;
			case Protocol.USR_DEV_CHK_REQ:
				userDevChkRequest(msg);
				break;
			case Protocol.USR_DEV_CHKLIST_REQ:
				userDevChkListRequest(msg);
				break;
			case Protocol.USR_DEV_CHKALL_REQ:
				userDevCheckAllRequest(msg);
				break;
			case Protocol.USR_DEV_UNCHKALL_REQ:
				userDevUncheckAllRequest(msg);
				break;
			case Protocol.USR_DEV_CHKDEL_REQ:
				userDevCheckDelRequest(msg);
				break;
			case Protocol.USR_DEV_COUNT_REQ:
				userDevCountRequest(msg);
				break;
			default:
				break;
			}
		}
	}

	public class NetctrlBinder extends Binder {
		public Handler getHandler() {
			return mHandler;
		}

		public List<Device> getDevices() {
			return mDevMgr.getDevices();
		}
		
		public Device getDevice(String mac) {
			return mDevMgr.getDevice(mac);
		}
		
		public Device getDevice(int index) {
			return mDevMgr.getDevice(index);
		}
		
		public void setForeignHandler(Handler handler) {
			mForeignHandler = handler;
		}
		
		public void setUser(User user) {
			mUser.setName(user.getName());
			mUser.setPwd(user.getPwd());
		}
	}

	private void sendForeignMsg(Message msg) {
		if(mForeignHandler != null) {
			mForeignHandler.sendMessage(msg);
		}
	}
	
	private void sendUdpMsg(Message msg) {
		if(mUdpCom != null) {
			mUdpCom.sendMsg(msg);
		}
	}
	
	private byte[] dataGenerate(short addr, byte code, short offset, byte count, byte[] value) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|...|crc(2B)
		short size = (short)(2 + 2 + 1 + 2 + 1 + count + 2);
		byte[] tx = new byte[size];
		byte[] a = Utility.short2Bytes(addr);
		tx[0] = a[0];
		tx[1] = a[1];
		byte[] b = Utility.short2Bytes(size);
		tx[2] = b[0];
		tx[3] = b[1];
		tx[4] = code;
		byte[] c = Utility.short2Bytes(offset);
		tx[5] = c[0];
		tx[6] = c[1];
		tx[7] = count;
		for(int i=0; i<count; i++) {
			tx[8+i] = value[i];
		}
		short crc = Utility.crc16(tx, size-2);
		byte[] d = Utility.short2Bytes(crc);
		tx[size-2] = d[0];
		tx[size-1] = d[1];
		return tx;
	}
	
	private void userRegisterRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|pwdlen(1B)|pwd(xB)|crc(2B)
		Bundle b = msg.getData();
		String sName = b.getString("name");
		String sPwd = b.getString("pwd");
		String sPwdMD5 = Utility.getMD5(sPwd);
		byte aName[] = sName.getBytes();
		byte aPwd[] = sPwdMD5.getBytes();
		byte nameLen = (byte)aName.length;
		byte pwdLen = (byte)aPwd.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.USR_REGISTER;
		byte vlen = (byte)(1 + nameLen + 1 + pwdLen);
		byte[] v = new byte[vlen];
		v[0] = nameLen;
		for(int i=0; i<nameLen; i++) {
			v[i+1] = aName[i];
		}
		v[nameLen+1] = pwdLen;
		for(int i=0; i<pwdLen; i++) {
			v[nameLen+1+1+i] = aPwd[i];
		}
		byte[] tx = dataGenerate(addr, code, offset, vlen, v);
		
		Message udpMsg = Message.obtain();
		Bundle udpBdl = new Bundle();
		udpBdl.putString("ip", Protocol.SERVER_HOST);
		udpBdl.putInt("port", Protocol.SERVER_PORT);
		udpBdl.putInt("len", tx.length);
		udpBdl.putByteArray("tx", tx);
		udpMsg.setData(udpBdl);
		Log.d(TAG, this.getClass().toString() + " userRegisterRequest() " + Utility.data2String(tx, tx.length));
		sendUdpMsg(udpMsg);
	}
	
	private void userRegisterResponse(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|resultlen(1B)|result(1B)|crc(2B)
		Bundle b = msg.getData();
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		if(len > 11) {
			byte vallen = rx[7];
			byte resLen = rx[8];
			byte res = rx[9];
			Log.d(TAG, this.getClass().toString() + " userRegisterResponse() vallen:" + vallen + " resLen:" + resLen + " res:" + res);
			Message userMsg = Message.obtain();
			userMsg.what = Protocol.USR_REGISTER_RES;
			userMsg.arg1 = res;
			sendForeignMsg(userMsg);
		}
	}
	
	private void userUnregisterRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|pwdlen(1B)|pwd(xB)|crc(2B)
		int status = mUser.getStatus();
		Log.d(TAG, this.getClass().toString() + " userUnregisterRequest() status:" + status);
		if (status != Protocol.USR_STATUS_ONLINE) {
			return;
		}
		String sName = mUser.getName();//b.getString("name");
		String sPwd = mUser.getPwd();//b.getString("pwd");
		String sPwdMD5 = Utility.getMD5(sPwd);
		byte aName[] = sName.getBytes();
		byte aPwd[] = sPwdMD5.getBytes();
		byte nameLen = (byte)aName.length;
		byte pwdLen = (byte)aPwd.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.USR_UNREGISTER;
		byte vlen = (byte)(1 + nameLen + 1 + pwdLen);
		byte[] v = new byte[vlen];
		v[0] = nameLen;
		for(int i=0; i<nameLen; i++) {
			v[i+1] = aName[i];
		}
		v[nameLen+1] = pwdLen;
		for(int i=0; i<pwdLen; i++) {
			v[nameLen+1+1+i] = aPwd[i];
		}
		byte[] tx = dataGenerate(addr, code, offset, vlen, v);
		
		Message udpMsg = Message.obtain();
		Bundle udpBdl = new Bundle();
		udpBdl.putString("ip", Protocol.SERVER_HOST);
		udpBdl.putInt("port", Protocol.SERVER_PORT);
		udpBdl.putInt("len", tx.length);
		udpBdl.putByteArray("tx", tx);
		udpMsg.setData(udpBdl);
		Log.d(TAG, this.getClass().toString() + " userUnregisterRequest() " + Utility.data2String(tx, tx.length));
		sendUdpMsg(udpMsg);
	}
	
	private void userUnregisterResponse(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|resultlen(1B)|result(1B)|crc(2B)
		Bundle b = msg.getData();
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		if(len > 11) {
			byte vallen = rx[7];
			byte resLen = rx[8];
			byte res = rx[9];
			Log.d(TAG, this.getClass().toString() + " userUnregisterResponse() vallen:" + vallen + " resLen:" + resLen + " res:" + res);
			mUser.clear();
			mDevMgr.setAllDevsUnregisted();
			
			Message userMsg = Message.obtain();
			userMsg.what = Protocol.USR_UNREGISTER_RES;
			userMsg.arg1 = res;
			sendForeignMsg(userMsg);
		}
	}
	
	private void userLogonRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|pwdlen(1B)|pwd(xB)|crc(2B)
		Bundle b = msg.getData();
		String sName = b.getString("name");
		String sPwd = b.getString("pwd");
		String sPwdMD5 = Utility.getMD5(sPwd);
		byte aName[] = sName.getBytes();
		byte aPwd[] = sPwdMD5.getBytes();
		byte nameLen = (byte)aName.length;
		byte pwdLen = (byte)aPwd.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.USR_LOGON;
		byte vlen = (byte)(1 + nameLen + 1 + pwdLen);
		byte[] v = new byte[vlen];
		v[0] = nameLen;
		for(int i=0; i<nameLen; i++) {
			v[i+1] = aName[i];
		}
		v[nameLen+1] = pwdLen;
		for(int i=0; i<pwdLen; i++) {
			v[nameLen+1+1+i] = aPwd[i];
		}
		byte[] tx = dataGenerate(addr, code, offset, vlen, v);
		
		Message udpMsg = Message.obtain();
		Bundle udpBdl = new Bundle();
		udpBdl.putString("ip", Protocol.SERVER_HOST);
		udpBdl.putInt("port", Protocol.SERVER_PORT);
		udpBdl.putInt("len", tx.length);
		udpBdl.putByteArray("tx", tx);
		udpMsg.setData(udpBdl);
		Log.d(TAG, this.getClass().toString() + " userLogonRequest() " + Utility.data2String(tx, tx.length));
		sendUdpMsg(udpMsg);
	}
	
	private void userLogonResponse(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|resultlen(1B)|result(1B)|crc(2B)
		Bundle b = msg.getData();
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		if(len > 11) {
			byte vallen = rx[7];
			byte resLen = rx[8];
			byte res = rx[9];
			Log.d(TAG, this.getClass().toString() + " userLogonResponse() vallen:" + vallen + " resLen:" + resLen + " res:" + res);
			
			Message userMsg = Message.obtain();
			userMsg.what = Protocol.USR_LOGON_RES;
			userMsg.arg1 = res;
			sendForeignMsg(userMsg);
		}
	}
	
	private void userLogoffRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|pwdlen(1B)|pwd(xB)|crc(2B)
		int status = mUser.getStatus();
		Log.d(TAG, this.getClass().toString() + " userLogoffRequest() status:" + status);
		if (status != Protocol.USR_STATUS_ONLINE) {
			return;
		}
		String sName = mUser.getName();//b.getString("name");
		String sPwd = mUser.getPwd();//b.getString("pwd");
		String sPwdMD5 = Utility.getMD5(sPwd);
		byte aName[] = sName.getBytes();
		byte aPwd[] = sPwdMD5.getBytes();
		byte nameLen = (byte)aName.length;
		byte pwdLen = (byte)aPwd.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.USR_LOGOFF;
		byte vlen = (byte)(1 + nameLen + 1 + pwdLen);
		byte[] v = new byte[vlen];
		v[0] = nameLen;
		for(int i=0; i<nameLen; i++) {
			v[i+1] = aName[i];
		}
		v[nameLen+1] = pwdLen;
		for(int i=0; i<pwdLen; i++) {
			v[nameLen+1+1+i] = aPwd[i];
		}
		byte[] tx = dataGenerate(addr, code, offset, vlen, v);
		
		Message udpMsg = Message.obtain();
		Bundle udpBdl = new Bundle();
		udpBdl.putString("ip", Protocol.SERVER_HOST);
		udpBdl.putInt("port", Protocol.SERVER_PORT);
		udpBdl.putInt("len", tx.length);
		udpBdl.putByteArray("tx", tx);
		udpMsg.setData(udpBdl);
		Log.d(TAG, this.getClass().toString() + " userLogoffRequest() " + Utility.data2String(tx, tx.length));
		sendUdpMsg(udpMsg);
	}
	
	private void userLogoffResponse(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|resultlen(1B)|result(1B)|crc(2B)
		Bundle b = msg.getData();
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		if(len > 11) {
			byte vallen = rx[7];
			byte resLen = rx[8];
			byte res = rx[9];
			Log.d(TAG, this.getClass().toString() + " userLogoffResponse() vallen:" + vallen + " resLen:" + resLen + " res:" + res);
			mUser.clear();
			mDevMgr.setAllDevsUnregisted();
			
			Message userMsg = Message.obtain();
			userMsg.what = Protocol.USR_LOGOFF_RES;
			userMsg.arg1 = res;
			sendForeignMsg(userMsg);
		}
	}
	
	private void userHeartBeatRequest(Message msg) {
		int status = mUser.getStatus();
		Log.d(TAG, this.getClass().toString() + " userHeartBeatRequest() status:" + status);
		if (status != Protocol.USR_STATUS_ONLINE) {
			return;
		}
		Message logmsg = Message.obtain();
		msg.what = Protocol.USR_LOGON_REQ;
		Bundle b = new Bundle();
		b.putString("name", mUser.getName());
		b.putString("pwd", mUser.getPwd());
		logmsg.setData(b);
		mHandler.sendMessage(logmsg);
	}
	
	private void userDevRegisterRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|crc(2B)
		int status = mUser.getStatus();
		Log.d(TAG, this.getClass().toString() + " userDevUnregisterRequest() status:" + status);
		if (status != Protocol.USR_STATUS_ONLINE) {
			return;
		}
		Bundle b = msg.getData();
		int index = b.getInt("index");
		Device dev = mDevMgr.getDevice(index);
		if(dev != null && !dev.getRegisted()) {
			String sName = mUser.getName();//b.getString("name");
			String sMac = dev.getMac();//b.getString("mac");
			byte aName[] = sName.getBytes();
			byte aMac[] = sMac.getBytes();
			byte nameLen = (byte)aName.length;
			byte macLen = (byte)aMac.length;
			short addr = (short)0xFFFF;
			byte code = Protocol.CODE_NETWORK_CTRL;
			short offset = Protocol.USR_DEV_REGISTER;
			byte vlen = (byte)(1 + nameLen + 1 + macLen);
			byte[] v = new byte[vlen];
			v[0] = nameLen;
			for(int i=0; i<nameLen; i++) {
				v[i+1] = aName[i];
			}
			v[nameLen+1] = macLen;
			for(int i=0; i<macLen; i++) {
				v[nameLen+1+1+i] = aMac[i];
			}
			byte[] tx = dataGenerate(addr, code, offset, vlen, v);
			
			Message udpMsg = Message.obtain();
			Bundle udpBdl = new Bundle();
			udpBdl.putString("ip", Protocol.SERVER_HOST);
			udpBdl.putInt("port", Protocol.SERVER_PORT);
			udpBdl.putInt("len", tx.length);
			udpBdl.putByteArray("tx", tx);
			udpMsg.setData(udpBdl);
			Log.d(TAG, this.getClass().toString() + " userDevRegisterRequest() " + Utility.data2String(tx, tx.length));
			sendUdpMsg(udpMsg);
		}
	}
	
	private void userDevRegisterResponse(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|maclen(1B)|mac(12B)|resultlen(1B)|result(1B)|crc(2B)
		Bundle b = msg.getData();
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		if(len > 23) {
			byte valLen = rx[7];
			byte macLen = rx[8];
			String sMac = "";
			for(int i=0; i<macLen; i++) {
				sMac += String.format("%c", rx[9+i]);
			}
			Log.d(TAG, this.getClass().toString() + " userDevRegisterResponse() valLen:" + valLen + " macLen:" + macLen + " mac:" + sMac);
			byte resLen = rx[9+macLen];
			byte res = rx[9+macLen+1];
			Log.d(TAG, this.getClass().toString() + " userDevRegisterResponse() resLen:" + resLen + " res:" + res);
			mDevMgr.setDevRegisted(sMac, true);
			
			Message userMsg = Message.obtain();
			userMsg.what = Protocol.USR_DEV_REGISTER_RES;
			userMsg.arg1 = res;
			sendForeignMsg(userMsg);
		}
	}
	
	private void userDevUnregisterRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|crc(2B)
		int status = mUser.getStatus();
		Log.d(TAG, this.getClass().toString() + " userDevUnregisterRequest() status:" + status);
		if (status != Protocol.USR_STATUS_ONLINE) {
			return;
		}
		Bundle b = msg.getData();
		int index = b.getInt("index");
		Device dev = mDevMgr.getDevice(index);
		if (dev != null && dev.getRegisted()) {
			String sName = mUser.getName();//b.getString("name");
			String sMac = dev.getMac();//b.getString("mac");
			byte aName[] = sName.getBytes();
			byte aMac[] = sMac.getBytes();
			byte nameLen = (byte)aName.length;
			byte macLen = (byte)aMac.length;
			short addr = (short)0xFFFF;
			byte code = Protocol.CODE_NETWORK_CTRL;
			short offset = Protocol.USR_DEV_UNREGISTER;
			byte vlen = (byte)(1 + nameLen + 1 + macLen);
			byte[] v = new byte[vlen];
			v[0] = nameLen;
			for(int i=0; i<nameLen; i++) {
				v[i+1] = aName[i];
			}
			v[nameLen+1] = macLen;
			for(int i=0; i<macLen; i++) {
				v[nameLen+1+1+i] = aMac[i];
			}
			byte[] tx = dataGenerate(addr, code, offset, vlen, v);
			
			Message udpMsg = Message.obtain();
			Bundle udpBdl = new Bundle();
			udpBdl.putString("ip", Protocol.SERVER_HOST);
			udpBdl.putInt("port", Protocol.SERVER_PORT);
			udpBdl.putInt("len", tx.length);
			udpBdl.putByteArray("tx", tx);
			udpMsg.setData(udpBdl);
			Log.d(TAG, this.getClass().toString() + " userDevUnregisterRequest() " + Utility.data2String(tx, tx.length));
			sendUdpMsg(udpMsg);
		}
	}
	
	private void userDevUnregisterResponse(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|maclen(1B)|mac(12B)|resultlen(1B)|result(1B)|crc(2B)
		Bundle b = msg.getData();
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		if(len > 23) {
			byte valLen = rx[7];
			byte macLen = rx[8];
			String sMac = "";
			for(int i=0; i<macLen; i++) {
				sMac += String.format("%c", rx[9+i]);
			}
			Log.d(TAG, this.getClass().toString() + " userDevUnregisterResponse() valLen:" + valLen + " macLen:" + macLen + " mac:" + sMac);
			byte resLen = rx[9+macLen];
			byte res = rx[9+macLen+1];
			Log.d(TAG, this.getClass().toString() + " userDevUnregisterResponse() resLen:" + resLen + " res:" + res);
			if(res == Protocol.USR_DEV_UNREGISTER_SUCCESS) {
				mDevMgr.setDevRegisted(sMac, false);
			}
			Message userMsg = Message.obtain();
			userMsg.what = Protocol.USR_DEV_UNREGISTER_RES;
			userMsg.arg1 = res;
			sendForeignMsg(userMsg);
		}
	}
	
	private void userDevRegListRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|pwdlen(1B)|pwd(xB)|crc(2B)
		int status = mUser.getStatus();
		Log.d(TAG, this.getClass().toString() + " userDevRegListRequest() status:" + status);
		if (status != Protocol.USR_STATUS_ONLINE) {
			return;
		}
		String sName = mUser.getName();//b.getString("name");
		String sPwd = mUser.getPwd();//b.getString("pwd");
		String sPwdMD5 = Utility.getMD5(sPwd);
		byte aName[] = sName.getBytes();
		byte aPwd[] = sPwdMD5.getBytes();
		byte nameLen = (byte)aName.length;
		byte pwdLen = (byte)aPwd.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.USR_DEV_REGLIST;
		byte vlen = (byte)(1 + nameLen + 1 + pwdLen);
		byte[] v = new byte[vlen];
		v[0] = nameLen;
		for(int i=0; i<nameLen; i++) {
			v[i+1] = aName[i];
		}
		v[nameLen+1] = pwdLen;
		for(int i=0; i<pwdLen; i++) {
			v[nameLen+1+1+i] = aPwd[i];
		}
		byte[] tx = dataGenerate(addr, code, offset, vlen, v);
		
		Message udpMsg = Message.obtain();
		Bundle udpBdl = new Bundle();
		udpBdl.putString("ip", Protocol.SERVER_HOST);
		udpBdl.putInt("port", Protocol.SERVER_PORT);
		udpBdl.putInt("len", tx.length);
		udpBdl.putByteArray("tx", tx);
		udpMsg.setData(udpBdl);
		Log.d(TAG, this.getClass().toString() + " userDevRegListRequest() " + Utility.data2String(tx, tx.length));
		sendUdpMsg(udpMsg);
	}
	
	private void userDevRegListResponse(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|maclen(1B)|mac(12B)|resultlen(1B)|result(1B)|crc(2B)
		Bundle b = msg.getData();
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		if(len > 23) {
			byte valLen = rx[7];
			byte macLen = rx[8];
			String sMac = "";
			for(int i=0; i<macLen; i++) {
				sMac += String.format("%c", rx[9+i]);
			}
			Log.d(TAG, this.getClass().toString() + " userDevRegListResponse() valLen:" + valLen + " macLen:" + macLen + " mac:" + sMac);
			byte resLen = rx[9+macLen];
			byte res = rx[9+macLen+1];
			Log.d(TAG, this.getClass().toString() + " userDevRegListResponse() resLen:" + resLen + " res:" + res);
			if(res == Protocol.USR_DEV_REGLIST_SUCCESS) {
				Device dev = mDevMgr.getDevice(sMac);
				if(dev == null) {
					Device newDev = new Device();
					newDev.setMac(sMac);
					newDev.setName(sMac);
					newDev.setRegisted(true);
					String sPath = mDevsDir + "/" + sMac + ".ini";
					newDev.setPath(sPath);
					mDevMgr.addDevice(newDev);
				} else {
					dev.setRegisted(true);
				}
			}
		}
	}
	
	//private void userConnectDevRequest(Message msg) {
	//}
	
	private void userConnectDevResponse(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|maclen(1B)|mac(12B)|iplen(1B)|ip(xB)|portlen(1B)|port(4B)|crc(2B)
		Bundle b = msg.getData();
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		if(len > 23) {
			byte valLen = rx[7];
			byte macLen = rx[8];
			String sMac = "";
			for(int i=0; i<macLen; i++) {
				sMac += String.format("%c", rx[9+i]);
			}
			Log.d(TAG, this.getClass().toString() + " userConnectDevResponse() valLen:" + valLen + " macLen:" + macLen + " mac:" + sMac);
			byte ipLen = rx[9+macLen];
			String sIp = "";
			for(int i=0; i<ipLen; i++) {
				sIp += String.format("%c", rx[10+macLen+i]);
			}
			byte portLen = rx[10+macLen+ipLen];
			byte aPort[] = new byte[4];
			for(int i=0; i<portLen; i++) {
				aPort[i] = rx[11+macLen+ipLen+i];
			}
			int port = Utility.bytes2Int(aPort);
			Log.d(TAG, this.getClass().toString() + " userConnectDevResponse() ip:" + sIp + " port:" + port);

			Device dev = mDevMgr.getDevice(sMac);
			if(dev != null) {
				dev.setWanIP(sIp);
				dev.setWanPort(port);
				
				Message devMsg = Message.obtain();
				devMsg.what = Protocol.USR_DEV_IPPORT_RES;
				sendForeignMsg(devMsg);
			}
		}
	}
	
	private void lightColorRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|cmdlen(1B)|cmd(xB)|crc(2B)
		String sName = mUser.getName();
		byte aName[] = sName.getBytes();
		byte nameLen = (byte)aName.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.CMD_TRANSLATE;
		String sDevsIndex = mDevMgr.getCheckDevsIndex();
		Log.d(TAG, this.getClass().toString() + " lightColorRequest() sDevsIndex:" + sDevsIndex);
		String aIndex[] = sDevsIndex.split("\\,");
		for (int i=0; i<aIndex.length; i++) {
			if (aIndex[i].equals("")) {
				continue;
			}
			int index = Integer.parseInt(aIndex[i], 10);
			Device dev = mDevMgr.getDevice(index);
			if (dev == null) {
				continue;
			}
			String sMac = dev.getMac();
			byte aMac[] = sMac.getBytes();
			byte macLen = (byte)aMac.length;
			int color = msg.arg1;
			byte aCmd[] = dev.lightColorCmd(color);
			byte cmdLen = (byte)aCmd.length;
			Log.d(TAG, this.getClass().toString() + " lightColorRequest() " + Utility.data2String(aCmd, cmdLen));
			if (dev.getRegisted()) {
				String ip = dev.getWanIP();
				int port = dev.getWanPort();
				byte[] tx = aCmd;
				//if (port == 0) {
					ip = Protocol.SERVER_HOST;
					port = Protocol.SERVER_PORT;
					byte vlen = (byte)(1 + nameLen + 1 + macLen + 1 + cmdLen);
					byte[] v = new byte[vlen];
					v[0] = nameLen;
					for(int j=0; j<nameLen; j++) {
						v[1+j] = aName[j];
					}
					v[nameLen+1] = macLen;
					for(int j=0; j<macLen; j++) {
						v[nameLen+1+1+j] = aMac[j];
					}
					v[nameLen+1+1+macLen] = cmdLen;
					for(int j=0; j<cmdLen; j++) {
						v[nameLen+1+1+macLen+1+j] = aCmd[j];
					}
					tx = dataGenerate(addr, code, offset, vlen, v);
				//}
				
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", ip);
				udpBdl.putInt("port", port);
				udpBdl.putInt("len", tx.length);
				udpBdl.putByteArray("tx", tx);
				udpMsg.setData(udpBdl);
				Log.d(TAG, this.getClass().toString() + " lightColorRequest() " + Utility.data2String(tx, tx.length));
				sendUdpMsg(udpMsg);
			} else {
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", dev.getLanIP());
				udpBdl.putInt("port", dev.getLanPort());
				udpBdl.putInt("len", cmdLen);
				udpBdl.putByteArray("tx", aCmd);
				udpMsg.setData(udpBdl);
				sendUdpMsg(udpMsg);
			}
		}
	}
	
	//private void lightColorResponse(Message msg) {
	//}
	
	private void lightOnOffRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|cmdlen(1B)|cmd(xB)|crc(2B)
		String sName = mUser.getName();
		byte aName[] = sName.getBytes();
		byte nameLen = (byte)aName.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.CMD_TRANSLATE;
		String sDevsIndex = mDevMgr.getCheckDevsIndex();
		Log.d(TAG, this.getClass().toString() + " lightOnOffRequest() sDevsIndex:" + sDevsIndex);
		String aIndex[] = sDevsIndex.split("\\,");
		for (int i=0; i<aIndex.length; i++) {
			if (aIndex[i].equals("")) {
				continue;
			}
			int index = Integer.parseInt(aIndex[i], 10);
			Device dev = mDevMgr.getDevice(index);
			String sMac = dev.getMac();
			byte aMac[] = sMac.getBytes();
			byte macLen = (byte)aMac.length;
			int onOff = msg.arg1;
			boolean bOnOff = false;
			if (onOff > 0) {
				bOnOff = true;
			}
			byte aCmd[] = dev.lightOnOffCmd(bOnOff);
			byte cmdLen = (byte)aCmd.length;
			Log.d(TAG, this.getClass().toString() + " lightOnOffRequest() " + Utility.data2String(aCmd, cmdLen));
			if (dev.getRegisted()) {
				String ip = dev.getWanIP();
				int port = dev.getWanPort();
				byte[] tx = aCmd;
				//if (port == 0) {
					ip = Protocol.SERVER_HOST;
					port = Protocol.SERVER_PORT;
					byte vlen = (byte)(1 + nameLen + 1 + macLen + 1 + cmdLen);
					byte[] v = new byte[vlen];
					v[0] = nameLen;
					for(int j=0; j<nameLen; j++) {
						v[1+j] = aName[j];
					}
					v[nameLen+1] = macLen;
					for(int j=0; j<macLen; j++) {
						v[nameLen+1+1+j] = aMac[j];
					}
					v[nameLen+1+1+macLen] = cmdLen;
					for(int j=0; j<cmdLen; j++) {
						v[nameLen+1+1+macLen+1+j] = aCmd[j];
					}
					tx = dataGenerate(addr, code, offset, vlen, v);
				//}
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", ip);
				udpBdl.putInt("port", port);
				udpBdl.putInt("len", tx.length);
				udpBdl.putByteArray("tx", tx);
				udpMsg.setData(udpBdl);
				Log.d(TAG, this.getClass().toString() + " lightOnOffRequest() " + Utility.data2String(tx, tx.length));
				sendUdpMsg(udpMsg);
			} else {
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", dev.getLanIP());
				udpBdl.putInt("port", dev.getLanPort());
				udpBdl.putInt("len", cmdLen);
				udpBdl.putByteArray("tx", aCmd);
				udpMsg.setData(udpBdl);
				sendUdpMsg(udpMsg);
			}
		}
	}
	
	//private void lightOnOffResponse(Message msg) {	
	//}
	
	private void lightAutoRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|cmdlen(1B)|cmd(xB)|crc(2B)
		String sName = mUser.getName();
		byte aName[] = sName.getBytes();
		byte nameLen = (byte)aName.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.CMD_TRANSLATE;
		String sDevsIndex = mDevMgr.getCheckDevsIndex();
		Log.d(TAG, this.getClass().toString() + " lightAutoRequest() sDevsIndex:" + sDevsIndex);
		String aIndex[] = sDevsIndex.split("\\,");
		for (int i=0; i<aIndex.length; i++) {
			if (aIndex[i].equals("")) {
				continue;
			}
			int index = Integer.parseInt(aIndex[i], 10);
			Device dev = mDevMgr.getDevice(index);
			String sMac = dev.getMac();
			byte aMac[] = sMac.getBytes();
			byte macLen = (byte)aMac.length;
			byte aCmd[] = dev.lightAuto();
			byte cmdLen = (byte)aCmd.length;
			Log.d(TAG, this.getClass().toString() + " lightAutoRequest() " + Utility.data2String(aCmd, cmdLen));
			if (dev.getRegisted()) {
				String ip = dev.getWanIP();
				int port = dev.getWanPort();
				byte[] tx = aCmd;
				//if (port == 0) {
					ip = Protocol.SERVER_HOST;
					port = Protocol.SERVER_PORT;
					byte vlen = (byte)(1 + nameLen + 1 + macLen + 1 + cmdLen);
					byte[] v = new byte[vlen];
					v[0] = nameLen;
					for(int j=0; j<nameLen; j++) {
						v[1+j] = aName[j];
					}
					v[nameLen+1] = macLen;
					for(int j=0; j<macLen; j++) {
						v[nameLen+1+1+j] = aMac[j];
					}
					v[nameLen+1+1+macLen] = cmdLen;
					for(int j=0; j<cmdLen; j++) {
						v[nameLen+1+1+macLen+1+j] = aCmd[j];
					}
					tx = dataGenerate(addr, code, offset, vlen, v);
				//}
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", ip);
				udpBdl.putInt("port", port);
				udpBdl.putInt("len", tx.length);
				udpBdl.putByteArray("tx", tx);
				udpMsg.setData(udpBdl);
				Log.d(TAG, this.getClass().toString() + " lightAutoRequest() " + Utility.data2String(tx, tx.length));
				sendUdpMsg(udpMsg);
			} else {
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", dev.getLanIP());
				udpBdl.putInt("port", dev.getLanPort());
				udpBdl.putInt("len", cmdLen);
				udpBdl.putByteArray("tx", aCmd);
				udpMsg.setData(udpBdl);
				sendUdpMsg(udpMsg);
			}
		}
	}
	
	//private void lightAutoResponse(Message msg) {	
	//}
	
	private void lightIncreaseRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|cmdlen(1B)|cmd(xB)|crc(2B)
		String sName = mUser.getName();
		byte aName[] = sName.getBytes();
		byte nameLen = (byte)aName.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.CMD_TRANSLATE;
		String sDevsIndex = mDevMgr.getCheckDevsIndex();
		Log.d(TAG, this.getClass().toString() + " lightIncreaseRequest() sDevsIndex:" + sDevsIndex);
		String aIndex[] = sDevsIndex.split("\\,");
		for (int i=0; i<aIndex.length; i++) {
			if (aIndex[i].equals("")) {
				continue;
			}
			int index = Integer.parseInt(aIndex[i], 10);
			Device dev = mDevMgr.getDevice(index);
			String sMac = dev.getMac();
			byte aMac[] = sMac.getBytes();
			byte macLen = (byte)aMac.length;
			byte aCmd[] = dev.lightIncreaseCmd();
			byte cmdLen = (byte)aCmd.length;
			Log.d(TAG, this.getClass().toString() + " lightIncreaseRequest() " + Utility.data2String(aCmd, cmdLen));
			if (dev.getRegisted()) {
				String ip = dev.getWanIP();
				int port = dev.getWanPort();
				byte[] tx = aCmd;
				//if (port == 0) {
					ip = Protocol.SERVER_HOST;
					port = Protocol.SERVER_PORT;
					byte vlen = (byte)(1 + nameLen + 1 + macLen + 1 + cmdLen);
					byte[] v = new byte[vlen];
					v[0] = nameLen;
					for(int j=0; j<nameLen; j++) {
						v[1+j] = aName[j];
					}
					v[nameLen+1] = macLen;
					for(int j=0; j<macLen; j++) {
						v[nameLen+1+1+j] = aMac[j];
					}
					v[nameLen+1+1+macLen] = cmdLen;
					for(int j=0; j<cmdLen; j++) {
						v[nameLen+1+1+macLen+1+j] = aCmd[j];
					}
					tx = dataGenerate(addr, code, offset, vlen, v);
				//}
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", ip);
				udpBdl.putInt("port", port);
				udpBdl.putInt("len", tx.length);
				udpBdl.putByteArray("tx", tx);
				udpMsg.setData(udpBdl);
				Log.d(TAG, this.getClass().toString() + " lightIncreaseRequest() " + Utility.data2String(tx, tx.length));
				sendUdpMsg(udpMsg);
			} else {
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", dev.getLanIP());
				udpBdl.putInt("port", dev.getLanPort());
				udpBdl.putInt("len", cmdLen);
				udpBdl.putByteArray("tx", aCmd);
				udpMsg.setData(udpBdl);
				sendUdpMsg(udpMsg);
			}
		}
	}
	
	//private void lightIncreaseResponse(Message msg) {
	//}
	
	private void lightDecreaseRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|cmdlen(1B)|cmd(xB)|crc(2B)
		String sName = mUser.getName();
		byte aName[] = sName.getBytes();
		byte nameLen = (byte)aName.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.CMD_TRANSLATE;
		String sDevsIndex = mDevMgr.getCheckDevsIndex();
		Log.d(TAG, this.getClass().toString() + " lightDecreaseRequest() sDevsIndex:" + sDevsIndex);
		String aIndex[] = sDevsIndex.split("\\,");
		for (int i=0; i<aIndex.length; i++) {
			if (aIndex[i].equals("")) {
				continue;
			}
			int index = Integer.parseInt(aIndex[i], 10);
			Device dev = mDevMgr.getDevice(index);
			String sMac = dev.getMac();
			byte aMac[] = sMac.getBytes();
			byte macLen = (byte)aMac.length;
			byte aCmd[] = dev.lightDecreaseCmd();
			byte cmdLen = (byte)aCmd.length;
			Log.d(TAG, this.getClass().toString() + " lightDecreaseRequest() " + Utility.data2String(aCmd, cmdLen));
			if (dev.getRegisted()) {
				String ip = dev.getWanIP();
				int port = dev.getWanPort();
				byte[] tx = aCmd;
				//if (port == 0) {
					ip = Protocol.SERVER_HOST;
					port = Protocol.SERVER_PORT;
					byte vlen = (byte)(1 + nameLen + 1 + macLen + 1 + cmdLen);
					byte[] v = new byte[vlen];
					v[0] = nameLen;
					for(int j=0; j<nameLen; j++) {
						v[1+j] = aName[j];
					}
					v[nameLen+1] = macLen;
					for(int j=0; j<macLen; j++) {
						v[nameLen+1+1+j] = aMac[j];
					}
					v[nameLen+1+1+macLen] = cmdLen;
					for(int j=0; j<cmdLen; j++) {
						v[nameLen+1+1+macLen+1+j] = aCmd[j];
					}
					tx = dataGenerate(addr, code, offset, vlen, v);
				//}
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", ip);
				udpBdl.putInt("port", port);
				udpBdl.putInt("len", tx.length);
				udpBdl.putByteArray("tx", tx);
				udpMsg.setData(udpBdl);
				Log.d(TAG, this.getClass().toString() + " lightDecreaseRequest() " + Utility.data2String(tx, tx.length));
				sendUdpMsg(udpMsg);
			} else {
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", dev.getLanIP());
				udpBdl.putInt("port", dev.getLanPort());
				udpBdl.putInt("len", cmdLen);
				udpBdl.putByteArray("tx", aCmd);
				udpMsg.setData(udpBdl);
				sendUdpMsg(udpMsg);
			}
		}
	}
	
	//private void lightDecreaseResponse(Message msg) {
	//}
	
	private void lightRandomRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|cmdlen(1B)|cmd(xB)|crc(2B)
		String sName = mUser.getName();
		byte aName[] = sName.getBytes();
		byte nameLen = (byte)aName.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.CMD_TRANSLATE;
		String sDevsIndex = mDevMgr.getCheckDevsIndex();
		Log.d(TAG, this.getClass().toString() + " lightRandomRequest() sDevsIndex:" + sDevsIndex);
		String aIndex[] = sDevsIndex.split("\\,");
		for (int i=0; i<aIndex.length; i++) {
			if (aIndex[i].equals("")) {
				continue;
			}
			int index = Integer.parseInt(aIndex[i], 10);
			Device dev = mDevMgr.getDevice(index);
			String sMac = dev.getMac();
			byte aMac[] = sMac.getBytes();
			byte macLen = (byte)aMac.length;
			byte aCmd[] = dev.lightRandomCmd();
			byte cmdLen = (byte)aCmd.length;
			Log.d(TAG, this.getClass().toString() + " lightRandomRequest() " + Utility.data2String(aCmd, cmdLen));
			if (dev.getRegisted()) {
				String ip = dev.getWanIP();
				int port = dev.getWanPort();
				byte[] tx = aCmd;
				//if (port == 0) {
					ip = Protocol.SERVER_HOST;
					port = Protocol.SERVER_PORT;	
					byte vlen = (byte)(1 + nameLen + 1 + macLen + 1 + cmdLen);
					byte[] v = new byte[vlen];
					v[0] = nameLen;
					for(int j=0; j<nameLen; j++) {
						v[1+j] = aName[j];
					}
					v[nameLen+1] = macLen;
					for(int j=0; j<macLen; j++) {
						v[nameLen+1+1+j] = aMac[j];
					}
					v[nameLen+1+1+macLen] = cmdLen;
					for(int j=0; j<cmdLen; j++) {
						v[nameLen+1+1+macLen+1+j] = aCmd[j];
					}
					tx = dataGenerate(addr, code, offset, vlen, v);
				//}
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", ip);
				udpBdl.putInt("port", port);
				udpBdl.putInt("len", tx.length);
				udpBdl.putByteArray("tx", tx);
				udpMsg.setData(udpBdl);
				Log.d(TAG, this.getClass().toString() + " lightRandomRequest() " + Utility.data2String(tx, tx.length));
				sendUdpMsg(udpMsg);
			} else {
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", dev.getLanIP());
				udpBdl.putInt("port", dev.getLanPort());
				udpBdl.putInt("len", cmdLen);
				udpBdl.putByteArray("tx", aCmd);
				udpMsg.setData(udpBdl);
				sendUdpMsg(udpMsg);
			}
		}
	}
	
	//private void lightRandomResponse(Message msg) {
	//}
	
	private void lightBluetoothRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|cmdlen(1B)|cmd(xB)|crc(2B)
		String sName = mUser.getName();
		byte aName[] = sName.getBytes();
		byte nameLen = (byte)aName.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.CMD_TRANSLATE;
		String sDevsIndex = mDevMgr.getCheckDevsIndex();
		Log.d(TAG, this.getClass().toString() + " lightBluetoothRequest() sDevsIndex:" + sDevsIndex);
		String aIndex[] = sDevsIndex.split("\\,");
		for (int i=0; i<aIndex.length; i++) {
			if (aIndex[i].equals("")) {
				continue;
			}
			int index = Integer.parseInt(aIndex[i], 10);
			Device dev = mDevMgr.getDevice(index);
			String sMac = dev.getMac();
			byte aMac[] = sMac.getBytes();
			byte macLen = (byte)aMac.length;
			byte aCmd[] = dev.lightBluetoothCmd();
			byte cmdLen = (byte)aCmd.length;
			Log.d(TAG, this.getClass().toString() + " lightBluetoothRequest() " + Utility.data2String(aCmd, cmdLen));
			if (dev.getRegisted()) {
				String ip = dev.getWanIP();
				int port = dev.getWanPort();
				byte[] tx = aCmd;
				//if (port == 0) {
					ip = Protocol.SERVER_HOST;
					port = Protocol.SERVER_PORT;
					byte vlen = (byte)(1 + nameLen + 1 + macLen + 1 + cmdLen);
					byte[] v = new byte[vlen];
					v[0] = nameLen;
					for(int j=0; j<nameLen; j++) {
						v[1+j] = aName[j];
					}
					v[nameLen+1] = macLen;
					for(int j=0; j<macLen; j++) {
						v[nameLen+1+1+j] = aMac[j];
					}
					v[nameLen+1+1+macLen] = cmdLen;
					for(int j=0; j<cmdLen; j++) {
						v[nameLen+1+1+macLen+1+j] = aCmd[j];
					}
					tx = dataGenerate(addr, code, offset, vlen, v);
				//}
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", ip);
				udpBdl.putInt("port", port);
				udpBdl.putInt("len", tx.length);
				udpBdl.putByteArray("tx", tx);
				udpMsg.setData(udpBdl);
				Log.d(TAG, this.getClass().toString() + " lightBluetoothRequest() " + Utility.data2String(tx, tx.length));
				sendUdpMsg(udpMsg);
			} else {
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", dev.getLanIP());
				udpBdl.putInt("port", dev.getLanPort());
				udpBdl.putInt("len", cmdLen);
				udpBdl.putByteArray("tx", aCmd);
				udpMsg.setData(udpBdl);
				sendUdpMsg(udpMsg);
			}
		}
	}
	
	//private void lightBluetoothResponse(Message msg) {
	//}
	
	private void lightMicphoneRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|namelen(1B)|name(xB)|maclen(1B)|mac(12B)|cmdlen(1B)|cmd(xB)|crc(2B)
		String sName = mUser.getName();
		byte aName[] = sName.getBytes();
		byte nameLen = (byte)aName.length;
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_NETWORK_CTRL;
		short offset = Protocol.CMD_TRANSLATE;
		String sDevsIndex = mDevMgr.getCheckDevsIndex();
		Log.d(TAG, this.getClass().toString() + " lightMicphoneRequest() sDevsIndex:" + sDevsIndex);
		String aIndex[] = sDevsIndex.split("\\,");
		for (int i=0; i<aIndex.length; i++) {
			if (aIndex[i].equals("")) {
				continue;
			}
			int index = Integer.parseInt(aIndex[i], 10);
			Device dev = mDevMgr.getDevice(index);
			String sMac = dev.getMac();
			byte aMac[] = sMac.getBytes();
			byte macLen = (byte)aMac.length;
			byte aCmd[] = dev.lightMicphoneCmd();
			byte cmdLen = (byte)aCmd.length;
			Log.d(TAG, this.getClass().toString() + " lightMicphoneRequest() " + Utility.data2String(aCmd, cmdLen));
			if (dev.getRegisted()) {
				String ip = dev.getWanIP();
				int port = dev.getWanPort();
				byte[] tx = aCmd;
				//if (port == 0) {
					ip = Protocol.SERVER_HOST;
					port = Protocol.SERVER_PORT;
					byte vlen = (byte)(1 + nameLen + 1 + macLen + 1 + cmdLen);
					byte[] v = new byte[vlen];
					v[0] = nameLen;
					for(int j=0; j<nameLen; j++) {
						v[1+j] = aName[j];
					}
					v[nameLen+1] = macLen;
					for(int j=0; j<macLen; j++) {
						v[nameLen+1+1+j] = aMac[j];
					}
					v[nameLen+1+1+macLen] = cmdLen;
					for(int j=0; j<cmdLen; j++) {
						v[nameLen+1+1+macLen+1+j] = aCmd[j];
					}
					tx = dataGenerate(addr, code, offset, vlen, v);
				//}
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", ip);
				udpBdl.putInt("port", port);
				udpBdl.putInt("len", tx.length);
				udpBdl.putByteArray("tx", tx);
				udpMsg.setData(udpBdl);
				Log.d(TAG, this.getClass().toString() + " lightMicphoneRequest() " + Utility.data2String(tx, tx.length));
				sendUdpMsg(udpMsg);
			} else {
				Message udpMsg = Message.obtain();
				Bundle udpBdl = new Bundle();
				udpBdl.putString("ip", dev.getLanIP());
				udpBdl.putInt("port", dev.getLanPort());
				udpBdl.putInt("len", cmdLen);
				udpBdl.putByteArray("tx", aCmd);
				udpMsg.setData(udpBdl);
				sendUdpMsg(udpMsg);
			}
		}
	}
	
	//private void lightMicphoneResponse(Message msg) {
	//}
	
	private void lightLanscanRequest(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|ip(4B)|port(4B)|crc(2B)
		int ip = getLocalIP();
		int bc = getBroadcastIP();
		Log.d(TAG, this.getClass().toString() + " lightLanscanRequest ip:" + ip + " bc:" + bc);
		if (ip == 0 || bc == 0) {
			return;
		}
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_LANSCAN;
		short offset = (short)0x0000;

		byte count = (byte)0x08;
		byte[] a = Utility.int2Bytes(ip);
		byte[] b = Utility.int2Bytes(Protocol.BROADCAST_PORT);
		byte[] v = new byte[count];
		v[0] = a[0];
		v[1] = a[1];
		v[2] = a[2];
		v[3] = a[3];
		v[4] = b[0];
		v[5] = b[1];
		v[6] = b[2];
		v[7] = b[3];
		byte[] tx = dataGenerate(addr, code, offset, count, v);
		Message udpMsg = Message.obtain();
		Bundle udpBdl = new Bundle();
		udpBdl.putString("ip", Utility.int2Ip(bc));
		udpBdl.putInt("port", Protocol.BROADCAST_PORT);
		udpBdl.putInt("len", tx.length);
		udpBdl.putByteArray("tx", tx);
		udpMsg.setData(udpBdl);
		Log.d(TAG, this.getClass().toString() + " lightLanscanRequest() " + Utility.data2String(tx, tx.length));
		sendUdpMsg(udpMsg);
	}
	
	private void lightLanscanResponse(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|mac(6B)|crc(2B)
		Bundle b = msg.getData();
		String lanIP = b.getString("ip");
		int lanPort = Protocol.BROADCAST_PORT;//b.getInt("port");
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		if(len > 14) {
			byte valLen = rx[7];
			Log.d(TAG, this.getClass().toString() + " lightLanscanResponse() valLen:" + valLen);
			byte macLen = 6;
			if (valLen != macLen) {
				return;
			}
			String sMac = "";
			for(int i=0; i<macLen; i++) {
				sMac += String.format("%02X", rx[8+i]);
			}
			Log.d(TAG, this.getClass().toString() + " lightLanscanResponse() macLen:" + macLen + " mac:" + sMac);

			Device dev = mDevMgr.getDevice(sMac);
			if(dev == null) {
				Device newDev = new Device();
				newDev.setMac(sMac);
				newDev.setName(sMac);
				newDev.setLanIP(lanIP);
				newDev.setLanPort(lanPort);
				String sPath = mDevsDir + "/" + sMac + ".ini";
				newDev.setPath(sPath);
				mDevMgr.addDevice(newDev);
			} else {
				dev.setLanIP(lanIP);
				dev.setLanPort(lanPort);
			}
			Message devMsg = Message.obtain();
			devMsg.what = Protocol.LIGHT_LANSCAN_RES;
			sendForeignMsg(devMsg);
		}
	}
	
	private void dataReceive(Message msg) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|...|crc(2B)
		Bundle b = msg.getData();
		String ip = b.getString("ip");
		int localIP = getLocalIP();
		int remoteIP = Utility.ip2Int(ip);
		Log.d(TAG, this.getClass().toString() + " dataReceive() localIP:"+ localIP + " remoteIP:" + remoteIP);
		if (localIP == remoteIP) {
			return;
		}
		int port = b.getInt("port");
		int len = b.getInt("len");
		byte rx[] = b.getByteArray("rx");
		Log.d(TAG, this.getClass().toString() + " dataReceive() "+ ip + ":"+ port+ " " + Utility.data2String(rx, len));
		boolean lenValid = Utility.isSizeValid(rx, len);
		boolean crcValid = Utility.isCrcValid(rx, len);
		Log.d(TAG, this.getClass().toString() + " dataReceive() sizevalid:" + lenValid + " crcvalid:" + crcValid);
		if(lenValid && crcValid) {
			byte code = rx[4];
			if(code == Protocol.CODE_NETWORK_CTRL) {
				short offset = 0;
				byte aOffset[] = new byte[2];
				aOffset[0] = rx[5];
				aOffset[1] = rx[6];
				offset = Utility.bytes2Short(aOffset);
				Log.d(TAG, this.getClass().toString() + " dataReceive() network ctrl offset:" + offset);
				switch(offset) {
				case Protocol.USR_REGISTER:
					userRegisterResponse(msg);
					break;
				case Protocol.USR_UNREGISTER:
					userUnregisterResponse(msg);
					break;
				case Protocol.USR_LOGON:
					userLogonResponse(msg);
					break;
				case Protocol.USR_LOGOFF:
					userLogoffResponse(msg);
					break;
				case Protocol.USR_DEV_REGISTER:
					userDevRegisterResponse(msg);
					break;
				case Protocol.USR_DEV_UNREGISTER:
					userDevUnregisterResponse(msg);
					break;
				case Protocol.USR_CON_DEV:
					userConnectDevResponse(msg);
					break;
				case Protocol.USR_DEV_REGLIST:
					userDevRegListResponse(msg);
					break;
				default:
					break;
				}
			} else if(code == Protocol.CODE_LANSCAN) {
				short offset = 0;
				byte aOffset[] = new byte[2];
				aOffset[0] = rx[5];
				aOffset[1] = rx[6];
				offset = Utility.bytes2Short(aOffset);
				Log.d(TAG, this.getClass().toString() + " dataReceive() lan scan offset:" + offset);
				switch (offset) {
				case 0x0000:
					lightLanscanResponse(msg);
					break;
				default:
					break;
				}
			}
		}
	}
	
	private void userSetRequest(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userSetRequest()");
		Bundle b = msg.getData();
		String name = b.getString("name");
		String pwd = b.getString("pwd");
		int status = b.getInt("status");
		mUser.setName(name);
		mUser.setPwd(pwd);
		mUser.setStatus(status);
	}
	
	private void userInfoRequest(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userInfoRequest()");
		Message usrMsg = Message.obtain();
		usrMsg.what = Protocol.USR_INFO_RES;
		Bundle b = new Bundle();
		b.putString("name", mUser.getName());
		b.putString("pwd", mUser.getPwd());
		b.putInt("status", mUser.getStatus());
		usrMsg.setData(b);
		sendForeignMsg(usrMsg);
	}
	
	private void userDevAddRequest(Message msg) {
		Bundle b = msg.getData();
		String sMac = b.getString("mac");
		String sName = b.getString("name");
		Device newDev = new Device();
		newDev.setMac(sMac);
		newDev.setName(sName);
		String sPath = mDevsDir + "/" + sMac + ".ini";
		newDev.setPath(sPath);
		mDevMgr.addDevice(newDev);
		
		Message devMsg = Message.obtain();
		devMsg.what = Protocol.USR_DEV_ADD_RES;
		sendForeignMsg(devMsg);
	}
	
	private void userDevEdtRequest(Message msg) {
		Bundle b = msg.getData();
		int index = b.getInt("index");
		String sName = b.getString("name");
		mDevMgr.setDevName(index, sName);

		Message devMsg = Message.obtain();
		devMsg.what = Protocol.USR_DEV_EDT_RES;
		sendForeignMsg(devMsg);
	}
	
	private void userDevDelRequest(Message msg) {
		Bundle b = msg.getData();
		int index = b.getInt("index");
		mDevMgr.delDevice(index);
		
		Message devMsg = Message.obtain();
		devMsg.what = Protocol.USR_DEV_DEL_RES;
		sendForeignMsg(devMsg);
	}
	
	private void userDevChkRequest(Message msg) {
		Bundle b = msg.getData();
		int index = b.getInt("index");
		boolean check = b.getBoolean("check");
		mDevMgr.setDevCheck(index, check);

		Message devMsg = Message.obtain();
		devMsg.what = Protocol.USR_DEV_CHK_RES;
		sendForeignMsg(devMsg);
	}
	
	private void userDevChkListRequest(Message msg) {
		String sDevsName = mDevMgr.getCheckDevsName();
		Log.d(TAG, this.getClass().toString() + " userDevChkListRequest() sDevsName:" + sDevsName);
		Message devMsg = Message.obtain();
		devMsg.what = Protocol.USR_DEV_CHKLIST_RES;
		Bundle b = new Bundle();
		b.putString("name", mUser.getName());
		b.putInt("status", mUser.getStatus());
		b.putString("devsname", sDevsName);
		devMsg.setData(b);
		sendForeignMsg(devMsg);
	}
	
	private void userDevCheckAllRequest(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userDevCheckAllRequest()");
		mDevMgr.checkAllDevs(true);
		Message devMsg = Message.obtain();
		devMsg.what = Protocol.USR_DEV_CHKALL_RES;
		sendForeignMsg(devMsg);
	}
	
	private void userDevUncheckAllRequest(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userDevUncheckAllRequest()");
		mDevMgr.checkAllDevs(false);
		Message devMsg = Message.obtain();
		devMsg.what = Protocol.USR_DEV_UNCHKALL_RES;
		sendForeignMsg(devMsg);
	}
	
	private void userDevCheckDelRequest(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userDevCheckDelRequest()");
		mDevMgr.checkDelDevs();
		Message devMsg = Message.obtain();
		devMsg.what = Protocol.USR_DEV_CHKDEL_RES;
		sendForeignMsg(devMsg);
	}
	
	private void userDevCountRequest(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userDevCountRequest()");
		Message devMsg = Message.obtain();
		devMsg.what = Protocol.USR_DEV_COUNT_RES;
		devMsg.arg1 = mDevMgr.getDevsCount();
		devMsg.arg2 = mDevMgr.getDevsCheckCount();
		sendForeignMsg(devMsg);
	}
}
