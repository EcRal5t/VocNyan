package cc.ecisr.vocnyan.utils;

import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.SystemFonts;
import android.util.Log;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Set;

public class FontUtil {
	private static final String TAG = "`FontUtil";
	
	
	static public class Fonts {
		Typeface tf = null;
		
		public Fonts(ArrayList<File> fontFiles) {
			if (fontFiles.isEmpty()) { return; }
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
				return;
			}
			try {
				Typeface.CustomFallbackBuilder builder = new Typeface.CustomFallbackBuilder(
						new FontFamily.Builder(new Font.Builder(fontFiles.get(0)).build()).build());
				for (int index = 1; index<fontFiles.size(); index++) {
					builder.addCustomFallback(
							new FontFamily.Builder(new Font.Builder(fontFiles.get(index)).build()).build());
				}
				tf = builder.build();
				
//				Set<Font> fs = SystemFonts.getAvailableFonts();
//				for (Font f: fs) {
//					Log.i(TAG, "Fonts: " + f);
//				}
			
			} catch (java.io.IOException ignore) { }
		}
		public Typeface getFont() { return tf; }
	}
}
