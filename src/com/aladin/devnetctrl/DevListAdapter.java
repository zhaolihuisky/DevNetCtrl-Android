package com.aladin.devnetctrl;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class DevListAdapter extends BaseAdapter {
	private static String TAG = "DevNetCtrl";
    private LayoutInflater mInflater;
    private List<Device> mDevs;
    private Handler mServiceHandler;
    
    public DevListAdapter() {
    }
    
    public DevListAdapter(Context context, List<Device> devices) {
    	Log.d(TAG, this.getClass().toString() + " DevListAdapter()");
    	mInflater = LayoutInflater.from(context);
    	mDevs = devices;
    }
    
    public void setServiceHandler(Handler handler) {
    	mServiceHandler = handler;
    }
    
    public void sendServiceMsg(Message msg) {
    	if (mServiceHandler != null) {
    		mServiceHandler.sendMessage(msg);
    	}
    }
    
	@Override
	public int getCount() {
		int size = mDevs.size();
		//Log.d(TAG, this.getClass().toString() + " getCount() size:" + size);
		return size;
	}

	@Override
	public Object getItem(int position) {
		Log.d(TAG, this.getClass().toString() + " getItem() position:" + position);
		return mDevs.get(position);
	}

	@Override
	public long getItemId(int position) {
		Log.d(TAG, this.getClass().toString() + " getItemId() position:" + position);
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		//Log.d(TAG, this.getClass().toString() + " getView() position:" + position);
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.dev_list_item, null);
			holder.tvName = (TextView) convertView.findViewById(R.id.device_name_tv);
			holder.tvMacip = (TextView) convertView.findViewById(R.id.device_macip_tv);
			holder.cbBox = (CheckBox) convertView.findViewById(R.id.device_check_cb);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		Device dev = mDevs.get(position);
		String sName = dev.getName();
		String sIpPort = dev.getLanIP() + ":" + dev.getLanPort();
		String sRegisted = "";//convertView.getResources().getString(R.string.dev_unregisted);
		if(dev.getRegisted()) {
			sRegisted = convertView.getResources().getString(R.string.dev_registed);
			sIpPort = dev.getWanIP() + ":" + dev.getWanPort();
		}
		holder.tvName.setText(sName);
		holder.tvMacip.setText(dev.getMac() + "  " + sIpPort + " " + sRegisted);
		holder.cbBox.setChecked(dev.getCheck());
		holder.cbBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, this.getClass().toString() + " CheckBox.setOnClickListener onClick position:" + position);
				Message msg = Message.obtain();
				msg.what = Protocol.USR_DEV_CHK_REQ;
				Bundle b = new Bundle();
				b.putInt("index", position);
				b.putBoolean("check", ((CheckBox)v).isChecked());
				msg.setData(b);
				sendServiceMsg(msg);
			}
		});
		
		return convertView;
	}
	
    public final class ViewHolder {
    	public TextView tvName;
    	public CheckBox cbBox;
    	public TextView tvMacip;
    } 
}
