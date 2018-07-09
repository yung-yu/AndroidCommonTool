package andy.android.utils;


import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import andy.android.manager.PermissionManager;


/**
 * 要求或檢查Android權限的工具類別
 */

public class PermissionUtil {

	public static boolean checkPermissions(Context context, String[] permissions){
		if(permissions != null && permissions.length > 0) {
			for(String p: permissions) {
				if(!checkPermission(context, p)){
					return false;
				}
			}
		}
		return true;
	}

	public static boolean checkPermission(Context context, String p){
		return ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED;
	}


	public static void requestPermissions(FragmentActivity activity, String[] permissions, String explanations, PermissionManager.onPermissionRequestCallBack callBack){
		PermissionManager.getInstance().requestPermissions(activity, permissions, explanations, callBack);
	}


	public static void requestPermission(FragmentActivity activity, String permission, String explanations, PermissionManager.onPermissionRequestCallBack callBack){
		PermissionManager.getInstance().requestPermissions(activity, new String[]{permission}, explanations, callBack);
	}

	public static void onActivityRequestPermissionsResult(FragmentActivity activity, int requestCode, String[] permissions, int[] grantResults){
		PermissionManager.getInstance().onRequestPermissionsResult(activity, requestCode, permissions, grantResults);
	}

}