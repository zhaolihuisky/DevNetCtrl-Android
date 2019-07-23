package com.aladin.devnetctrl;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class UdpCom {
	private static String TAG = "DevNetCtrl";
	private UdpRecvThread mRecvThread = null;
	private boolean mRecvExit = false;
	private UdpSendHandler mSendHandler = null;
	private HandlerThread mSendHandlerThread = null;
	
	private DatagramSocket mUdpSocket = null;
	private final int mBufSize = 1024;
	
	private Handler mServiceHandler = null;
	
	public UdpCom() {
		mRecvThread = null;
		mRecvExit = false;
		mSendHandler = null;

		mUdpSocket = null;
	}
	
	public void createUdp() {
		try {
			mUdpSocket = new DatagramSocket(Protocol.BROADCAST_PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void destroyUdp() {
		if(mUdpSocket != null) {
			mUdpSocket.close();
		}
	}
	
	public void setServiceHandler(Handler handler) {
		mServiceHandler = handler;
	}
	
	class UdpRecvThread extends Thread {
		@Override
		public void run() {
			byte[] buf = new byte[mBufSize];
			while(mRecvExit == false) {
				try {
					if (mUdpSocket == null) {
						mRecvExit = true;
						Log.d(TAG, this.getClass().toString() + " mUdpSocket is null");
						continue;
					}
					mUdpSocket.setSoTimeout(5000);
					DatagramPacket recvPacket = new DatagramPacket(buf, mBufSize);
					mUdpSocket.receive(recvPacket);
					String ip = recvPacket.getAddress().getHostAddress();
					int port = recvPacket.getPort();
					int len = recvPacket.getLength();
					String str = "recvfrom " + ip + ":" + port + " len:" + len + " buf:" + Utility.data2String(buf, len);
					Log.d(TAG, this.getClass().toString() + " run " + str);
					Message msg = Message.obtain();
					msg.what = Protocol.DATA_RECEIVE;
					Bundle b = new Bundle();
					b.putString("ip", ip);
					b.putInt("port", port);
					b.putInt("len", len);
					byte[] rx = new byte[len];
					for(int i=0; i<len; i++) {
						rx[i] = buf[i];
					}
					b.putByteArray("rx", rx);
					msg.setData(b);
					sendServiceMsg(msg);
				} catch (InterruptedIOException e) {
					Log.d(TAG, this.getClass().toString() + " run receive timeout");
					e.printStackTrace();
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	class UdpSendHandler extends Handler {
		public UdpSendHandler() {
		}
		
		public UdpSendHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, this.getClass().toString() + " handleMessage thrdID:" + Thread.currentThread().getId() + " msg:" + msg.toString());
			Bundle b = msg.getData();
			handleBundle(b);
		}
		
		private void handleBundle(Bundle b) {
			String ip = b.getString("ip");
			int port = b.getInt("port");
			int len = b.getInt("len");
			byte[] tx = b.getByteArray("tx");
			Log.d(TAG, this.getClass().toString() + " handleBundle ip:" + ip + " port:" + port + " tx:" + Utility.data2String(tx, len));
			try {
				InetAddress address = InetAddress.getByName(ip);
				DatagramPacket sendPacket = new DatagramPacket(tx, len, address, port);
				if (mUdpSocket != null) {
					mUdpSocket.send(sendPacket);
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Log.d(TAG, this.getClass().toString() + " handleBundle send UnknownHostException " + e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				Log.d(TAG, this.getClass().toString() + " handleBundle send IOException " + e.getMessage());
			}
		}
	}
	
	public void startRecv() {
		//start udp receive thread
		Log.d(TAG, this.getClass().toString() + " startRecv");
		mRecvThread = new UdpRecvThread();
		mRecvThread.start();
		
	}
	
	public void stopRecv() {
		Log.d(TAG, this.getClass().toString() + " stopRecv");
		mRecvExit = true;
		try {
			mRecvThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void startSend() {
		//android application supply the handler thread
		mSendHandlerThread = new HandlerThread("SendHandlerThread");
		//the handler thread's start function must call before handler thread's getLooper function
		mSendHandlerThread.start();
		mSendHandler = new UdpSendHandler(mSendHandlerThread.getLooper());
		Log.d(TAG, this.getClass().toString() + " startSend HandlerThread thrdID:" + mSendHandlerThread.getId());
	}
	
	public void stopSend() {
		Log.d(TAG, this.getClass().toString() + " stopSend");
		mSendHandlerThread.quit();
	}
	
	public void sendMsg(Message msg) {
		if(mSendHandler != null) {
			mSendHandler.sendMessage(msg);
		}
	}
	
	public void sendServiceMsg(Message msg) {
		if(mServiceHandler != null) {
			mServiceHandler.sendMessage(msg);
		}
	}
}
