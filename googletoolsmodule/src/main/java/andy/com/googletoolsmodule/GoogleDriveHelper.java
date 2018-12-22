package andy.com.googletoolsmodule;


import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Google Drive 使用
 */
public class GoogleDriveHelper {
	private static final String TAG = "GoogleDriveHelper";
	private static final int REQUEST_CODE_SIGN_IN = 111;
	private static final int REQUEST_CODE_GET_ACCOUNT = 222;
	private static final int REQUEST_CODE_APP_PERMISSION = 333;

	private static final String APPDATAFOLDER = "appDataFolder";
	private static final int CHUNK_SIZE =  2*1024*1024;

	private GoogleSignInClient mGoogleSignInClient;
	private static GoogleDriveHelper instance;
	private OnGoogleSignEventListener signInListener;
	private Drive driveService;


	public interface OnGoogleSignEventListener {
		void onSuccess();

		void onCancel();

		void onFailed();
	}
	public interface OnUploadFileListener {
		void onSuccess();

		void onProgress(int progress);

		void onFailed();

		void onCancel();

		boolean isCancel();
	}
	public interface OnDownloadFileListener {
		void onSuccess(@Nullable java.io.File file, Map<String, String> appData);

		void onProgress(int progress);

		void onFailed();

		void onCancel();

		boolean isCancel();
	}

	public interface OnDeleteFileListener {
		void onSuccess();
		void onFailed();
	}

	public interface OnFindFileMetaDataListener {
		void onSuccess(Date createDate, long fileSize);

		void onFailed();
	}

	public interface OnCheckListener {
		void onResult(boolean result);
	}

	public interface OnFileListListener {
		void onResult(@Nullable List<File> data);
	}

	public static GoogleDriveHelper getInstance() {
		if (instance == null) {
			instance = new GoogleDriveHelper();
		}
		return instance;
	}
	private static HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
	private static  JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();


	/**
	 * Creates an authorized Credential object.
	 * @return An authorized Credential object.
	 */
	private  GoogleAccountCredential getCredentials(Context context) {
		// Load client secrets.
		GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context.getApplicationContext());
		if (account != null) {
			GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_APPDATA));
			credential.setSelectedAccount(account.getAccount());
			return credential;
		}
		return null;
	}

	private Drive getDriveService(Context context) {
		GoogleAccountCredential credential = getCredentials(context);
		if(credential != null ) {
			if(driveService == null) {
				driveService =  new Drive.Builder(HTTP_TRANSPORT,
						JSON_FACTORY, credential).setApplicationName(context.getString(R.string.app_name)).build();

			}
			return driveService;
		} else {
			return null;
		}
	}
	/**
	 * 執行Google sin-in
	 *
	 */
	public void signIn(FragmentActivity activity, @NonNull OnGoogleSignEventListener signInListener) {
		Log.d(TAG, "signIn");
		this.signInListener = signInListener;
		driveService = null;
		GoogleSignInAccount account = getGoogleAccount(activity);
		Intent intent = AccountManager.newChooseAccountIntent(account != null ? account.getAccount() : null, null, new String[]{"com.google"},
				true, null, null, null, null);
		activity.startActivityForResult(intent, REQUEST_CODE_GET_ACCOUNT);

	}


	/**
	 * 檢查App folder權限
	 *
	 */
	private boolean checkAndRequestAppFolderPermission(Context context) {
		if (GoogleSignIn.hasPermissions(
				GoogleSignIn.getLastSignedInAccount(context),
				new Scope(DriveScopes.DRIVE_APPDATA))) {
			Log.d(TAG, "true");
			return true;
		}
		Log.d(TAG, "false");
		return false;
	}

	/**
	 * 取得登入google account
	 *
	 * @return GoogleSignInAccount
	 */
	private GoogleSignInAccount getGoogleAccount(Context context) {
		GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(context);
		if (googleSignInAccount != null) {
			if (PermissionUtil.checkPermission(context, Manifest.permission.GET_ACCOUNTS)) {
				if (Tools.checkGoogleAccountIsExist(context, googleSignInAccount.getEmail())) {
					return googleSignInAccount;
				} else {
					return null;
				}
			}
		}
		return googleSignInAccount;
	}

	/**
	 * 取得登入google account Email
	 *
	 * @return Email String
	 */
	public boolean isGoogleAccountSignIn(Context context) {
		GoogleSignInAccount googleSignInAccount = getGoogleAccount(context);
		return googleSignInAccount != null && checkAndRequestAppFolderPermission(context);
	}

	/**
	 * 取得登入google account Email
	 *
	 * @return Email String
	 */
	public String getGoogleAccountEmail(Context context) {
		GoogleSignInAccount googleSignInAccount = getGoogleAccount(context);
		if (googleSignInAccount != null) {
			return googleSignInAccount.getEmail();
		}
		return null;
	}

	/**
	 * 需要使用google Drive 服務Activity的onActivityResult()執行
	 *
	 */
	public void onActivityResult(FragmentActivity activity, int requestCode, int resultCode, Intent data) {
		Log.d(TAG, requestCode + " " + resultCode + " " + data);
		if (requestCode == REQUEST_CODE_SIGN_IN) {
			if (resultCode == Activity.RESULT_OK) {
				if (checkAndRequestAppFolderPermission(activity)) {
					Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
					GoogleSignInAccount account = handleSignInResult(task);
					if (account != null) {
						if (signInListener != null) {
							signInListener.onSuccess();
						}
					} else {
						if (signInListener != null) {
							signInListener.onFailed();
						}
					}
				} else {
					GoogleSignIn.requestPermissions(
							activity,
							REQUEST_CODE_APP_PERMISSION,
							getGoogleAccount(activity),
							new Scope(DriveScopes.DRIVE_APPDATA));
				}
			} else {
				if (signInListener != null) {
					signInListener.onCancel();
				}
			}
		} else if (requestCode == REQUEST_CODE_APP_PERMISSION) {
			if (resultCode == Activity.RESULT_OK) {
				GoogleSignInAccount account = getGoogleAccount(activity);
				if (account != null) {
					if (signInListener != null) {
						signInListener.onSuccess();
					}
				} else {
					if (signInListener != null) {
						signInListener.onCancel();
					}
				}
			} else {
				if (signInListener != null) {
					signInListener.onCancel();
				}
			}
		}
		if (requestCode == REQUEST_CODE_GET_ACCOUNT) {
			if (resultCode == Activity.RESULT_OK) {
				String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				GoogleSignInAccount account = getGoogleAccount(activity);
				if (account != null) {
					if (!TextUtils.isEmpty(account.getEmail())
							&&account.getEmail().equals(accountName)) {
						Log.d(TAG, "google sign in same account");
						mGoogleSignInClient = buildGoogleSignInClient(activity, account.getEmail());
						activity.startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
					} else {
						Log.d(TAG, "google sign in different account");
						mGoogleSignInClient = buildGoogleSignInClient(activity, null);
						mGoogleSignInClient.signOut().addOnSuccessListener(aVoid1 -> {
							Log.d(TAG, "google sign out success");
							mGoogleSignInClient = buildGoogleSignInClient(activity, accountName);
							activity.startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
						});
					}
				} else {
					Log.d(TAG, "google sign in no account");
					mGoogleSignInClient = buildGoogleSignInClient(activity, accountName);
					activity.startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
				}
			} else {
				if (signInListener != null) {
					signInListener.onCancel();
				}
			}
		}
	}



	/**
	 * 建立Ｇoogle sign-in 的物件
	 *
	 * @return GoogleSignInClient
	 */
	private GoogleSignInClient buildGoogleSignInClient(Context context, String accountName) {
		GoogleSignInOptions signInOptions;
		if (TextUtils.isEmpty(accountName)) {
			signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
					.requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
					.requestEmail()
					.build();
		} else {
			signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
					.requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
					.setAccountName(accountName)
					.requestEmail()
					.build();
		}
		return GoogleSignIn.getClient(context, signInOptions);
	}



	/**
	 * 處理收到google sign-in call back 結果
	 */
	private GoogleSignInAccount handleSignInResult(Task<GoogleSignInAccount> completedTask) {
		try {
			if (completedTask.isSuccessful()) {
				return completedTask.getResult(ApiException.class);
			}
		} catch (ApiException e) {
			Log.e(e);
			Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
		}
		ApiException exception = (ApiException) completedTask.getException();
		if (exception != null) {
			Log.w(TAG, "signInResult:failed code=" + exception.getStatusCode());
		}
		return null;

	}


	/**
	 * 上傳檔案到Google drive的App Folder
	 *
	 */
	public Thread uploadFileToAppFolder(Context context, java.io.File sourceFile, String fileName, String mineType, Map<String, String> appData, @NonNull OnUploadFileListener listener) {
		Log.d(TAG, fileName + " " + mineType + " " + sourceFile.toString());
		Thread uploadThread = new Thread(){
			@Override
			public void run() {
				super.run();
				Drive drive = getDriveService(context);
				if(drive == null){
					listener.onFailed("");
					return;
				}
				File fileMetadata = new File();
				if(appData != null){
			  		fileMetadata.setAppProperties(appData);
				}
				fileMetadata.setName(fileName);
				fileMetadata.setParents(Collections.singletonList(APPDATAFOLDER));
				FileContent mediaContent = new FileContent(mineType, sourceFile);
				try {
					Drive.Files.Create create = drive.files().create(fileMetadata, mediaContent).setFields("id");
					MediaHttpUploader uploader = create.getMediaHttpUploader();
					uploader.setChunkSize(MediaHttpUploader.DEFAULT_CHUNK_SIZE > sourceFile.length()?MediaHttpUploader.MINIMUM_CHUNK_SIZE:CHUNK_SIZE);
					uploader.setProgressListener(new MediaHttpUploaderProgressListener() {
						@Override
						public void progressChanged(MediaHttpUploader uploader) throws IOException {
							switch (uploader.getUploadState()) {
								case INITIATION_STARTED:
									Log.d(TAG, "INITIATION_STARTED");
									break;
								case INITIATION_COMPLETE:
									Log.d(TAG, "INITIATION_COMPLETE");
									break;
								case MEDIA_IN_PROGRESS:
									if(Log.IS_DEBUG) {
										Log.d(TAG, "MEDIA_IN_PROGRESS " + uploader.getProgress());
									}
									listener.onProgress((int) (uploader.getProgress() * 100f));
									break;
								case MEDIA_COMPLETE:
									Log.d(TAG, "MEDIA_COMPLETE");
									listener.onProgress(100);
									break;
								case NOT_STARTED:
									Log.d(TAG, "NOT_STARTED");
									listener.onFailed(context.getString(R.string.google_backup_failed));
									break;
							}
						}
					});
					File file = create.execute();
					if(Log.IS_DEBUG) {
						Log.d(TAG, " upload success " + file.getId());
					}
					try {
						//刪除歷史檔案
						FileList fileResult = drive.files().list()
								.setQ("name = '" + fileName + "'")
								.setSpaces(APPDATAFOLDER)
								.setFields("nextPageToken, files(id)")
								.execute();
						if (fileResult != null) {
							List<File> allFiles = fileResult.getFiles();
							if (allFiles != null && allFiles.size() > 1) {
								for (int i = 1; i < allFiles.size(); i++) {
									if (!file.getId().equals(allFiles.get(i).getId())) {
										drive.files().delete(allFiles.get(i).getId()).execute();
									}
								}
							}
						}
					} catch (Exception e){
						Log.e(e);
					}
					listener.onSuccess();
				} catch (Exception e) {
					Log.e(e);
					if(e instanceof GoogleJsonResponseException){
						GoogleJsonResponseException jsonResponseException = (GoogleJsonResponseException) e;
						if(jsonResponseException.getDetails().getCode() == 403){
							if(jsonResponseException.getDetails().getErrors() != null &&
									jsonResponseException.getDetails().getErrors().size() > 0&&
									"storageQuotaExceeded".equals(jsonResponseException.getDetails().getErrors().get(0).getReason())) {
								listener.onFailed();
								return;
							}
						}
					}
					if(listener.isCancel()){
						listener.onCancel();
					} else {
						listener.onFailed();
					}

				}
			}
		};
		uploadThread.start();
		return uploadThread;
	}



	/**
	 * 取得App folder指定 title的檔案 metaData
	 *
	 */
	public void getCreateDateAndFileSizeFromAppFolder(Context context, String fileName, @NonNull OnFindFileMetaDataListener listener) {
		Log.d(TAG, fileName);
		new Thread(){
			@Override
			public void run() {
				super.run();
				Drive drive = getDriveService(context);
				if(drive == null){
					listener.onFailed();
					return;
				}

				try {
					FileList result = drive.files().list()
							.setQ("name = '"+fileName+"'")
							.setSpaces(APPDATAFOLDER)
							.setFields("nextPageToken, files(createdTime,id,name,size)")
							.execute();
					List<File> data = result.getFiles();
					if(data != null && data.size() > 0){
						long createDate = data.get(0).getCreatedTime().getValue();
						long  fileSize = data.get(0).getSize();
						listener.onSuccess(new Date(createDate), fileSize);
					} else {
						listener.onFailed();
					}
				} catch (Exception e) {
					Log.e(e);
					listener.onFailed();
				}

			}
		}.start();


	}


	public void checkFileIsExist(Context context, String fileName, @NonNull OnCheckListener listener) {
		new Thread(){
			@Override
			public void run() {
				super.run();
				Drive drive = getDriveService(context);
				if(drive == null){
					listener.onResult(false);
					return;
				}
				try {
					FileList result = drive.files().list()
							.setQ("name = '"+fileName+"'")
							.setSpaces(APPDATAFOLDER)
							.setFields("nextPageToken, files(id,name)")
							.execute();
					List<File> data = result.getFiles();
					listener.onResult(data != null && data.size() > 0);
				} catch (Exception e) {
					Log.e(e);
					listener.onResult(false);
				}

			}
		}.start();

	}

	/**
	 * 取得App folder指定 title的檔案 file
	 *
	 */
	public Thread getAppFolderFile(Context context, String fileName, @NonNull OnDownloadFileListener listener) {
		Log.d(TAG, fileName);
		Thread downloadThread = new Thread(){
			@Override
			public void run() {
				super.run();
				Drive drive = getDriveService(context);
				if(drive == null){
					listener.onFailed();
					return;
				}
				try {
					FileList fileResult = drive.files().list()
							.setQ("name = '"+fileName+"'")
							.setSpaces(APPDATAFOLDER)
							.setFields("nextPageToken, files(id,name,size,appProperties)")
							.execute();
					List<File> data = fileResult.getFiles();
					if(data != null && data.size() > 0){
						File file = data.get(0);
						long restoreFileSize = file.getSize();
						long free = context.getCacheDir().getFreeSpace();
						if (Log.IS_DEBUG) {
							Log.d(TAG, "file size " + restoreFileSize);
							Log.d(TAG, "free space " + free);
						}
						if (restoreFileSize  > free) {
							listener.onFailed();
							return;
						}
						java.io.File restoreFile = new java.io.File(context.getCacheDir(), file.getName());
						if(restoreFile.exists()){
							if(restoreFile.delete()) {
								boolean result = restoreFile.createNewFile();
								Log.d(TAG, "restore recreate "+result);
							}
						}
						OutputStream output = new FileOutputStream(restoreFile){
							long writtenByte;
							long totalByte = restoreFileSize;
							int curProgress = -1;

							@Override
							public void write(byte[] b) throws IOException {
								super.write(b);
								this.writtenByte += b.length;
								int progress = (int) ((float) writtenByte / (float) totalByte * 100f);
								if(curProgress != progress) {
									curProgress = progress;
									listener.onProgress(progress);
								}
							}

							@Override
							public void write(byte[] b, int off, int len) throws IOException {
								super.write(b, off, len);
								if (len < b.length) {
									this.writtenByte += len;
								} else {
									this.writtenByte += b.length;
								}
								int progress = (int) ((float) writtenByte / (float) totalByte * 100f);
								if(curProgress != progress) {
									curProgress = progress;
									listener.onProgress(progress);
								}
							}

							@Override
							public void write(int b) throws IOException {
								super.write(b);
								this.writtenByte++;
								int progress = (int) ((float) writtenByte / (float) totalByte * 100f);
								if(curProgress != progress) {
									curProgress = progress;
									listener.onProgress(progress);
								}
							}
						};
						Drive.Files.Get request =  drive.files().get(file.getId());
						request.executeMedia().download(output);
						listener.onSuccess(restoreFile, file.getAppProperties());
					} else {
						listener.onFailed();
					}
				} catch (Exception e) {
					Log.e(e);
					if (listener.isCancel()) {
						listener.onCancel();
					} else {
						listener.onFailed();
					}
				}
			}
		};
		downloadThread.start();
		return downloadThread;
	}
	public void getAppFolderFileList(Context context, String fileName, @NonNull OnFileListListener listener) {
		new Thread(){
			@Override
			public void run() {
				super.run();
				Drive drive = getDriveService(context);
				if(drive == null){
					listener.onResult(null);
					return;
				}
				try {
					FileList result = drive.files().list()
							.setQ("name = '"+fileName+"'")
							.setSpaces(APPDATAFOLDER)
							.setFields("nextPageToken, files(id,name)")
							.execute();
					List<File> data = result.getFiles();
					listener.onResult(data);
				} catch (Exception e) {
					Log.e(e);
					listener.onResult(null);
				}

			}
		}.start();

	}
	/**
	 * 刪除在App Folder指定title的所有檔案
	 *
	 */
	public void deleteAppFolderFile(Context context, List<File> data , @NonNull OnDeleteFileListener listener) {
		Log.d(TAG, "");
		new Thread(){
			@Override
			public void run() {
				super.run();
				Drive drive = getDriveService(context);
				if(drive == null){
					listener.onFailed();
					return;
				}
				try {
					if(data != null && data.size() > 0){
						for(File file :data){
							drive.files().delete(file.getId()).execute();
						}
						listener.onSuccess();
					}
				} catch (Exception e) {
					Log.e(e);
					listener.onFailed();
				}
			}
		}.start();



	}
}
