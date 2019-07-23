package com.aladin.devnetctrl;

import java.io.File;
import java.io.IOException;
import android.graphics.Color;

public class Device {
	private String mMac;
	private String mWanIP;
	private int mWanPort;
	private String mLanIP;
	private int mLanPort;
	private boolean mCheck;
	
	private String mName;
	private boolean mRegisted;
	private String mPath;
	
	public Device() {
		mMac = "";
		mWanIP = "0.0.0.0";
		mWanPort = 0;
		mLanIP = "0.0.0.0";
		mLanPort = 0;
		mCheck = false;
		mName = "";
		mRegisted = false;
		mPath = "";
	}
	
	public void setMac(String mac) {
		mMac = mac;
	}
	
	public String getMac() {
		return mMac;
	}
	
	public void setWanIP(String wanIP) {
		mWanIP = wanIP;
	}
	
	public String getWanIP() {
		return mWanIP;
	}
	
	public void setWanPort(int wanPort) {
		mWanPort = wanPort;
	}
	
	public int getWanPort() {
		return mWanPort;
	}
	
	public void setLanIP(String lanIP) {
		mLanIP = lanIP;
	}
	
	public String getLanIP() {
		return mLanIP;
	}
	
	public void setLanPort(int lanPort) {
		mLanPort = lanPort;
	}
	
	public int getLanPort() {
		return mLanPort;
	}
	
	public void setCheck(boolean check) {
		mCheck = check;
	}
	
	public boolean getCheck() {
		return mCheck;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}
	
	public void setRegisted(boolean registed) {
		mRegisted = registed;
	}
	
	public boolean getRegisted() {
		return mRegisted;
	}
	
	public void setPath(String sPath) {
		mPath = sPath;
	}
	
	public void saveMac() {
		try {
			IniFile.setProfileString(mPath, "DEV", "MAC", mMac);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveName() {
		try {
			IniFile.setProfileString(mPath, "DEV", "NAME", mName);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveCheck() {
		String sCheck = "0";
		if (mCheck) {
			sCheck = "1";
		}
		try {
			IniFile.setProfileString(mPath, "DEV", "CHECK", sCheck);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void save() {
		saveMac();
		saveName();
		saveCheck();
	}
	
	public boolean load() {
		boolean bRet = false;
		try {
			String sPath = mPath;
			String sMac = IniFile.getProfileString(sPath, "DEV", "MAC", "");
			String sName = IniFile.getProfileString(sPath, "DEV", "NAME", "");
			String sChk = IniFile.getProfileString(sPath, "DEV", "CHECK", "");
			//String s = " mac:" + strMac + " ip:" + strIp + " name:" + strName + " check:" + strChk;
			if (sMac.equals("") || sChk.equals("")) {
			} else {
				try {
					int check = Integer.parseInt(sChk);
					mMac = sMac;
					mName = sName;
					mCheck = false;
					if (check == 1) {
						mCheck = true;
					}
					bRet = true;
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		return bRet;
	}
	
	public void delete() {
		File f = new File(mPath);
		if(f.exists()) {
			f.delete();
		}
	}
	
	public byte[] lightColorCmd(int color) {
		byte r = (byte) (Color.red(color)/2);
		byte g = (byte) (Color.green(color)/2);
		byte b = (byte) (Color.blue(color)/2);
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_LIGHT;
		short offset = 0x0001;
		byte count = (byte)0x06;
		byte[] v = new byte[count];
		v[0] = r;
		v[1] = g;
		v[2] = b;
		v[3] = 0x00;
		v[4] = 0x00;
		v[5] = 0x00;
		byte[] tx = Utility.dataGenerate(addr, code, offset, count, v);
		return tx;
	}
	
	public byte[] lightOnOffCmd(boolean b) {
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_ONOFF;
		short offset = (short)0x0001;
		if(b == false) {
			offset = (short)0x0002;
		}
		byte count = (byte)0x06;
		byte[] v = new byte[count];
		v[0] = 0x00;
		v[1] = 0x00;
		v[2] = 0x00;
		v[3] = 0x00;
		v[4] = 0x00;
		v[5] = 0x00;
		byte[] tx = Utility.dataGenerate(addr, code, offset, count, v);
		return tx;
	}
	
	public byte[] lightAuto() {
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_LIGHT;
		short offset = (short)0x0007;
		byte count = (byte)0x06;
		byte[] v = new byte[count];
		v[0] = 0x00;
		v[1] = 0x00;
		v[2] = 0x00;
		v[3] = 0x00;
		v[4] = 0x00;
		v[5] = 0x00;
		byte[] tx = Utility.dataGenerate(addr, code, offset, count, v);
		return tx;
	}
	
	public byte[] lightIncreaseCmd() {
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_LIGHT;
		short offset = (short)0x0005;
		byte count = (byte)0x06;
		byte[] v = new byte[count];
		v[0] = 0x00;
		v[1] = 0x00;
		v[2] = 0x00;
		v[3] = 0x00;
		v[4] = 0x00;
		v[5] = 0x00;
		byte[] tx = Utility.dataGenerate(addr, code, offset, count, v);
		return tx;
	}
	
	public byte[] lightDecreaseCmd() {
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_LIGHT;
		short offset = (short)0x0006;
		byte count = (byte)0x06;
		byte[] v = new byte[count];
		v[0] = 0x00;
		v[1] = 0x00;
		v[2] = 0x00;
		v[3] = 0x00;
		v[4] = 0x00;
		v[5] = 0x00;
		byte[] tx = Utility.dataGenerate(addr, code, offset, count, v);
		return tx;
	}
	
	public byte[] lightRandomCmd() {
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_LIGHT;
		short offset = (short)0x0003;
		byte count = (byte)0x06;
		byte[] v = new byte[count];
		v[0] = 0x00;
		v[1] = 0x00;
		v[2] = 0x00;
		v[3] = 0x00;
		v[4] = 0x00;
		v[5] = 0x00;
		byte[] tx = Utility.dataGenerate(addr, code, offset, count, v);
		return tx;
	}
	
	public byte[] lightBluetoothCmd() {
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_BLUETOOTH;
		short offset = (short)0x0001;
		byte count = (byte)0x06;
		byte[] v = new byte[count];
		v[0] = 0x00;
		v[1] = 0x00;
		v[2] = 0x00;
		v[3] = 0x00;
		v[4] = 0x00;
		v[5] = 0x00;
		byte[] tx = Utility.dataGenerate(addr, code, offset, count, v);
		return tx;
	}
	
	public byte[] lightMicphoneCmd() {
		short addr = (short)0xFFFF;
		byte code = Protocol.CODE_MICPHONE;
		short offset = (short)0x0001;
		byte count = (byte)0x06;
		byte[] v = new byte[count];
		v[0] = 0x00;
		v[1] = 0x00;
		v[2] = 0x00;
		v[3] = 0x00;
		v[4] = 0x00;
		v[5] = 0x00;
		byte[] tx = Utility.dataGenerate(addr, code, offset, count, v);
		return tx;
	}
}
