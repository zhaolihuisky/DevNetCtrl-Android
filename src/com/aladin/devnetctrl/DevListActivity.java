package com.aladin.devnetctrl;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class DevListActivity extends ListActivity {
	private static String TAG = "DevNetCtrl";
	private DevListHandler mHandler = null;
	private Handler mServiceHandler = null;
	private NetctrlService.NetctrlBinder mBinder = null;
	private int mStatus = 0;
	private DevListAdapter mAdapter = null;
	private int index = -1;

	private ServiceConnection mSrvCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBinder = (NetctrlService.NetctrlBinder)service;
			mBinder.setForeignHandler(mHandler);
			mServiceHandler = mBinder.getHandler();
			mAdapter = new DevListAdapter(DevListActivity.this, mBinder.getDevices());
			mAdapter.setServiceHandler(mServiceHandler);
			getListView().setAdapter(mAdapter);
			scanDevices();
			userInfoReq();
			userDevCountReq();
			Log.d(TAG, this.getClass().toString() + " onServiceConnected componentname:" + name);
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mServiceHandler = null;
			mAdapter.setServiceHandler(null);
			Log.d(TAG, this.getClass().toString() + " onServiceDisconnected ");
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView lvList = getListView();
		lvList.setBackgroundResource(R.drawable.background);
		registerForContextMenu(lvList);
		lvList.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				index = position;
				Log.d(TAG, this.getClass().toString() + " onItemLongClick:" + index);
				return false;
			}
		});
		
		mHandler = new DevListHandler();
		Intent intent = new Intent(this, NetctrlService.class);
		bindService(intent, mSrvCon, Context.BIND_AUTO_CREATE);
		Log.d(TAG, this.getClass().toString() + " onCreate()");
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.devlist_options_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch(item.getItemId())
		{
		case R.id.dev_scan:
			scanDevices();
			break;
		case R.id.dev_add:
			addDevice();
			break;
		case R.id.dev_chkall:
			checkAllDevs();
			break;
		case R.id.dev_unchkall:
			uncheckAllDevs();
			break;
		case R.id.dev_chkdel:
			checkDelDevs();
			break;
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		//Log.d(TAG, this.getClass().toString() + " onCreateContextMenu v:" + v.toString());
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.devlist_context_menu, menu);
		MenuItem itemDevReg = menu.findItem(R.id.dev_reg);
		MenuItem itemDevUnreg = menu.findItem(R.id.dev_unreg);
		if (mStatus == Protocol.USR_STATUS_ONLINE) {
			itemDevReg.setVisible(true);
			itemDevUnreg.setVisible(true);
		} else {
			itemDevReg.setVisible(false);
			itemDevUnreg.setVisible(false);
		}
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.dev_edt:
			if (index > -1) {
				edtDevice(index);
			}
			break;
		case R.id.dev_del:
			if (index > -1) {
				//unregDevice(index);
				delDevice(index);
			}
			break;
		case R.id.dev_reg:
			if (index > -1) {
				regDevice(index);
			}
			break;
		case R.id.dev_unreg:
			if (index > -1) {
				unregDevice(index);
			}
			break;
		default:  
			break;  
		}
		index = -1;
		return true;  
	}
	
	private void scanDevices() {
		Message msg = Message.obtain();
		msg.what = Protocol.LIGHT_LANSCAN_REQ;
		sendServiceMsg(msg);
	}
	
	private void addDevice() {
		Log.d(TAG, this.getClass().toString() + " addDevice");
		final Dialog devDlg = new Dialog(this);
		devDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
		devDlg.show();
		Window window = devDlg.getWindow();
		window.setBackgroundDrawable(new ColorDrawable(0));
		window.setContentView(R.layout.add_dev_dialog);
		final EditText etMac = (EditText) window.findViewById(R.id.mac_et);
		final EditText etName = (EditText) window.findViewById(R.id.name_et);
		final Button bnCancel = (Button) window.findViewById(R.id.cancel_bn);
		bnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				devDlg.dismiss();
			}
    	});
		final Button bnOk = (Button) window.findViewById(R.id.ok_bn);
		bnOk.setOnClickListener(new OnClickListener() {
			@SuppressLint("DefaultLocale")
			@Override
			public void onClick(View v) {
				Log.d(TAG, this.getClass().toString() + "addDevice onClick OK");
				String sMac = etMac.getText().toString();
				sMac = sMac.replace("-", "");
				sMac = sMac.replace(":", "");
				sMac = sMac.toUpperCase();
				String sName = etName.getText().toString();
				if (sName.equals("")) {
					sName = sMac;
				}
				boolean valid = Utility.isMacValid(sMac);
				if (valid) {
					Device dev = mBinder.getDevice(sMac);
					if (dev == null) {
						Message msg = Message.obtain();
						msg.what = Protocol.USR_DEV_ADD_REQ;
						Bundle b = new Bundle();
						b.putString("mac", sMac);
						b.putString("name", sName);
						msg.setData(b);
						sendServiceMsg(msg);
						devDlg.dismiss();
					} else {
						Toast.makeText(DevListActivity.this, R.string.dev_mac_exist, Toast.LENGTH_SHORT).show();
					}
				} else {
					Toast.makeText(DevListActivity.this, R.string.dev_mac_invalid, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	private void edtDevice(final int index) {
		Log.d(TAG, this.getClass().toString() + " edtDevice");
		final Dialog devDlg = new Dialog(this);
		devDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
		devDlg.show();
		Window window = devDlg.getWindow();
		window.setBackgroundDrawable(new ColorDrawable(0));
		window.setContentView(R.layout.add_dev_dialog);
		final EditText etMac = (EditText) window.findViewById(R.id.mac_et);
		etMac.setEnabled(false);
		final EditText etName = (EditText) window.findViewById(R.id.name_et);
		final Device dev = mBinder.getDevice(index);
		etMac.setText(dev.getMac());
		etName.setText(dev.getName());
		final Button bnCancel = (Button) window.findViewById(R.id.cancel_bn);
		bnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				devDlg.dismiss();
			}
    	});
		final Button bnOk = (Button) window.findViewById(R.id.ok_bn);
		bnOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, this.getClass().toString() + "edtDevice onClick OK");
				String sName = etName.getText().toString();
				if (sName.equals("")) {
					Toast.makeText(DevListActivity.this, R.string.dev_name_notnull, Toast.LENGTH_SHORT).show();
				} else {
					Message msg = Message.obtain();
					msg.what = Protocol.USR_DEV_EDT_REQ;
					Bundle b = new Bundle();
					b.putInt("index", index);
					b.putString("name", sName);
					msg.setData(b);
					sendServiceMsg(msg);
					devDlg.dismiss();
				}
			}
		});
	}
	
	private void checkAllDevs() {
		//Log.d(TAG, this.getClass().toString() + " checkAllDevs()");
		Message msg = Message.obtain();
		msg.what = Protocol.USR_DEV_CHKALL_REQ;
		sendServiceMsg(msg);
	}
	
	private void uncheckAllDevs() {
		//Log.d(TAG, this.getClass().toString() + " uncheckAllDevs()");
		Message msg = Message.obtain();
		msg.what = Protocol.USR_DEV_UNCHKALL_REQ;
		sendServiceMsg(msg);
	}
	
	private void checkDelDevs() {
		//Log.d(TAG, this.getClass().toString() + " checkDelDevs()");
		Message msg = Message.obtain();
		msg.what = Protocol.USR_DEV_CHKDEL_REQ;
		sendServiceMsg(msg);
	}
	
	private void userInfoReq() {
		//Log.d(TAG, this.getClass().toString() + " userInfoReq()");
		Message msg = Message.obtain();
		msg.what = Protocol.USR_INFO_REQ;
		sendServiceMsg(msg);
	}
	
	private void userInfoRes(Message msg) {
		//Log.d(TAG, this.getClass().toString() + " userInfoRes()");
		Bundle b = msg.getData();
		mStatus = b.getInt("status");
	}	

	private void delDevice(final int index) {
		Message msg = Message.obtain();
		msg.what = Protocol.USR_DEV_DEL_REQ;
		Bundle b = new Bundle();
		b.putInt("index", index);
		msg.setData(b);
		sendServiceMsg(msg);
	}
	
	private void regDevice(final int index) {
		Message msg = Message.obtain();
		msg.what = Protocol.USR_DEV_REGISTER_REQ;
		Bundle b = new Bundle();
		b.putInt("index", index);
		msg.setData(b);
		sendServiceMsg(msg);
	}
	
	private void unregDevice(final int index) {
		Message msg = Message.obtain();
		msg.what = Protocol.USR_DEV_UNREGISTER_REQ;
		Bundle b = new Bundle();
		b.putInt("index", index);
		msg.setData(b);
		sendServiceMsg(msg);
	}
	
	private void userDevCountReq() {
		//Log.d(TAG, this.getClass().toString() + " userDevCountReq()");
		Message msg = Message.obtain();
		msg.what = Protocol.USR_DEV_COUNT_REQ;
		sendServiceMsg(msg);
	}
	
	private void userDevCountRes(Message msg) {
		//Log.d(TAG, this.getClass().toString() + " userDevCountRes()");
		int count = msg.arg1;
		int chkcnt = msg.arg2;
		String title = getString(R.string.dev_list) + "  " + chkcnt + "/" + count;
		setTitle(title);
	}
	
	class DevListHandler extends Handler {
		public DevListHandler() {
		}

		public DevListHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			Log.d(TAG, this.getClass().toString() + " handleMessage thrdID:" + Thread.currentThread().getId() + " msg:" + msg.toString());
			int what = msg.what;
			switch (what) {
			case Protocol.USR_INFO_RES:
				userInfoRes(msg);
				break;
			case Protocol.USR_DEV_EDT_RES:
			case Protocol.USR_DEV_REGISTER_RES:
			case Protocol.USR_DEV_UNREGISTER_RES:
			case Protocol.USR_DEV_IPPORT_RES:
				if(mAdapter != null) {
					mAdapter.notifyDataSetChanged();
				}
				break;
			case Protocol.LIGHT_LANSCAN_RES:
			case Protocol.USR_DEV_ADD_RES:
			case Protocol.USR_DEV_DEL_RES:
			case Protocol.USR_DEV_CHK_RES:
			case Protocol.USR_DEV_CHKALL_RES:
			case Protocol.USR_DEV_UNCHKALL_RES:
			case Protocol.USR_DEV_CHKDEL_RES:
				if(mAdapter != null) {
					mAdapter.notifyDataSetChanged();
				}
				userDevCountReq();
				break;
			case Protocol.USR_DEV_COUNT_RES:
				userDevCountRes(msg);
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
}
