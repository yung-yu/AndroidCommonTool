package andy.android.utils;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import java.util.Locale;

public class AndroidUtils {

	/**
	 * 關掉SoftKeyboard
	 */
	public static void closeSoftKeyboard(Context context, View View) {
		if (context == null) {
			return;
		}
		if (View == null) {
			return;
		}
		InputMethodManager im = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if(im !=null){
			im.hideSoftInputFromWindow(View.getWindowToken(), 0);
		}

	}

	public static void showSoftKeyboard(Context context, View view) {
		if (context == null) {
			return;
		}
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if(imm !=null){
			imm.showSoftInput(view, 0);
		}
	}

	public static void startFragment(FragmentManager manager, Fragment targetFragment, int widgetId, Bundle bundle, boolean addFragmentToStack) {
		if (targetFragment !=null && manager != null) {
			try {
				if (bundle != null) {
					targetFragment.setArguments(bundle);
				}
				FragmentTransaction transaction = manager.beginTransaction();
				transaction.replace(widgetId, targetFragment, targetFragment.getClass().getName());
				if (addFragmentToStack) {
					transaction.addToBackStack(targetFragment.getClass().getName());
				}
				transaction.commitAllowingStateLoss();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void addFragment(FragmentManager manager, Fragment sourceFragment,
								   Fragment targetFragment, int widgetId, Bundle bundle) {
		try {
			if (manager != null) {
				Fragment preFragment = manager.findFragmentByTag(targetFragment.getClass().getName());
				FragmentTransaction transaction = manager.beginTransaction();
				if (preFragment != null) {
					transaction.remove(preFragment);
				}
				if (sourceFragment != null) {
					transaction.hide(sourceFragment);
				}
				targetFragment.setArguments(bundle);
				transaction.add(widgetId, targetFragment, targetFragment.getClass().getName());
				transaction.addToBackStack(targetFragment.getClass().getName());
				transaction.commitAllowingStateLoss();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 啟動新的DialogFragment使用的method
	 */
	public static void startDialogFragment(FragmentManager manager, DialogFragment dialogFragment, Bundle bundle, boolean addFragmentToStack) {
		if (dialogFragment != null && manager != null) {
			String tag = dialogFragment.getClass().getName();
			if (manager.findFragmentByTag(tag) == null) {
				FragmentTransaction ft = manager.beginTransaction();
				if(addFragmentToStack){
					ft.addToBackStack(dialogFragment.getClass().getName());
				}
				if (bundle != null) {
					dialogFragment.setArguments(bundle);
				}
				if(!dialogFragment.isAdded()){
					dialogFragment.show(ft, dialogFragment.getClass().getName());
				}
			}
		}
	}
	public static void finish(FragmentManager manager, boolean isImmediate) {
		if (manager == null) {
			return;
		}
		try {
			if (isImmediate) {
				manager.popBackStackImmediate();
			} else {
				manager.popBackStack();
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void popupAllFragment(FragmentManager manager) {
		try {
			manager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Fragment getCurrentFragment(FragmentManager manager){
		int count = manager.getBackStackEntryCount();
		if(count-1< 0 ){
			return null;
		}
		FragmentManager.BackStackEntry backStackEntry = manager.getBackStackEntryAt(count - 1);
		return manager.findFragmentByTag(backStackEntry.getName());
	}
	/**
	 * This method converts dp unit to equivalent pixels, depending on device density.
	 *
	 * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
	 * @param c Context to get resources and device specific display metrics
	 * @return A float value to represent px equivalent to dp depending on device density
	 */
	public static float convertDpToPixel(float dp, Context c) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics());
	}

	public static Fragment getTopFragment(FragmentManager manager) {
		int count = manager.getBackStackEntryCount();
		if (count > 0) {
			FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(count - 1);
			String name = entry.getName();
			return  manager.findFragmentByTag(name);
		}
		return null;
	}
	public static void restartApp(Activity activity){

		PendingIntent RESTART_INTENT = PendingIntent.getActivity(activity.getBaseContext(), 0,
				new Intent(activity.getBaseContext(), activity.getClass()), PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager mgr = (AlarmManager)activity.getSystemService(Context.ALARM_SERVICE);
		if(mgr !=null){
			mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, RESTART_INTENT);
		}
		System.exit(2);
	}
	/**
	 * 取得 App Package Name
	 * @param context Context
	 */
	public static String getAppPackageName(Context context) {
		PackageManager manager = context.getPackageManager();
		try {
			PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
			return info.packageName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 更新statusBar color
	 * @param  activity
	 * @param  color
	 */
	public static void setStatusBarColor(Activity activity, int color){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = activity.getWindow();
			window.setStatusBarColor(color);
		}
	}
	/**
	 *  變更APP語系
	 * @param context
	 * @param myLocale
	 */
	public static void changeLanguage(Context context, Locale myLocale) {
		android.content.res.Configuration config = context.getResources().getConfiguration();
		config.locale = myLocale;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			context.createConfigurationContext(config);
		} else {
			context.getResources().updateConfiguration(config, null);
		}
	}
}