package andy.android.utils;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;

public class FileUtils {

	/**
	 * 取得檔案的副檔名
	 *
	 * @param filename
	 * @return
	 */
	public static String getFileExtension(String filename) {
		String fileExtension = "";
		if (!TextUtils.isEmpty(filename)) {
			int index = filename.lastIndexOf(".");
			if (index != -1) {
				String extension = filename.substring(index);
				fileExtension = MimeTypeMap.getFileExtensionFromUrl(extension);
			}
		}
		return fileExtension;
	}

	public static String formatFileSize(long size) {
		if(size <= 0){
			return "0";
		}
		final String[] units = new String[] { "Bytes", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
		String pattern;
		if(digitGroups > 1){
			pattern = "#,##0.##";
		}else{
			pattern = "#,##0";
		}
		double value = size/Math.pow(1024, digitGroups);
		return new DecimalFormat(pattern).format(value) + " " + units[digitGroups];
	}

	/**
	 * @param srcFile
	 * @return
	 */
	public static long getFolderFileSize(File srcFile) {
		long length = 0;
		if(srcFile.isDirectory()) {
			for (File file : srcFile.listFiles()) {
				if (file.isFile())
					length += file.length();
				else
					length += getFolderFileSize(file);
			}
		} else {
			length = srcFile.length();
		}
		return length;
	}


	public static void copyFileToDirectory(String directoryPath, File sourceFile,
											  boolean isKeepSourceFolder) throws Exception {
		if (TextUtils.isEmpty(directoryPath) || (sourceFile == null || !sourceFile.exists())) {
			return;
		}
			File dir = new File(directoryPath);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					return;
				}
			}
			if(!sourceFile.isDirectory()) {
				File dest = new File(directoryPath, sourceFile.getName());
				if (!dest.exists()) {
					if (!dest.createNewFile()) {
						return;
					}
				}
				copyFile(dest.getAbsolutePath(), sourceFile);
			} else {
				File dest;
				if(isKeepSourceFolder) {
					dest = new File(directoryPath, sourceFile.getName());
					if (!dest.exists()) {
						if (!dest.mkdirs()) {
							return;
						}
					}
				}else{
					dest = new File(directoryPath);
				}
				File[] sourceFiles = sourceFile.listFiles();
				if(sourceFiles != null && sourceFiles.length >0 ) {
					for (File file: sourceFiles){
						copyFileToDirectory(dest.getAbsolutePath(), file, true);
					}
				}
			}
	}

	public static void copyFile(String destPath, File sourceFile) throws Exception{
		if (TextUtils.isEmpty(destPath) || (sourceFile == null || !sourceFile.exists())) {
			return;
		}

		File destFile = new File(destPath);
		FileChannel src = null;
		FileChannel dst = null;
		try {
			src = new FileInputStream(sourceFile).getChannel();
			dst = new FileOutputStream(destFile).getChannel();
			dst.transferFrom(src, 0, src.size());
			src.close();
			dst.close();
		}  finally {

			if (src != null) {
				src.close();
			}
			if (dst != null) {
				dst.close();
			}
		}
	}
}
