package cc.ecisr.vocnyan.utils;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

public class ColorUtil {
	/**
	 * 將輸入顏色的明度乘以一個係數再返回
	 * 以調節該顏色明度
	 * 爲了保持顏色的飽和度，明度在乘以係數的同時，飽和度除以該係數的平方
	 * 如，係數爲 0.5 時，明度降爲一半，飽和度昇爲四倍
	 *
	 * @param colorString 表示顏色的十六進制字符串，如"#FFFFFFF"
	 * @param ratio 明度係數
	 * @return 以整形數字表示的顏色代碼
	 */
	public static int darken(String colorString, double ratio) {
		return darken(Color.parseColor(colorString), ratio);
	}
	public static int darken(int color, double ratio) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] *= ratio;
		hsv[1] /= ratio*ratio;
		return Color.HSVToColor(hsv);
	}
	
	/**
	 * 將色相空間分為 max 份，返回第 i 份顏色，i 從 1 開始計
	 */
	public static String ithColorInHsv(int i, int max) {
		float[] hsv = new float[3];
		hsv[0] = (float)((int)(i/2f+1) + ((i%2==0)?(int)(max/2f-0.5):0) - 1) * 360 / max;
		hsv[1] = 0.4f;
		hsv[2] = 0.8f;
		int color = Color.HSVToColor(hsv);
		return "#" + Integer.toString(color, 16);
	}
	
	public static int parseColor(String s) {
		if (s.startsWith("#")) {
			return Integer.parseInt(s.substring(1), 16) + 0xFF000000;
		} else {
			return Integer.parseInt(s, 16) + 0xFF000000;
		}
	}
}
