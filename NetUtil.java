package cn.gnsit.wenwan.app.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetUtil {

	private NetUtil(){}
	/**
	 * 判断网络是否连接
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isConnected(Context context) {

		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (null != connectivity) {
			NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (null != info && info.isConnected()) {
				if (info.getState() == NetworkInfo.State.CONNECTED) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 判断是否是wifi连接
	 */
	public static boolean isWifi(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null || cm.getActiveNetworkInfo() == null)
			return false;
		else
			return cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;

	}

	/**
	 * 判断是否是手机连接
	 */
	public static boolean isMobile(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null || cm.getActiveNetworkInfo() == null)
			return false;
		else
			return cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE;

	}

	/**
	 * 获取网络类型 联通的3G为UMTS或HSDPA，移动和联通的2G为GPRS或EDGE，电信的2G为CDMA，电信的3G为EVDO<br/>
	 * NETWORK_TYPE_CDMA 网络类型为CDMA<br/>
	 * NETWORK_TYPE_EDGE 网络类型为EDGE<br/>
	 * NETWORK_TYPE_EVDO_0 网络类型为EVDO0<br/>
	 * NETWORK_TYPE_EVDO_A 网络类型为EVDOA<br/>
	 * NETWORK_TYPE_GPRS 网络类型为GPRS<br/>
	 * NETWORK_TYPE_HSDPA 网络类型为HSDPA<br/>
	 * NETWORK_TYPE_HSPA 网络类型为HSPA<br/>
	 * NETWORK_TYPE_HSUPA 网络类型为HSUPA<br/>
	 * NETWORK_TYPE_UMTS 网络类型为UMTS<br/>
	 * 
	 * @param context
	 * @return
	 */
	public static int getNetType(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null || cm.getActiveNetworkInfo() == null)
			return -1;
		else
			return cm.getActiveNetworkInfo().getType();

	}

	/**
	 * 获取手机ip
	 * 
	 * @return
	 */
	public static String getPhoneIp() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
						// if (!inetAddress.isLoopbackAddress() && inetAddress
						// instanceof Inet6Address) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (Exception e) {
		}
		return "";
	}

	/**
	 * 打开网络设置界面
	 */
	public static void openSetting(Activity activity) {
		Intent intent = new Intent("/");
		ComponentName cm = new ComponentName("com.android.settings", "com.android.settings.WirelessSettings");
		intent.setComponent(cm);
		intent.setAction("android.intent.action.VIEW");
		activity.startActivityForResult(intent, 0);
	}
}
