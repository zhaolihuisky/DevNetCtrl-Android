package com.aladin.devnetctrl;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private static String TAG = "DevNetCtrl";
	private static final int USR_REGISTER_SUCCESS = 0x00;
	private static final int USR_NAME_TOOLONG = 0x01;
	private static final int USR_NAME_INVALID = 0x02;
	private static final int USR_NAME_EXIST = 0x03;
	private static final int USR_PWD_TOOLONG = 0x04;
	
	private static final int USR_NAME_SIZE = 32;
	private static final int USR_PWD_SIZE = 64;
	
	private String mUserName = null;
	private String mUserPwd = null;
	private boolean mIsRempwd = false;
	
	private LoginHandler mHandler = null;
	private Handler mServiceHandler = null;
	private NetctrlService.NetctrlBinder mBinder = null;
	
	private EditText etUserName = null;
	private EditText etUserPwd = null;
	private CheckBox cbRempwd = null;
	private Button bnUserRegister = null;
	private Button bnUserLogon = null;
	private ProgressThread mProgressThread = null;
	
	private ServiceConnection mSrvCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBinder = (NetctrlService.NetctrlBinder)service;
			mServiceHandler = mBinder.getHandler();
			mBinder.setForeignHandler(mHandler);
			Log.d(TAG, this.getClass().toString() + " onServiceConnected componentname:" + name);
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mServiceHandler = null;
			Log.d(TAG, this.getClass().toString() + " onServiceDisconnected ");
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		mHandler = new LoginHandler();
		Intent intent = new Intent(this, NetctrlService.class);
		bindService(intent, mSrvCon, Context.BIND_AUTO_CREATE);
		Log.d(TAG, this.getClass().toString() + " onCreate ");
		
		etUserName = (EditText)findViewById(R.id.user_name_et);
		etUserPwd = (EditText)findViewById(R.id.user_pwd_et);
		cbRempwd = (CheckBox)findViewById(R.id.user_rempwd_cb);
		loadUser();
		if (mIsRempwd) {
			etUserName.setText(mUserName);
			etUserPwd.setText(mUserPwd);
			cbRempwd.setChecked(true);
		}
		
		bnUserRegister = (Button)findViewById(R.id.user_register_bn);
		bnUserLogon = (Button)findViewById(R.id.user_login_bn);
		
		bnUserRegister.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mUserName = etUserName.getText().toString();
				mUserPwd = etUserPwd.getText().toString();
				boolean bName = adjustName(mUserName);
				boolean bPwd = adjustPwd(mUserPwd);
				if (bName && bPwd) {
					Message msg = Message.obtain();
					msg.what = Protocol.USR_REGISTER_REQ;
					Bundle b = new Bundle();
					b.putString("name", mUserName);
					b.putString("pwd", mUserPwd);
					msg.setData(b);
					sendServiceMsg(msg);
					String sTitle = getResources().getString(R.string.user_register);
					String sText = getResources().getString(R.string.user_connect_server);
					startProgress(sTitle, sText);
				}
			}
		});
		
		bnUserLogon.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mUserName = etUserName.getText().toString();
				mUserPwd = etUserPwd.getText().toString();
				boolean bName = adjustName(mUserName);
				boolean bPwd = adjustPwd(mUserPwd);
				if (bName && bPwd) {
					Message msg = Message.obtain();
					msg.what = Protocol.USR_LOGON_REQ;
					Bundle b = new Bundle();
					b.putString("name", mUserName);
					b.putString("pwd", mUserPwd);
					msg.setData(b);
					sendServiceMsg(msg);
					String sTitle = getResources().getString(R.string.user_logon);
					String sText = getResources().getString(R.string.user_connect_server);
					startProgress(sTitle, sText);
				}
			}
		});
	}
	
	@Override
	protected void onResume() {
		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		super.onResume();
    }

	@Override
	protected void onDestroy() {
		Log.d(TAG, this.getClass().toString() + " onDestroy unbindService");
		unbindService(mSrvCon); //must before onDestroy
		super.onDestroy();
	}
	
	class LoginHandler extends Handler {
		public LoginHandler() {
		}

		public LoginHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			Log.d(TAG, this.getClass().toString() + " handleMessage thrdID:" + Thread.currentThread().getId() + " msg:" + msg.toString());
			int what = msg.what;
			switch (what) {
			case Protocol.USR_REGISTER_RES:
				stopProgress();
				userRegisterResponse(msg);
				break;
			case Protocol.USR_LOGON_RES:
				stopProgress();
				userLogonResponse(msg);
				break;
			default:
				break;
			}
		}
	}
	
	private void sendServiceMsg(Message msg) {
		if(mServiceHandler != null) {
			mServiceHandler.sendMessage(msg);
		}
	}
	
	private void userRegisterResponse(Message msg) {
		int res = msg.arg1;
		int userRegisterRes = R.string.user_name_invalid;
		switch (res) {
		case USR_REGISTER_SUCCESS:
			userRegisterRes = R.string.user_register_success;
			break;
		case USR_NAME_TOOLONG:
			userRegisterRes = R.string.user_name_toolong;
			break;
		case USR_NAME_INVALID:
			userRegisterRes = R.string.user_name_invalid;
			break;
		case USR_NAME_EXIST:
			userRegisterRes = R.string.user_name_exist;
			break;
		case USR_PWD_TOOLONG:
			userRegisterRes = R.string.user_pwd_toolong;
			break;
		default:
			break;
		}
		Toast.makeText(LoginActivity.this, userRegisterRes, Toast.LENGTH_SHORT).show();
	}
	
	private void userLogonResponse(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userLogonResponse()");
		int res = msg.arg1;
		if (res == Protocol.USR_LOGON_SUCCESS) {
			Message svcMsg = Message.obtain();
			svcMsg.what = Protocol.USR_SET_REQ;
			Bundle b = new Bundle();
			b.putString("name", mUserName);
			b.putString("pwd", mUserPwd);
			b.putInt("status", Protocol.USR_STATUS_ONLINE);
			svcMsg.setData(b);
			sendServiceMsg(svcMsg);
			
			if (cbRempwd.isChecked()) {
				saveUser();
			}
			finish();
		} else {
			clearUser();
			int userLogonRes = R.string.user_logon_error;
			Toast.makeText(LoginActivity.this, userLogonRes, Toast.LENGTH_SHORT).show();
		}
	}
	
	private boolean adjustName(String name) {
		boolean b = false;
		byte aName[] = name.getBytes();
		int len = aName.length;
		if ((0 < len) && (len < USR_NAME_SIZE)) {
			b = true;
			for (int i=0; i<len; i++) {
				if (0x30 <= aName[i] && aName[i] <=0x39) {
				} else if (0x41 <= aName[i] && aName[i] <= 0x5A) {
				} else if (0x61 <= aName[i] && aName[i] <= 0x7A) {
				} else {
					b = false;
					break;
				}
			}
		}
		return b;
	}

	private boolean adjustPwd(String pwd) {
		boolean b = false;
		byte aPwd[] = pwd.getBytes();
		int len = aPwd.length;
		if ((0 < len) && (len < USR_PWD_SIZE)) {
			b = true;
		}
		return b;
	}
	
	class ProgressThread extends Thread {
		private boolean mExit = false;
		private ProgressDialog mProgressDlg;
		@Override
		public void run() {
			Log.d(TAG, this.getClass().toString() + " ProgressDialog.show");
			for(int i=0; i<9 && !mExit; i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mProgressDlg.dismiss();
		}
		
		public void showProgress(String title, String text) {
			mProgressDlg = ProgressDialog.show(LoginActivity.this, title, text);
		}
		
		public void quit() {
			mExit = true;
		}
	}
	
	public void startProgress(String title, String text) {
		Log.d(TAG, this.getClass().toString() + " startProgress");
		mProgressThread = new ProgressThread();
		mProgressThread.showProgress(title, text);
		mProgressThread.start();
	}
	
	public void stopProgress() {
		Log.d(TAG, this.getClass().toString() + " stopProgress");
		if (mProgressThread != null) {
			mProgressThread.quit();
			try { 
				mProgressThread.join();
			} catch (InterruptedException e) { 
				throw new RuntimeException(e); 
			}
			mProgressThread = null;
		}
	}
	
	private void saveUser() {
		SharedPreferences sp = getSharedPreferences("user", Context.MODE_PRIVATE);
		Editor et = sp.edit();
		et.putBoolean("rempwd", true);
		et.putString("name", mUserName);
		et.putString("pwd", mUserPwd);
		et.commit();
	}
	
	private void clearUser() {
		SharedPreferences sp = getSharedPreferences("user", Context.MODE_PRIVATE);
		Editor et = sp.edit();
		et.clear();
		et.commit();
	}
	
	private void loadUser() {
		SharedPreferences sp = getSharedPreferences("user", Context.MODE_PRIVATE);
		mIsRempwd = sp.getBoolean("rempwd", false);
		mUserName = sp.getString("name", "");
		mUserPwd = sp.getString("pwd", "");
	}
}
