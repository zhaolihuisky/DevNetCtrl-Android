package com.aladin.devnetctrl;

public class Protocol {
	//service defined function code
	public static final byte CODE_LANSCAN = 0x00;
	public static final byte CODE_LIGHT = 0x01;
	public static final byte CODE_BLUETOOTH = 0x02;
	public static final byte CODE_MICPHONE = 0x03;
	public static final byte CODE_ONOFF = 0x04;
	public static final byte CODE_NETWORK_CTRL = 0x7F;
	
	public static final String SERVER_HOST = "aladinrgb.vicp.cc";
	public static final int SERVER_PORT = 10086;
	public static final int BROADCAST_PORT = 10010;
	
	public static final int USR_STATUS_OFFLINE = 0x00;
	public static final int USR_STATUS_ONLINE = 0x01;
	public static final int USR_LOGON_SUCCESS = 0x00;
	public static final int USR_LOGOFF_SUCCESS = 0x00;
	public static final int USR_UNREGISTER_SUCCESS = 0x00;
	public static final int USR_DEV_REGISTER_SUCCESS = 0x00;
	public static final int USR_DEV_UNREGISTER_SUCCESS = 0x00;
	public static final int USR_DEV_REGLIST_SUCCESS = 0x00;

	//service defined offset
	public static final int CMD_TRANSLATE = 0x0000;
	public static final int USR_REGISTER = 0x0001;
	public static final int USR_UNREGISTER = 0x0002;
	public static final int USR_LOGON = 0x0003;
	public static final int USR_LOGOFF = 0x0004;
	public static final int USR_DEV_REGISTER = 0x0005;
	public static final int USR_DEV_UNREGISTER = 0x0006;
	public static final int DEV_CONNECT = 0x0007;
	public static final int DEV_DISCONNECT = 0x0008;
	public static final int USR_CON_DEV = 0x0009;
	public static final int DEV_CON_USR = 0x000A;
	public static final int USR_DEV_REGLIST = 0x000B;
	
	//oneself defined
	public static final int DATA_RECEIVE = 0x0030;
	public static final int USR_SET_REQ = 0x0031;
	public static final int USR_SET_RES = 0x0032;
	public static final int USR_INFO_REQ = 0x0033;
	public static final int USR_INFO_RES = 0x0034;
	public static final int USR_REGISTER_REQ = 0x0035;
	public static final int USR_REGISTER_RES = 0x0036;
	public static final int USR_UNREGISTER_REQ = 0x0037;
	public static final int USR_UNREGISTER_RES = 0x0038;
	public static final int USR_LOGON_REQ = 0x0039;
	public static final int USR_LOGON_RES = 0x003A;
	public static final int USR_LOGOFF_REQ = 0x003B;
	public static final int USR_LOGOFF_RES = 0x003C;
	public static final int USR_HEART_BEAT_REQ = 0x003D;
	public static final int USR_HEART_BEAT_RES = 0x003E;
	
	public static final int USR_DEV_REGISTER_REQ = 0x0040;
	public static final int USR_DEV_REGISTER_RES = 0x0041;
	public static final int USR_DEV_UNREGISTER_REQ = 0x0042;
	public static final int USR_DEV_UNREGISTER_RES = 0x0043;
	public static final int USR_DEV_REGLIST_REQ = 0x0044;
	public static final int USR_DEV_REGLIST_RES = 0x0045;
	
	public static final int LIGHT_ONOFF_REQ = 0x0050;
	public static final int LIGHT_ONOFF_RES = 0x0051;
	public static final int LIGHT_AUTO_REQ = 0x0052;
	public static final int LIGHT_AUTO_RES = 0x0053;
	public static final int LIGHT_INCREASE_REQ = 0x0054;
	public static final int LIGHT_INCREASE_RES = 0x0055;
	public static final int LIGHT_DECREASE_REQ = 0x0056;
	public static final int LIGHT_DECREASE_RES = 0x0057;
	public static final int LIGHT_RANDOM_REQ = 0x0058;
	public static final int LIGHT_RANDOM_RES = 0x0059;
	public static final int LIGHT_BLUETOOTH_REQ = 0x005A;
	public static final int LIGHT_BLUETOOTH_RES = 0x005B;
	public static final int LIGHT_MICPHONE_REQ = 0x005C;
	public static final int LIGHT_MICPHONE_RES = 0x005D;
	public static final int LIGHT_COLOR_REQ = 0x005E;
	public static final int LIGHT_COLOR_RES = 0x005F;
	public static final int LIGHT_LANSCAN_REQ = 0x0060;
	public static final int LIGHT_LANSCAN_RES = 0x0061;
	
	public static final int USR_DEV_ADD_REQ = 0x0072;
	public static final int USR_DEV_ADD_RES = 0x0073;
	public static final int USR_DEV_EDT_REQ = 0x0074;
	public static final int USR_DEV_EDT_RES = 0x0075;
	public static final int USR_DEV_DEL_REQ = 0x0076;
	public static final int USR_DEV_DEL_RES = 0x0077;
	public static final int USR_DEV_CHK_REQ = 0x0078;
	public static final int USR_DEV_CHK_RES = 0x0079;
	public static final int USR_DEV_CHKLIST_REQ = 0x007A;
	public static final int USR_DEV_CHKLIST_RES = 0x007B;
	public static final int USR_DEV_IPPORT_REQ = 0x007C;
	public static final int USR_DEV_IPPORT_RES = 0x007D;
	public static final int USR_DEV_CHKALL_REQ = 0x007E;
	public static final int USR_DEV_CHKALL_RES = 0x007F;
	public static final int USR_DEV_UNCHKALL_REQ = 0x0080;
	public static final int USR_DEV_UNCHKALL_RES = 0x0081;
	public static final int USR_DEV_CHKDEL_REQ = 0x0082;
	public static final int USR_DEV_CHKDEL_RES = 0x0083;
	public static final int USR_DEV_COUNT_REQ = 0x0084;
	public static final int USR_DEV_COUNT_RES = 0x0085;
}
