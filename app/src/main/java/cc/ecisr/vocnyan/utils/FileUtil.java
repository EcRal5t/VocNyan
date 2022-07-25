package cc.ecisr.vocnyan.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

public class FileUtil {
	private static final String TAG = "`FileUtil";

	public static int copyFileToDes(String src, String des) {
		//String desPath = StringUtil.arrayToString(des.split(File.separator));
		File desParentFile = new File(des).getParentFile();
		if (desParentFile==null || !desParentFile.exists() && !desParentFile.mkdir()) {
			Log.e(TAG, "Cannot create des' parent dir.");
			return -1;
		}
		
		try {
			//InputStream is = context.getAssets().open(src);
			InputStream is = new FileInputStream(src);
			Log.i(TAG, "orig length: " + is.available());
			OutputStream os = new FileOutputStream(des);
			byte[] buffer = new byte[1024];
			int length, t = 0;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
				t += length;
			}
			os.flush();
			os.close();
			is.close();
			return t;
		} catch (java.io.FileNotFoundException e) {
			Log.e(TAG, "java.io.FileNotFoundException");
			return -1;
		} catch (java.io.IOException e) {
			Log.e(TAG, "java.io.IOException");
			return -1;
		}
	}
	
	public static void copy(String srcPath, String dstPath) {
		try {
			makeParentDirs(srcPath);
			File srcFile = new File(srcPath);
			File dstFile = new File(dstPath);
			FileInputStream srcStream = new FileInputStream(srcFile);
			FileOutputStream dstStream = new FileOutputStream(dstFile);
			FileChannel srcChannel = srcStream.getChannel();
			FileChannel dstChannel = dstStream.getChannel();
			srcChannel.transferTo(0, srcChannel.size(), dstChannel);
			srcStream.close();
			dstStream.close();
		} catch (IOException e) {
			Log.e(TAG, "IOException");
		}
	}
	
	public static boolean makeParentDirs(String path) {
		File parent = new File(path).getParentFile();
		if (parent!=null && !parent.exists()) {
			return parent.mkdirs();
		}
		return false;
	}
	
	public static boolean isFile(String path) {
		File file = new File(path);
		// return file.isFile();
		return file.exists() && file.isFile();
	}
	public static boolean isDir(String path) {
		File file = new File(path);
		// return file.isFile();
		return file.exists() && file.isDirectory();
	}
	public static boolean deleteFileIfExist(String path) {
		File file = new File(path);
		// return file.delete();
		if (file.isFile()) { return file.delete(); }
		return false;
	}
}
