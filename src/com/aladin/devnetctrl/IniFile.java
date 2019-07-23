package com.aladin.devnetctrl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
//import android.util.Log;

public class IniFile {
	//private static String TAG = "DevNetCtrl";
	public static String getProfileString(String file, String section, String variable, String defaultValue) throws IOException {
		String strLine, value = "";
		BufferedReader bufferedReader;
		File f = new File(file);
		boolean bExist = f.exists();
		if(bExist == false) return value;
		//Log.d("DiffLight", "IniFile.getProfileString " + section + " " + variable + "=" + value);
		bufferedReader = new BufferedReader(new FileReader(file));
		try {
			while((strLine = bufferedReader.readLine()) != null) {
				strLine = strLine.trim();
				//Log.d(TAG, "IniFile.getProfileString " + strLine);
				String[] strArray = strLine.split("=");
		    	if(strArray.length == 1) {
		    		value = strArray[0].trim();
		    		if(value.equalsIgnoreCase(variable)) {
		    			value = "";
		    			return value;
		    		}
		    	} else if(strArray.length == 2) {
		    		value = strArray[0].trim();
		    		if(value.equalsIgnoreCase(variable)) {
		    			value = strArray[1].trim();
		    			//Log.d(TAG, "IniFile.getProfileString value=" + value);
		    			return value;
		    		}
		    	} else if(strArray.length > 2) {
		    		value = strArray[0].trim();
		    		if(value.equalsIgnoreCase(variable)) {
		    			value = strLine.substring(strLine.indexOf("=") + 1).trim();
		    			return value;
		    		}
		    	}
			}
		} finally {
			bufferedReader.close();
		}
		return defaultValue;
	}

	public static boolean setProfileString(String file, String section, String variable, String value) throws IOException {
		String fileContent, strLine, newLine;
		boolean isInSection = false;
		File f = new File(file);
		boolean bExist = f.exists();
		if(bExist == false) f.createNewFile();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		fileContent = "";
		//Log.d(TAG, "IniFile.setProfileString section:" + section + " " + variable + "=" + value);
		try {
			strLine = bufferedReader.readLine();
			//Log.d(TAG, "IniFile.setProfileString strLine:" + strLine);
			while(strLine != null) {
				strLine = strLine.trim();
				String[] strArray = strLine.split("=");
				String strKey = strArray[0].trim();
				if(strKey.equalsIgnoreCase(variable)) {
					newLine = strKey + "=" + value;
					fileContent += newLine + "\n";
					isInSection = true;
					//Log.d(TAG, "IniFile.setProfileString newLine:" + newLine);
				}
				else {
					fileContent += strLine + "\n";
					//Log.d("DiffLight", "IniFile.setProfileString " + fileContent);
				}
				strLine = bufferedReader.readLine();
			}
			if(isInSection == false) {
				if(fileContent == "") {
					newLine = "[" + section + "]\n" + variable + "=" + value;
				} else {
					newLine = variable + "=" + value;
				}
				fileContent += newLine + "\n";
			}
			//Log.d(TAG, "IniFile.setProfileString fileContent:" + fileContent);
			bufferedReader.close();
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));
			bufferedWriter.write(fileContent);
			bufferedWriter.flush();
			bufferedWriter.close();
		} finally {
			bufferedReader.close();
		}
		return true;
	}
	
}
