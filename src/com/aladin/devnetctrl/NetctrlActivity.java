package com.aladin.devnetctrl;

import com.aladin.devnetctrl.ColorRingView.Listener;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class NetctrlActivity extends Activity {
	private static String TAG = "DevNetCtrl";
	private static final int LIST_ACTIVITY_REQ_CODE = 0;
	private static final int LOGIN_ACTIVITY_REQ_CODE = 1;
	private NetctrlHandler mHandler = null;
	private Handler mServiceHandler = null;
	private NetctrlService.NetctrlBinder mBinder = null;
	private long mExitTime = 0;
	private int mStatus = 0;
	
	private RollTextView tvTitle = null;
	private RollTextView tvSubtitle = null;
	private Button bnLightSelect = null;
	private Button bnLightPoweron = null;
	private Button bnLightPoweroff = null;
	private Button bnLightAuto = null;
	private Button bnLightIncrease = null;
	private Button bnLightDecrease = null;
	private Button bnLightRandom = null;
	private Button bnLightBluetooth = null;
	private Button bnLightMicphone = null;
	private ColorRingView vwLightColor = null;

	private ServiceConnection mSrvCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBinder = (NetctrlService.NetctrlBinder)service;
			mBinder.setForeignHandler(mHandler);
			mServiceHandler = mBinder.getHandler();
			scanDevices();
			userInfoReq();
			Log.d(TAG, this.getClass().toString() + " onServiceConnected componentname:" + name);
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mServiceHandler = null;
			Log.d(TAG, this.getClass().toString() + " onServiceDisconnected");
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, this.getClass().toString() + " onCreate");
		setContentView(R.layout.activity_netctrl);
		
        ActionBar ab = getActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        ab.setCustomView(R.layout.roll_title);
        tvTitle = (RollTextView)findViewById(R.id.roll_title_tv);
        tvSubtitle = (RollTextView)findViewById(R.id.roll_subtitle_tv);
		
		mHandler = new NetctrlHandler();
		Intent intent = new Intent(NetctrlActivity.this, NetctrlService.class);
		bindService(intent, mSrvCon, Context.BIND_AUTO_CREATE);
		
		bnLightSelect = (Button)findViewById(R.id.light_select_bn);
		bnLightSelect.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, this.getClass().toString() + " onClick() bnLightSelect");
				Intent it = new Intent(NetctrlActivity.this, DevListActivity.class);
				startActivityForResult(it, LIST_ACTIVITY_REQ_CODE);
			}
		});
		bnLightPoweron = (Button)findViewById(R.id.light_poweron_bn);
		bnLightPoweron.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Log.d(TAG, this.getClass().toString() + " onClick() bnLightPoweron");
		    	Message msg = Message.obtain();
		    	msg.what = Protocol.LIGHT_ONOFF_REQ;
		    	msg.arg1 = 1;
		    	sendServiceMsg(msg);
			}
		});
		bnLightPoweroff = (Button)findViewById(R.id.light_poweroff_bn);
		bnLightPoweroff.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Log.d(TAG, this.getClass().toString() + " onClick() bnLightPoweroff");
		    	Message msg = Message.obtain();
		    	msg.what = Protocol.LIGHT_ONOFF_REQ;
		    	msg.arg1 = 0;
		    	sendServiceMsg(msg);
			}
		});
		bnLightAuto = (Button)findViewById(R.id.light_auto_bn);
		bnLightAuto.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Log.d(TAG, this.getClass().toString() + " onClick() bnLightAuto");
		    	Message msg = Message.obtain();
		    	msg.what = Protocol.LIGHT_AUTO_REQ;
		    	sendServiceMsg(msg);
			}
		});
		bnLightIncrease = (Button)findViewById(R.id.light_increase_bn);
		bnLightIncrease.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Log.d(TAG, this.getClass().toString() + " onClick() bnLightIncrease");
		    	Message msg = Message.obtain();
		    	msg.what = Protocol.LIGHT_INCREASE_REQ;
		    	sendServiceMsg(msg);
			}
		});
		bnLightDecrease = (Button)findViewById(R.id.light_decrease_bn);
		bnLightDecrease.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Log.d(TAG, this.getClass().toString() + " onClick() bnLightDecrease");
		    	Message msg = Message.obtain();
		    	msg.what = Protocol.LIGHT_DECREASE_REQ;
		    	sendServiceMsg(msg);
			}
		});
		bnLightRandom = (Button)findViewById(R.id.light_random_bn);
		bnLightRandom.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Log.d(TAG, this.getClass().toString() + " onClick() bnLightRandom");
		    	Message msg = Message.obtain();
		    	msg.what = Protocol.LIGHT_RANDOM_REQ;
		    	sendServiceMsg(msg);
			}
		});
		bnLightBluetooth = (Button)findViewById(R.id.light_bluetooth_bn);
		bnLightBluetooth.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Log.d(TAG, this.getClass().toString() + " onClick() bnLightBluetooth");
		    	Message msg = Message.obtain();
		    	msg.what = Protocol.LIGHT_BLUETOOTH_REQ;
		    	sendServiceMsg(msg);
		    	lightConfigBluetooth();
			}
		});
		bnLightMicphone = (Button)findViewById(R.id.light_micphone_bn);
		bnLightMicphone.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Log.d(TAG, this.getClass().toString() + " onClick() bnLightMicphone");
		    	Message msg = Message.obtain();
		    	msg.what = Protocol.LIGHT_MICPHONE_REQ;
		    	sendServiceMsg(msg);
			}
		});
		vwLightColor = (ColorRingView)findViewById(R.id.light_colorring_vw);
		vwLightColor.registerListener(new Listener() {
		    public void onRgb(int color) {
		    	Log.d(TAG, this.getClass().toString() + " onRgb() color=" + color);
		    	Message msg = Message.obtain();
		    	msg.what = Protocol.LIGHT_COLOR_REQ;
		    	msg.arg1 = color;
		    	sendServiceMsg(msg);
		    }
		});
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, this.getClass().toString() + " onResume()");
		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		super.onResume();
    }

	@Override
	protected void onDestroy() {
		Log.d(TAG, this.getClass().toString() + " onDestroy unbindService");
		if (mBinder != null) {
			mBinder.setForeignHandler(null);
		}
		unbindService(mSrvCon); //must before onDestroy
		super.onDestroy();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if((keyCode == KeyEvent.KEYCODE_BACK) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
			if ((System.currentTimeMillis() - mExitTime) > 2000) {
				Toast.makeText(this, R.string.user_press_exit, Toast.LENGTH_SHORT).show();
				mExitTime = System.currentTimeMillis();
			} else {
				userLogoffReq();
				finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.netctrl_options_menu, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.d(TAG, this.getClass().toString() + " onPrepareOptionsMenu()");
		MenuItem itemReg = menu.findItem(R.id.user_menu_register);
		MenuItem itemLogon = menu.findItem(R.id.user_menu_logon);
		MenuItem itemLogoff = menu.findItem(R.id.user_menu_logoff);
		MenuItem itemUnreg = menu.findItem(R.id.user_menu_unregister);
		if (mStatus == Protocol.USR_STATUS_ONLINE) {
			itemReg.setVisible(false);
			itemLogon.setVisible(false);
			itemLogoff.setVisible(true);
			itemUnreg.setVisible(true);
		} else {
			itemReg.setVisible(true);
			itemLogon.setVisible(true);
			itemLogoff.setVisible(false);
			itemUnreg.setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch(item.getItemId())
		{
		case R.id.user_menu_register:
		case R.id.user_menu_logon:
			userRegLogon();
			break;
		case R.id.user_menu_logoff:
			userLogoffReq();
			break;
		case R.id.user_menu_unregister:
			userUnregister();
			break;
		}
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mBinder.setForeignHandler(mHandler);
		Log.d(TAG, this.getClass().toString() + " onActivityResult requestCode:" + requestCode);
		switch(requestCode) {
			case LIST_ACTIVITY_REQ_CODE:
				devChkListReq();
				break;
			case LOGIN_ACTIVITY_REQ_CODE:
				userInfoReq();
				break;
			default:
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void userUnregister() {
		Log.d(TAG, this.getClass().toString() + " userUnregister() status:" + mStatus);
		if (mStatus == Protocol.USR_STATUS_ONLINE) {
			final AlertDialog confirmDlg = new AlertDialog.Builder(this).create();
			confirmDlg.show();
			Window w = confirmDlg.getWindow();
			w.setContentView(R.layout.confirm_dialog);
			final TextView tvContent = (TextView) w.findViewById(R.id.content_tv);
			tvContent.setText(getString(R.string.user_unregister_confirm));
			Button bnCancel = (Button) w.findViewById(R.id.cancel_bn);
			bnCancel.setOnClickListener(new OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
	    			confirmDlg.dismiss();
	    		}
	    	});
			Button bnOk = (Button) w.findViewById(R.id.ok_bn);
			bnOk.setOnClickListener(new OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
	    			confirmDlg.dismiss();
	    			userUnregisterReq();
	    		}
	    	});
		} else {
			int userUnregError = R.string.user_unregister_error;
			Toast.makeText(NetctrlActivity.this, userUnregError, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void lightConfigBluetooth() {
		Log.d(TAG, this.getClass().toString() + " lightConfigBluetooth()");
		final AlertDialog confirmDlg = new AlertDialog.Builder(this).create();
		confirmDlg.show();
		Window w = confirmDlg.getWindow();
		w.setContentView(R.layout.confirm_dialog);
		final TextView tvContent = (TextView) w.findViewById(R.id.content_tv);
		tvContent.setText(getString(R.string.light_bluetooth_useornot));
		Button bnCancel = (Button) w.findViewById(R.id.cancel_bn);
		bnCancel.setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			confirmDlg.dismiss();
    		}
    	});
		Button bnOk = (Button) w.findViewById(R.id.ok_bn);
		bnOk.setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			confirmDlg.dismiss();
    			startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
    		}
    	});
	}
	
	private void devChkListReq() {
		Message msg = Message.obtain();
		msg.what = Protocol.USR_DEV_CHKLIST_REQ;
		sendServiceMsg(msg);
	}
	
	private void devChkListRes(Message msg) {
		Bundle b = msg.getData();
		int status = b.getInt("status");
		String sName = b.getString("name");
		String sDevsName = b.getString("devsname");
		Log.d(TAG, this.getClass().toString() + " devChkListRes() name:" + sName + " devsname:" + sDevsName);
		String sTitle = getString(R.string.user_not_logon);
		if (status == Protocol.USR_STATUS_ONLINE) {
			sTitle = getString(R.string.user_name) + ": " + sName;
		}
		tvTitle.setText(sTitle);
		
		String sSubtitle = getString(R.string.dev_unselect);
		if (sDevsName != "") {
			sSubtitle = getString(R.string.dev_select) + ":" + sDevsName;
		}
		tvSubtitle.setText(sSubtitle);
	}
	
	private void userInfoReq() {
		Log.d(TAG, this.getClass().toString() + " userInfoReq()");
		Message msg = Message.obtain();
		msg.what = Protocol.USR_INFO_REQ;
		sendServiceMsg(msg);
	}
	
	private void userInfoRes(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userInfoRes()");
		Bundle b = msg.getData();
		mStatus = b.getInt("status");
		
		devChkListReq();
		if (mStatus == Protocol.USR_STATUS_ONLINE) {
			devRegistedListReq();
		}
	}
	
	private void scanDevices() {
		Message msg = Message.obtain();
		msg.what = Protocol.LIGHT_LANSCAN_REQ;
		sendServiceMsg(msg);
	}
	
	private void devRegistedListReq() {
		Log.d(TAG, this.getClass().toString() + " devRegistedListReq()");
		Message msg = Message.obtain();
		msg.what = Protocol.USR_DEV_REGLIST_REQ;
		sendServiceMsg(msg);
	}
	
	private void userUnregisterReq() {
		Log.d(TAG, this.getClass().toString() + " userUnregisterReq()");
		Message msg = Message.obtain();
		msg.what = Protocol.USR_UNREGISTER_REQ;
		sendServiceMsg(msg);
	}
	
	private void userUnregisterRes(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userUnregisterRes()");
		userInfoReq();
	}
	
	private void userRegLogon() {
		Log.d(TAG, this.getClass().toString() + " userRegLogon()");
		Intent it = new Intent(NetctrlActivity.this, LoginActivity.class);
		startActivityForResult(it, LOGIN_ACTIVITY_REQ_CODE);
	}
	
	private void userLogoffReq() {
		Log.d(TAG, this.getClass().toString() + " userLogoffReq()");
		Message msg = Message.obtain();
		msg.what = Protocol.USR_LOGOFF_REQ;
		sendServiceMsg(msg);
	}
	
	private void userLogoffRes(Message msg) {
		Log.d(TAG, this.getClass().toString() + " userLogoffRes()");
		userInfoReq();
	}

	class NetctrlHandler extends Handler {
		public NetctrlHandler() {
		}

		public NetctrlHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			Log.d(TAG, this.getClass().toString() + " handleMessage thrdID:" + Thread.currentThread().getId() + " msg:" + msg.toString());
			int what = msg.what;
			switch (what) {
			case Protocol.USR_DEV_CHKLIST_RES:
				devChkListRes(msg);
				break;
			case Protocol.USR_UNREGISTER_RES:
				userUnregisterRes(msg);
				break;
			case Protocol.USR_LOGOFF_RES:
				userLogoffRes(msg);
				break;
			case Protocol.USR_INFO_RES:
				userInfoRes(msg);
				break;
			default:
				break;
			}
		}
	}
	
	public void sendServiceMsg(Message msg) {
		if(mServiceHandler != null) {
			mServiceHandler.sendMessage(msg);
		}
	}
}
