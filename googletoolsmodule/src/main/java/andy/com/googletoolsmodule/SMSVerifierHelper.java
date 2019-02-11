package andy.com.googletoolsmodule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class SMSVerifierHelper {
	static SMSVerifierHelper instance;

	public static SMSVerifierHelper getInstance() {
		if(instance == null){
			instance = new SMSVerifierHelper();
		}
		return instance;
	}

	public interface onSmsEventListener{
		void onSmsRec(String smsMsg);
		void onTimeOut();
	}

	private static final String TAG = "SMSVerifierHelper";
	private SmsBrReceiver smsReceiver;
	private SmsRetrieverClient smsRetrieverClient;
	private onSmsEventListener listener;

	public void setListener(onSmsEventListener listener) {
		this.listener = listener;
	}

	public void init(@NonNull Context context){
		if (smsReceiver == null) {
			smsReceiver = new SmsBrReceiver();
		}
		smsRetrieverClient = SmsRetriever.getClient(context);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);
		context.getApplicationContext().registerReceiver(smsReceiver, intentFilter);
		new AppSignatureHelper(context).getAppSignatures();
	}

	public void start(){
		// Start SMS receiver code
		if(smsReceiver == null || smsRetrieverClient == null){
			throw new IllegalStateException("not call init method");
		}
		Task<Void> task = smsRetrieverClient.startSmsRetriever();
		task.addOnSuccessListener(new OnSuccessListener<Void>() {
			@Override
			public void onSuccess(Void aVoid) {
				smsReceiver.setTimeout();
				Log.d(TAG, "SmsRetrievalResult status: Success");

			}
		});
		task.addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				Log.e(TAG, "SmsRetrievalResult start failed.", e);
			}
		});
	}

	public void release(Context context){
		listener = null;
		smsRetrieverClient = null;
		if (smsReceiver != null) {
			context.getApplicationContext().unregisterReceiver(smsReceiver);
			smsReceiver.cancelTimeout();
			smsReceiver = null;
		}
		instance = null;
	}

	class SmsBrReceiver extends BroadcastReceiver {
		private int MAX_TIMEOUT = 1800000;
		Handler h = new Handler();
		Runnable r = new Runnable() {
			@Override
			public void run() {
				doTimeout();
			}
		};

		public void setTimeout() {
			h.postDelayed(r, MAX_TIMEOUT);
		}

		public void cancelTimeout() {
			h.removeCallbacks(r);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null) {
				return;
			}

			String action = intent.getAction();
			if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(action)) {
				Bundle extras = intent.getExtras();
				Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
				switch(status.getStatusCode()) {
					case CommonStatusCodes.SUCCESS:
						String smsMessage = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
						Log.d(TAG, "Retrieved sms code: " + smsMessage);
						if(listener != null){
							listener.onSmsRec(smsMessage);
						}
						break;
					case CommonStatusCodes.TIMEOUT:
						doTimeout();
						break;
					default:
						break;
				}
			}
		}

		private void doTimeout() {
			if(listener != null){
				listener.onTimeOut();
			}
		}
	}
}
