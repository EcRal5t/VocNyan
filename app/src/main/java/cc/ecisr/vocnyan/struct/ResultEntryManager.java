package cc.ecisr.vocnyan.struct;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cc.ecisr.vocnyan.utils.ColorUtil;
import cc.ecisr.vocnyan.utils.DbUtil;
import cc.ecisr.vocnyan.utils.StringUtil;

public class ResultEntryManager {
	private static final String TAG = "`ResultEntryManager";
	public enum KEY {
		ID("id"), TERM("term"), SUP("supplement"), CLASS("class"), AFFIX("affixrule"), MEAN("meanings");
		private final String key;
		KEY(String key) { this.key = key; }
		@NonNull public String toString() { return this.key; }
	}
	public static SQLiteDatabase db = null;
	
	public static void initialize() {
		db = DbUtil.getDb();
	}
	
	static public ResultEntry[] search(String s) {
		if (".".equals(s)) {
			return retrieveResultBySql(KEY.ID, ">=", "0", "ORDER BY `" + KEY.ID + "` DESC");
		}
		try { Pattern.compile(s); } catch (PatternSyntaxException ignored) { return new ResultEntry[0]; }
		if (s.matches("\\d+")) {
			ResultEntry[] inId = retrieveResultBySql(KEY.ID, ">=", s);
			ResultEntry[] inTerm = retrieveResult(KEY.TERM, s);
			return StringUtil.concat(inId, inTerm);
		} else {
			ResultEntry[] inTerm = retrieveResult(KEY.TERM, s);
			ResultEntry[] inMean = retrieveResult(KEY.MEAN, ".*" + s);
			//ResultEntry[] inClass = retrieveResult(KEY.CLASS, ".*" + s);
			//return StringUtil.concat(StringUtil.concat(inTerm, inMean), inClass);
			return StringUtil.concat(inTerm, inMean);
		}
	}
	
	static ResultEntry[] retrieveResult(KEY col, String s) {
		DictManager.ArrayListMap<String> replaceMap = DictManager.getSelectingMap(DictManager.KEY.REPLACE_MAP);
		DictManager.ArrayListMap<String> ellipsisMap = DictManager.getSelectingMap(DictManager.KEY.ELLIPSIS_MAP);
		
		String replacedS = s;
		for (int index = 0; index < replaceMap.getCount(); index++) {
			replacedS = replacedS.replaceAll(replaceMap.key(index), replaceMap.val(index));
		}
		ResultEntry[] resultEntries1 = retrieveResultBySql(col, DbUtil.REG, replacedS + "[^" + Configs.SEARCH_SEPARATE_SIGN + "]*");
		
		String ellipsisS = replacedS;
		for (int index=0; index < ellipsisMap.getCount(); index++) {
			ellipsisS = ellipsisS.replaceAll(ellipsisMap.key(index), ellipsisMap.val(index));
		}
		if (ellipsisS.equals(replacedS)) return resultEntries1;
		ResultEntry[] resultEntries2 = retrieveResultBySql(col, DbUtil.REG, ellipsisS + "[^" + Configs.SEARCH_SEPARATE_SIGN + "]*");
		
		return StringUtil.concat(resultEntries1, resultEntries2);
	}
	
	@NonNull
	static ResultEntry[] retrieveResultBySql(KEY col, String method, String desc) {
		return retrieveResultBySql(col, method, desc, "");
	}
	
	@NonNull
	static ResultEntry[] retrieveResultBySql(KEY col, String method, String desc, String sup) {
		//if (db == null) return new ResultEntry[0];
		String name = DictManager.getSelectingAttr(DictManager.KEY.NAME);
		//String sql = String.format("SELECT * FROM `%s` WHERE `id` IN (SELECT `id` FROM `%s` WHERE %s %s ? LIMIT 200)", name, name, col, method);
		String sql = String.format("SELECT * FROM `%s` WHERE %s %s ? %s LIMIT 100 ", name, col, method, sup);
		Log.i(TAG, "retrieveResultBySql(): " + sql.replace("?", desc));
		try {
			Cursor cursor = db.rawQuery(sql, new String[]{desc});
			ArrayList<ResultEntry> entries = new ArrayList<>(cursor.getCount());
			ResultEntry entry;
			while (cursor.moveToNext()) {
				entry = new ResultEntry();
				entry.dictIndex = DictManager.selectingDictIndex;
				entry.id = cursor.getInt(0);
				entry.term = cursor.getString(1);
				Log.i(TAG, "retrieveResultBySql: id=" + entry.id + ", term="+entry.term);
				entry.supplement = cursor.getString(2);
				entry.class_ = cursor.getString(3);
				entry.affixrule = cursor.getString(4);
				entry.mean = cursor.getString(5);
				entries.add(entry);
			}
			cursor.close();
			ResultEntry[] entries_ = entries.toArray(new ResultEntry[0]);
			switch (Configs.sortRule) {
				case LENGTH:
					Arrays.sort(entries_, (o1, o2) -> {
						int o1ml = o1.mean.length(), o2ml = o2.mean.length();
						int o1tl = o1.term.length(), o2tl = o2.term.length();
						return o1tl!=o2tl ? Integer.compare(o1tl, o2tl) : Integer.compare(o1ml, o2ml);
					});
					break;
				case UNICODE:
					Arrays.sort(entries_, (o1, o2) ->  o1.term.compareTo(o2.term)); break;
				case ID:
					Arrays.sort(entries_, (o1, o2) -> Integer.compare(o1.id, o2.id)); break;
				case ID_REV:
					Arrays.sort(entries_, (o1, o2) -> Integer.compare(o2.id, o1.id)); break;
				case NO:
				default:
					break;
			}
			
			return entries_;
		} catch (SQLException e) {
			Log.e(TAG, e.toString());
		}
		return new ResultEntry[0];
	}
	
	
	
	public static class ResultEntry {
		int id, dictIndex;
		String term, supplement, class_, affixrule, mean; // `affixrule` is un-used
		int termHighlightColor = 0;
		
		public ResultEntry() {}
		public ResultEntry(int dictIndex, String left, String upper, String bottom) {
			this.dictIndex  = dictIndex;
			this.parseLeft(left).parseUpper(upper).parseBottom(bottom);
		}
		public ResultEntry termHighlightColor(int color) { termHighlightColor = color; return this; }
		public ResultEntry id(int id) { this.id = id; return this; }
		public ResultEntry dictIndex(int index) { this.dictIndex = index; return this; }
		public int termHighlightColor() { return termHighlightColor; }
		public int id() { return id; }
		public int dictIndex() { return dictIndex; }
		
		public SpannableString printLeft() {
			SpannableString ss = new SpannableString(term);
			if (!"".equals(term) && (termHighlightColor&0xFF000000)!=0) {
				ss.setSpan(new ForegroundColorSpan(termHighlightColor),
						0, ss.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
			}
			return ss;
		}
		public SpannableString printUpper() {
			SpannableStringBuilder ssb = new SpannableStringBuilder();
			DictManager.ArrayListMap<String> highlight = DictManager.getMap(dictIndex, DictManager.KEY.HIGHLIGHT);
			ssb.append(constructSsbWithHighlight(class_, highlight));
			if (!"".equals(affixrule)) {
				ssb.append(" | ").append(constructSsbWithHighlight(affixrule, highlight));
			}
			return new SpannableString(ssb);
		}
		public SpannableString printBottom() {
			SpannableStringBuilder ssb = new SpannableStringBuilder();
			DictManager.ArrayListMap<String> highlight = DictManager.getMap(dictIndex, DictManager.KEY.HIGHLIGHT);
			ssb.append(constructSsbWithHighlight(mean, highlight));
			if (!"".equals(supplement)) {
				ssb.append("\n");
				ssb.append(constructSsbWithHighlight(supplement, highlight),
						new RelativeSizeSpan(0.8f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			return new SpannableString(ssb);
		}
		
		private SpannableStringBuilder constructSsbWithHighlight(String s, DictManager.ArrayListMap<String> highlight) {
			SpannableStringBuilder ssb = new SpannableStringBuilder();
			int index = 0, beginPosition, endPosition;
			String key, val;
			String[] segs = s.split("(?<=[^\\\\]|^)#");
			ssb.append(segs[0].replace("\\#", "#"));
			for (int j=1; j <segs.length; j++) {
				String seg = segs[j].replace("\\#", "#");
				if (TextUtils.isEmpty(seg)) { continue; }
				index = 0;
				for (; index < highlight.getCount(); index++) {
					if (!highlight.key(index).startsWith("#")) { continue; }
					key = highlight.key(index).substring(1);
					if (seg.startsWith(key)) {
						beginPosition = ssb.length();
						ssb.append(seg.substring(key.length()));
						endPosition = ssb.length();
						if (beginPosition == endPosition) { continue; }
						val = highlight.val(index);
						String boldOrItalic;
						if (val.contains("#")) {
							String[] opts = val.split("#", 2);
							boldOrItalic = opts[0];
							ssb.setSpan(new ForegroundColorSpan(ColorUtil.parseColor(opts[1])),
									beginPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						} else {
							boldOrItalic = val;
						}
						if (boldOrItalic.contains("b") && boldOrItalic.contains("i")) {
							ssb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC),
									beginPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						} else if (boldOrItalic.contains("b")) {
							ssb.setSpan(new StyleSpan(Typeface.BOLD),
									beginPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						} else if (boldOrItalic.contains("i")) {
							ssb.setSpan(new StyleSpan(Typeface.ITALIC),
									beginPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
						break;
					}
				}
				if (index == highlight.getCount()) {
					ssb.append(seg);
				}
			}
			
			for (index = highlight.getCount(); index > 0; index--) {
				String s_ = ssb.toString();
				key = highlight.key(index-1);
				if (key.startsWith("#")) { continue; }
				if (!s_.contains(key)) { continue; }
				beginPosition = -key.length();
				while ((beginPosition = s_.indexOf(key, beginPosition+key.length())) >= 0) {
					//Log.i(TAG, "constructSsbWithHighlight: KEY = " + key + ", POS = " + beginPosition);
					val = highlight.val(index-1);
					String opt1;
					if (val.contains("#")) {
						String[] opts = val.split("#", 2);
						opt1 = opts[0];
						ssb.setSpan(new ForegroundColorSpan(ColorUtil.parseColor(opts[1])),
								beginPosition, beginPosition+key.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					} else {
						opt1 = val;
					}
					if (opt1.contains("b") && opt1.contains("i")) {
						ssb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC),
								beginPosition, beginPosition+key.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					} else if (opt1.contains("b")) {
						ssb.setSpan(new StyleSpan(Typeface.BOLD),
								beginPosition, beginPosition+key.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					} else if (opt1.contains("i")) {
						ssb.setSpan(new StyleSpan(Typeface.ITALIC),
								beginPosition, beginPosition+key.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
			}
			return ssb;
		}
		
		public String dumpLeft() {
			return term;
		}
		public String dumpUpper() {
			return class_;
			//return TextUtils.isEmpty(affixrule) ? class_ : class_+Configs.PARSE_UPPER_SEPARATE_SIGN+affixrule;
		}
		public String dumpBottom() {
			return TextUtils.isEmpty(supplement) ? mean : mean+"\n"+Configs.SEARCH_SEPARATE_SIGN+"\n"+supplement;
		}
		
		public String getAttr(KEY key) {
			switch (key) {
				case TERM: return term;
				case SUP: return supplement;
				case CLASS: return class_;
				case AFFIX: return affixrule;
				case MEAN: return mean;
			}
			return null;
		}
		
		public ResultEntry parseLeft(String left) {
			DictManager.ArrayListMap<String> replaceMap = DictManager.getMap(dictIndex, DictManager.KEY.REPLACE_MAP);
			for (int index = 0; index < replaceMap.getCount(); index++) {
				left = left.replaceAll(replaceMap.key(index), replaceMap.val(index));
			}
			term = left;
			return this;
		}
		public ResultEntry parseUpper(String upper) {
//			if (!upper.contains(Configs.PARSE_UPPER_SEPARATE_SIGN)) {
//				class_ = upper;
//				affixrule = "";
//			} else {
//				String[] tmp = upper.split(Configs.PARSE_UPPER_SEPARATE_SIGN, 2);
//				class_ = tmp[0].trim();
//				affixrule = tmp[1].trim();
//			}
			class_ = upper;
			affixrule = "";
			return this;
		}
		public ResultEntry parseBottom(String bottom) {
			DictManager.ArrayListMap<String> replaceMap = DictManager.getMap(dictIndex, DictManager.KEY.REPLACE_MAP);
			for (int index = 0; index < replaceMap.getCount(); index++) {
				bottom = bottom.replaceAll(replaceMap.key(index), replaceMap.val(index));
			}
			
			if (!bottom.contains(Configs.SEARCH_SEPARATE_SIGN)) {
				mean = bottom;
				supplement = "";
			} else {
				String[] tmp = bottom.split(Configs.SEARCH_SEPARATE_SIGN_REGEX, 2);
				mean = tmp[0].trim();
				supplement = tmp[1].trim();
			}
			return this;
		}
		
		public void updateEntry() {
			assert id>=0 && dictIndex>=0;
			String name = DictManager.getAttr(dictIndex, DictManager.KEY.NAME);
			ContentValues values = new ContentValues();
			values.put(KEY.TERM.toString(), term);
			values.put(KEY.SUP.toString(), supplement);
			values.put(KEY.CLASS.toString(), class_);
			values.put(KEY.AFFIX.toString(), affixrule);
			values.put(KEY.MEAN.toString(), mean);
			db.update(name, values, KEY.ID+"=?", new String[]{String.valueOf(id)});
//			String sql =
//					"UPDATE " + name + " " +
//					"SET " + KEY.TERM + "=?, " + KEY.SUP + "=?, "
//							+ KEY.CLASS + "=?, " + KEY.AFFIX + "=?, " + KEY.MEAN + "=? " +
//					"WHERE " + KEY.ID + "=" + id + ";";
//			db.execSQL(sql, new String[]{term, supplement, class_, affixrule, mean});
		}
		public void addEntry() {
			assert dictIndex>=0;
			String name = DictManager.getAttr(dictIndex, DictManager.KEY.NAME);
			ContentValues values = new ContentValues();
			values.put(KEY.TERM.toString(), term);
			values.put(KEY.SUP.toString(), supplement);
			values.put(KEY.CLASS.toString(), class_);
			values.put(KEY.AFFIX.toString(), affixrule);
			values.put(KEY.MEAN.toString(), mean);
//			db.insert(name, null, values); // ??????
			String sql =
					"INSERT INTO `" + name + "` " +
//					"SET " + KEY.ID + "=null, " + KEY.TERM + "=?, " + KEY.SUP + "=?, "
//					+ KEY.CLASS + "=?, " + KEY.AFFIX + "=?, " + KEY.MEAN + "=? ";
					"VALUES (null, ?, ?, ?, ?, ?);";
			db.execSQL(sql, new String[]{term, supplement, class_, affixrule, mean});
		}
		public void deleteEntry() {
			String name = DictManager.getAttr(dictIndex, DictManager.KEY.NAME);
			db.delete(name, KEY.ID+"=?", new String[]{String.valueOf(id)});
//			String sql =
//					"DELETE FROM " + name + " " +
//					"WHERE " + KEY.ID + "=" + id + ";";
//			db.execSQL(sql);
		}
	}
}
