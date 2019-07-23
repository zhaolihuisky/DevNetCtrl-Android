package com.aladin.devnetctrl;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class DeviceMgr {
	private List<Device> mDevs;
	private List<Device> mDelList;
	private String mDevsDir = "";
	private int mCheckedCount = 0;
	private DevComparator mComparator;
	
	public DeviceMgr() {
		mDevs = new ArrayList<Device>();
		mDelList = new ArrayList<Device>();
		mDevsDir = "";
		mCheckedCount = 0;
		mComparator = new DevComparator();
	}
	
	public List<Device> getDevices() {
		return mDevs;
	}
	
	public int getDevsCount() {
		return mDevs.size();
	}
	
	public int getDevsCheckCount() {
		return mCheckedCount;
	}
	
	private void setDevsCheckCount(boolean check) {
		if (check) {
			mCheckedCount++;
		} else {
			mCheckedCount--;
		}
	}
	
	public void setDevsDir(String sDir) {
		mDevsDir = sDir;
		createDevsDir();
	}
	
	public void createDevsDir() {
		if(mDevsDir != "") {
			File file = new File(mDevsDir);
			if (!file.exists()) {
				file.mkdir();
			}
		}
	}
	/*
	public FileFilter fileFilter = new FileFilter() {
		@SuppressLint("DefaultLocale")
		public boolean accept(File file) {
			String tmp = file.getName().toLowerCase();
			if (tmp.endsWith(".ini") || tmp.endsWith(".jpg")) {
				return true;
			}
			return false;
		}
	};
	*/
	public void testLargeDevs() {
		int size = 1000;
		Random random = new Random(System.currentTimeMillis());
		for (int i=0; i<size; i++) {
			String mac = String.format("%012X", random.nextInt());
			Device newDev = new Device();
			newDev.setMac(mac);
			newDev.setName(mac);
			String sPath = mDevsDir + "/" + mac + ".ini";
			newDev.setPath(sPath);
			addDevice(newDev);
		}
		Collections.sort(mDevs, mComparator);
	}

	public static FilenameFilter getFileExtensionFilter(String extension) {
		final String ext = extension;
		return new FilenameFilter() {
			public boolean accept(File file, String name) {
				boolean ret = name.endsWith(ext);
				return ret;
			}
		};
	}
	
	public class DevComparator implements Comparator<Device> {
		public int compare(Device d1, Device d2) {
			return d1.getName().compareToIgnoreCase(d2.getName());
		}
	}
	
	public void loadDevices() {
		File dir = new File(mDevsDir);
		File files[] = dir.listFiles(getFileExtensionFilter(".ini"));
		if (files != null) {
			int size = files.length;
			for(int i=0; i<size; i++) {
				File f = files[i];
				String sPath = f.getPath();
				Device dev = new Device();
				dev.setPath(sPath);
				boolean bRet = dev.load();
				if (bRet) {
					mDevs.add(dev);
					if (dev.getCheck()) {
						setDevsCheckCount(true);
					}
				}
			}
			//sort device by name
			Collections.sort(mDevs, mComparator);
		}
	}
	
	public void delAllDevices() {
		int size = mDevs.size();
		for (int i=0; i<size; i++) {
			Device d = mDevs.get(i);
			d.delete();
		}
		mDevs.removeAll(mDevs);
	}
	
	public void setAllDevsUnregisted() {
		int size = mDevs.size();
		for (int i=0; i<size; i++) {
			Device d = mDevs.get(i);
			if (d.getRegisted() == true) {
				d.setRegisted(false);
			}
		}
	}
	
	public void addDevice(Device dev) {
		dev.save();
		mDevs.add(dev);
		Collections.sort(mDevs, mComparator);
	}
	
	public void delDevice(int index) {
		int size = mDevs.size();
		if ((0<=index) && (index<size)) {
			Device d = mDevs.get(index);
			d.delete();
			if (d.getCheck()) {
				setDevsCheckCount(false);
			}
			mDevs.remove(index);
		}
	}
	
	public Device getDevice(String mac) {
		int size = mDevs.size();
		Device dev = null;
		for (int i=0; i<size; i++) {
			Device d = mDevs.get(i);
			if (d.getMac().equals(mac)) {
				dev = d;
				break;
			}
		}
		return dev;
	}
	
	public Device getDevice(int index) {
		Device dev = null;
		if ((0<=index) && (index<mDevs.size())) {
			dev = mDevs.get(index);
		}
		return dev;
	}
	
	public void setDevCheck(int index, boolean check) {
		Device d = getDevice(index);
		if (d != null) {
			d.setCheck(check);
			d.saveCheck();
			setDevsCheckCount(check);
		}
	}
	
	public void setDevName(int index, String name) {
		Device d = getDevice(index);
		if (d != null) {
			d.setName(name);
			d.saveName();
			Collections.sort(mDevs, mComparator);
		}
	}
	
	public void setDevRegisted(String mac, boolean registed) {
		Device dev = getDevice(mac);
		if (dev != null) {
			dev.setRegisted(registed);
		}
	}
	
	public String getCheckDevsIndex() {
		String sIndex = "";
		int size = mDevs.size();
		for (int i=0; i<size; i++) {
			Device d = mDevs.get(i);
			if (d.getCheck()) {
				sIndex += i + ",";
			}
		}
		return sIndex;
	}
	
	public String getCheckDevsName() {
		String sName = "";
		String sDevsIndex = getCheckDevsIndex();
		String aIndex[] = sDevsIndex.split("\\,");
		for (int i=0; i<aIndex.length; i++) {
			if (aIndex[i].equals("")) {
				continue;
			}
			int index = Integer.parseInt(aIndex[i], 10);
			Device dev = getDevice(index);
			if (dev != null) {
				sName += dev.getName() + " ";
			}
		}
		return sName;
	}
	
	public void checkAllDevs(boolean check) {
		int size = mDevs.size();
		for (int i=0; i<size; i++) {
			Device d = mDevs.get(i);
			boolean b = d.getCheck();
			if (b != check) {
				d.setCheck(check);
				d.saveCheck();
				setDevsCheckCount(check);
			}
		}
	}
	
	public void checkDelDevs() {
		int size = mDevs.size();
		for (int i=0; i<size; i++) {
			Device d = mDevs.get(i);
			if (d.getCheck()) {
				d.delete();
				mDelList.add(d);
				setDevsCheckCount(false);
			}
		}
		mDevs.removeAll(mDelList);
		mDelList.clear();
	}
}
