package com.aladin.devnetctrl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utility {
	//convert int to byte array with big-endian
	public static byte[] int2Bytes(int n) {
		byte[] b = new byte[4];
		b[3] = (byte) (n & 0xff);
		b[2] = (byte) (n >> 8 & 0xff);
		b[1] = (byte) (n >> 16 & 0xff);
		b[0] = (byte) (n >> 24 & 0xff);
		return b;
	}
	
	//convert short to byte array with big-endian
	public static byte[] short2Bytes(short n) {
		byte[] b = new byte[2];
		b[1] = (byte) (n & 0xff);
		b[0] = (byte) (n >> 8 & 0xff);
		return b;
	}
	
	public static short bytes2Short(byte[] b){
		return (short)( b[1] & 0xff | (b[0] & 0xff) << 8 );
	}
	
	public static int bytes2Int(byte b[]) {
		return b[3] & 0xff | (b[2] & 0xff) << 8 | (b[1] & 0xff) << 16 | (b[0] & 0xff) << 24;
	}
	
	public static String int2Ip(int ip) {
		String strIp =  ((ip & 0xff) + "." + (ip >> 8 & 0xff) + "." + (ip >> 16 & 0xff) + "." + (ip >> 24 & 0xff));
		return strIp;
	}
	
	public static int ip2Int(String ip) {
	    String[] a = ip.split("\\.");
	    int v = (Integer.valueOf(a[3]) << 24) | (Integer.valueOf(a[2]) << 16) | (Integer.valueOf(a[1]) << 8) | Integer.valueOf(a[0]);
	    return v;
	}
	
	public static String mac2String(byte[] mac) {
		String strMac = "";
		for (int i=0; i<mac.length; i++) {
			strMac = strMac + String.format("%02X-", mac[i]);
		}
		if (strMac.length() > 1) {
			strMac = strMac.substring(0, strMac.length()-1);
		}
		return strMac;
	}
	
	public static short crc16(byte []data, int size)
	{
		short i = 0, j = 0;
		short crc = (short)0xffff;
		short poly = (short)0xa001;
		for(i=0; i<size; i++) {
			crc = (short) (crc ^ (data[i] & 0xff));
			for(j=0; j<8; j++) {
				if ((crc & 0x0001) == 0x00001)
					crc = (short) (((crc & 0xffff) >> 1) ^ (poly & 0xffff));
				else
					crc = (short) ((crc & 0xffff) >> 1);
			}
		}
		return crc;
	}
	
	public static String data2String(byte []data, int size) {
		int i = 0;
		String strData = "";
		for(i=0; i<size; i++) {
			strData = strData + String.format("%02X ", data[i]);
		}
		return strData;
	}
	
	public static boolean isMacValid(String strMac) {
		boolean valid = false;
		if ((strMac != null) && (strMac.length() == 12)) {
			valid = true;
			for (int i=0; i<strMac.length(); i++) {
				char c = strMac.charAt(i);
				if('0' <= c && c <= '9') {
				} else if('A' <= c && c <= 'F') {
				} else if('a' <= c && c <= 'f') {
				} else {
					valid = false;
					break;
				}
			}
		}
		return valid;
	}
    
	public static boolean isIpValid(String strIp) {
		boolean valid = false;
		String[] aIp = strIp.split("\\.");
		if (aIp.length == 4) {
			for (int i=0; i<aIp.length; i++) {
				valid = false;
				if (aIp[i].equals("")) {
					break;
				} else {
					try {
						int v = Integer.parseInt(aIp[i]);
						if (0x00<=v && v<=0xFF) {
							valid = true;
						}
					} catch (NumberFormatException e) {
						break;
					}
				}
			}
		}
		return valid;
	}
    
	public static boolean isSizeValid(byte[] data, int size) {
		//data: address(2B)|size(2B)|code(1B)|offset(2B)|count(1B)|value(xB)|crc(2B)
		boolean valid = false;
		if (size > 7) {
	    	byte[] a = new byte[2];
	    	a[0] = data[2];
	    	a[1] = data[3];
	    	short v = bytes2Short(a);
	    	if (v == size) {
	    		valid = true;
	    	}
		}
		return valid;
	}
    
	public static boolean isCrcValid(byte[] data, int size) {
		//data: address(2B)|size(2B)|code(1B)|offset(2B)|count(1B)|value(xB)|crc(2B)
		boolean valid = false;
		if (size > 2) {
	    	short crc = crc16(data, size-2);
	    	byte[] a = new byte[2];
	    	a[0] = data[size-2];
	    	a[1] = data[size-1];
	    	short v = bytes2Short(a);
	    	if (crc == v) {
	    		valid = true;
	    	}
		}
		return valid;
	}
    
	public static byte[] dataGenerate(short addr, byte code, short offset, byte count, byte[] value) {
		//addr(2B)|size(2B)|code(1B)|offset(2B)|vallen(1B)|...|crc(2B)
		short size = (short)(2 + 2 + 1 + 2 + 1 + count + 2);
		byte[] tx = new byte[size];
		byte[] a = short2Bytes(addr);
		tx[0] = a[0];
		tx[1] = a[1];
		byte[] b = short2Bytes(size);
		tx[2] = b[0];
		tx[3] = b[1];
		tx[4] = code;
		byte[] c = short2Bytes(offset);
		tx[5] = c[0];
		tx[6] = c[1];
		tx[7] = count;
		for(int i=0; i<count; i++) {
			tx[8+i] = value[i];
		}
		short crc = crc16(tx, size-2);
		byte[] d = short2Bytes(crc);
		tx[size-2] = d[0];
		tx[size-1] = d[1];
		return tx;
	}
	
	public static String getMD5(String val) {
		String sMD5 = "";
		try {
			if (val != "") {
				MessageDigest md5;
				md5 = MessageDigest.getInstance("MD5");
				md5.update(val.getBytes());
				byte[] m = md5.digest();
				for(int i=0; i<m.length; i++) {
					sMD5 += String.format("%02X", m[i]);
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return sMD5;
	}
}
