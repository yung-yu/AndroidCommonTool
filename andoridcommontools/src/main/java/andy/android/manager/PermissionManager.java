package andy.android.manager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import com.andy.andoridcommontools.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import andy.android.utils.PermissionUtil;


public class PermissionManager {
	private final static String TAG = "PermissionManager";
	private static PermissionManager instance;
	private static final int REQUEST_CODE = 123;
	private List<String> deniedLists = new ArrayList<>();
	private onPermissionRequestCallBack callBack = null;
	private String explanations = "";

	public static PermissionManager getInstance(){
		if(instance == null){
			instance = new PermissionManager();
		}
		return instance;
	}


	public interface onPermissionRequestCallBack{
		void onRequestSuccess(List<String> permissions, boolean isFromUserAgree);
		void onGotoSetPermission();
		void onRequestFailed(List<String> failedList);
	}

	public void requestPermissions(FragmentActivity activity, String[] permissions, String explanations, onPermissionRequestCallBack callBack){
		if(activity == null){
			return;
		}
		if(permissions==null || permissions.length==0){
			return;
		}
		this.explanations = explanations;
		deniedLists.clear();
		this.callBack = callBack;
		for(String p: permissions) {
			if(!PermissionUtil.checkPermission(activity, p)){
				deniedLists.add(p);
			}
		}
		if(deniedLists.size() > 0) {
			ActivityCompat.requestPermissions(activity, deniedLists.toArray(new String[deniedLists.size()]), REQUEST_CODE);
		} else {
			if (this.callBack != null) {
				this.callBack.onRequestSuccess(Arrays.asList(permissions), false);
				this.callBack = null;
			}

		}
	}

	public void onRequestPermissionsResult(FragmentActivity activity, int requestCode, String[] permissions, int[] grantResults){
		if(activity == null){
			return;
		}
		if( requestCode == REQUEST_CODE ){
			List<String> failedList = new ArrayList<>();
			List<String> successList = new ArrayList<>();
			for(int i = 0; i < grantResults.length; i++){
				if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
					failedList.add(permissions[i]);
				} else {
					successList.add(permissions[i]);
				}
			}

			if (failedList.isEmpty()) {
				if (callBack != null) {
					callBack.onRequestSuccess(successList, true);
					callBack = null;
				}
			} else {
				showExplanationDialog(activity, failedList, explanations);
			}
		}
	}

	private void showExplanationDialog(final FragmentActivity activity, final List<String> failedList, String explanation){
		if (TextUtils.isEmpty(explanation)||failedList.isEmpty()){
			return;
		}
		AlertDialog.Builder ab = new AlertDialog.Builder(activity);
		ab.setMessage(explanation);
		ab.setCancelable(false);
		ab.setPositiveButton(R.string.ok, (dialog, which) -> {
			if (callBack != null) {
				callBack.onGotoSetPermission();
				callBack = null;
			}
			Intent intent = new Intent();
			intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
			intent.setData(uri);
			activity.startActivity(intent);
			dialog.cancel();
		});
		ab.setNegativeButton(R.string.notNow, (dialog, which) -> {
			if (callBack != null) {
				callBack.onRequestFailed(failedList);
				callBack = null;
			}
			dialog.cancel();
		});
		ab.create().show();
	}

}
