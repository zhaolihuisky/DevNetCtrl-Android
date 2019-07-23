package com.aladin.devnetctrl;

public class User {
	private String mName = "";
	private String mPwd = "";
	private int mStatus = 0;
	
	public User() {
		mName = "";
		mPwd = "";
		mStatus = 0;
	}
	
	public User(String name, String pwd) {
		mName = name;
		mPwd = pwd;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}
	
	public void setPwd(String pwd) {
		mPwd = pwd;
	}
	
	public String getPwd() {
		return mPwd;
	}
	
	public void setStatus(int status) {
		mStatus = status;
	}
	
	public int getStatus() {
		return mStatus;
	}
	
	public void clear() {
		mName = "";
		mPwd = "";
		mStatus = 0;
	}
}
