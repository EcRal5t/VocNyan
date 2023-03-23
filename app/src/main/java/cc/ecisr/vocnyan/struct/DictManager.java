package cc.ecisr.vocnyan.struct;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cc.ecisr.vocnyan.utils.ColorUtil;
import cc.ecisr.vocnyan.utils.DbUtil;
import cc.ecisr.vocnyan.utils.FileUtil;
import cc.ecisr.vocnyan.utils.FontUtil;

public class DictManager {
	private static final String TAG = "`DictManager";
	
	public static SQLiteDatabase db = null;
	public static final String DB_TABLE_INFO = "_info_";
	public static ArrayList<String> dictsName;
	static HashMap<String, Dict> dicts;
	
	public static int selectingDictIndex = 0;
	
	public enum STATUS {
		SUCCESS, ERR_INVALID_FILE, ERR_KEY_CONFLICT, ERR_UNALLOWED_ACTION, ERR_INVALID_FORMAT,
		ERR_EMPTY_INPUT, ERR_KEY_FORMAT, ERR_UNKNOWN, ERR_NO_PERMISSION
	}
	
	static public void initialize() {
		db = DbUtil.getDb();
		retrieveDicts();
	}
	/* Allow `i` in [0, dictCount()-1] | {-1} */
	public static void setSelectingDictIndex(int i) {
		selectingDictIndex = i==-1 ? dictCount()-1 : (i>=dictCount() ? 0 : i);
	}
	
	static public void retrieveDicts() {
		Cursor cursor = db.query(DB_TABLE_INFO, null, null, null, null, null, null, null);
		int dictCount = cursor.getCount();
		Log.i(TAG, "retrieveDicts: " + dictCount);
		dicts = new HashMap<>(dictCount);
		dictsName = new ArrayList<>(dictCount);
		if (dictCount==0 || cursor.getColumnCount()!=8) return;
		Dict dict;
		while (cursor.moveToNext()) {
			dict = new Dict();
			dict.setName(cursor.getString(0));
			dict.parseReplaceMap(cursor.getString(1));
			dict.parseEllipsisMap(cursor.getString(2));
			dict.parseFontPath(cursor.getString(3));
			dict.parseHighlightWord(cursor.getString(4));
			dict.setNote(cursor.getString(5));
			dict.setEditable(cursor.getString(6));
			dictsName.add(dict.name);
			dicts.put(dict.name, dict);
		}
		cursor.close();
		if (selectingDictIndex >= dictCount()) {
			selectingDictIndex = dictCount() - 1;
		}
	}
	
	static public STATUS addDict(String name) {
		if (name.matches("\\d+")) { return STATUS.ERR_KEY_FORMAT; }
		Cursor cursor = db.query(DB_TABLE_INFO, null, "dict_name=?", new String[]{name}, null, null, null);
		if (cursor.getCount()>0) { return STATUS.ERR_KEY_CONFLICT; }
		cursor.close();
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd", Locale.CHINA);
		String date = sdf.format(new Date());
		
		ContentValues insertInfo = new ContentValues();
		insertInfo.put("dict_name", name);
		insertInfo.put("editable", 1);
		insertInfo.put("version", date);
		db.insert(DB_TABLE_INFO, null, insertInfo);
		db.execSQL("DROP TABLE IF EXISTS `" + name + "`;");
		db.execSQL("CREATE TABLE `" + name + "` ( \n" +
				"    id         INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE,\n" +
				"    term       VARCHAR,\n" +
				"    supplement VARCHAR,\n" +
				"    class      VARCHAR,\n" +
				"    affixrule  VARCHAR,\n" +
				"    meanings   VARCHAR );");
		selectingDictIndex = dictCount();
		return STATUS.SUCCESS;
	}
	
	static public STATUS deleteDict(String name) {
		db.delete(DB_TABLE_INFO, "`dict_name`=?", new String[]{name});
		db.execSQL("DROP TABLE IF EXISTS `" + name + "`;");
		//retrieveDicts();
		return STATUS.SUCCESS;
	}
	
	static public STATUS importDicts(String path) {
		if (!FileUtil.isFile(path)) { return STATUS.ERR_INVALID_FILE; }
		Log.i(TAG, "importDicts: " + path);
		SQLiteDatabase dbExtra;
		ArrayList<String> sheetNames = new ArrayList<>();
		try {
			dbExtra = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
			Cursor c1 = dbExtra.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
			while (c1.moveToNext()) { sheetNames.add(c1.getString(0)); }
			c1.close();
		} catch (SQLiteDatabaseCorruptException e) {
			return STATUS.ERR_INVALID_FORMAT;
		} catch (SQLiteCantOpenDatabaseException e) {
			return STATUS.ERR_NO_PERMISSION;
		}
		if (!sheetNames.contains(DB_TABLE_INFO)) { return STATUS.ERR_INVALID_FORMAT; }
		Cursor c2 = dbExtra.query(DB_TABLE_INFO, null,null,null,null,null,null,null);
		ArrayList<String> columnNames = new ArrayList<>(Arrays.asList(c2.getColumnNames()));
		String[] requiredCols = new String[]{"dict_name", "replace_map", "ellipsis_map", "font_path", "highlight_word", "dict_note", "editable", "version"};
		for (String requireCol: requiredCols) { if (!columnNames.contains(requireCol)) { return STATUS.ERR_INVALID_FORMAT; } }
		while (c2.moveToNext()) { if (!sheetNames.contains(c2.getString(0))) { return STATUS.ERR_INVALID_FORMAT; } }
		while (c2.moveToPrevious()) { if (dictsName.contains(c2.getString(0))) { deleteDict(c2.getString(0)); } }
		c2.close();
		
		db.execSQL("ATTACH DATABASE '" + path + "' AS candidate;");
		db.execSQL(
				"INSERT INTO " + DB_TABLE_INFO + " (dict_name, replace_map, ellipsis_map, font_path, highlight_word, dict_note, editable, version) " +
				"SELECT dict_name, replace_map, ellipsis_map, font_path, highlight_word, dict_note, editable, version " +
				"FROM candidate." + DB_TABLE_INFO +";");
		
		try {
			Cursor cursor = dbExtra.query(DB_TABLE_INFO, null, null, null, null, null, null, null);
			String dictName;
			while (cursor.moveToNext()) {
				dictName = cursor.getString(0);
//			db.execSQL("CREATE TABLE " + dictName + " AS SELECT * FROM candidate." + dictName + ";");
				//db.execSQL("DROP TABLE IF EXISTS `" + dictName + "`;");
				db.execSQL("CREATE TABLE `" + dictName + "` (" +
						"    id         INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE,\n" +
						"    term       VARCHAR,\n" +
						"    supplement VARCHAR,\n" +
						"    class      VARCHAR,\n" +
						"    affixrule  VARCHAR,\n" +
						"    meanings   VARCHAR );");
				db.execSQL(
						"INSERT INTO `" + dictName + "` \n" +
						"SELECT * \n" +
						"FROM candidate." + dictName + ";");
//			//db.execSQL("ATTACH DATABASE '" + path + "' AS candidate;");
//			db.execSQL(
//					"INSERT INTO " + dictName + " (id, term, supplement, class, affixrule, meanings) " +
//					"SELECT id, term, supplement, class, affixrule, meanings " +
//					"FROM `candidate." + dictName +"`;");
//			//db.execSQL("DETACH DATABASE candidate;");
			}
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
			db.execSQL("DETACH DATABASE candidate;");
			return STATUS.ERR_UNKNOWN;
		}
		db.execSQL("DETACH DATABASE candidate;");
		return STATUS.SUCCESS;
//		SQLiteDatabase dbExtra = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
//		Cursor cursor;
//		cursor = dbExtra.query(DB_TABLE_INFO, null, null, null, null, null, null, null);
//		if (cursor.getCount()==0 || cursor.getColumnCount()!=8) return false; // 8: 七列基本属性+数据库版本号
//		ArrayList<String> dictsName = new ArrayList<>(cursor.getCount());
//		String sqlInsert;
//		boolean allInsertionSuccess = true;
//		while (cursor.moveToNext()) {
//			if (cursor.getInt(7) < db.getVersion()) {
//				allInsertionSuccess = false;
//				continue;
//			}
//			sqlInsert = String.format(Locale.CHINA,
//					"INSERT INTO " + DB_TABLE_INFO + " (" +
//					"dict_name, replace_map, ellipsis_map, font_path, highlight_word, dict_note, editable" +
//					") VALUES ('%s', '%s', '%s', '%s', '%s', '%s', %d)",
//					cursor.getString(0),
//					cursor.getString(1),
//					cursor.getString(2),
//					cursor.getString(3),
//					cursor.getString(4),
//					cursor.getString(5),
//					cursor.getInt(6)
//			);
//			db.execSQL(sqlInsert);
//			dictsName.add(cursor.getString(0));
//		}
//		cursor.close();
//		String sqlMoveTable;
//		for (String dictName: dictsName) { }
//		String dropSql = "";
//		return allInsertionSuccess;
	}
	
	static public STATUS exportDicts(String path) {
		if (FileUtil.isDir(path)) { return STATUS.ERR_INVALID_FILE; }
		FileUtil.deleteFileIfExist(path);
		FileUtil.copy(db.getPath(), path);
		return STATUS.SUCCESS;
	}
	
	static public int dictCount() {
		return dictsName.size();
	}
	
	public enum KEY {
		NAME("dict_name"), VERSION("version"),
		REPLACE_MAP("replace_map"), ELLIPSIS_MAP("ellipsis_map"), FONT_PATH("font_path"),
		HIGHLIGHT("highlight_word"), NOTE("dict_note"), EDITABLE("editable");
		private final String key;
		KEY(String key) { this.key = key; }
		@NonNull public String toString() { return this.key; }
	}

	static public STATUS updateDict(KEY key, String val) {
		String name = dictsName.get(selectingDictIndex);
		Dict dict = dicts.get(name);
		assert dict != null;
		if ((key!=KEY.EDITABLE && key!=KEY.FONT_PATH) && !"1".equals(dict.editable)) {
			return STATUS.ERR_UNALLOWED_ACTION;
		}
		switch (key) {
			case REPLACE_MAP:
				dict.parseReplaceMap(val); break;
			case ELLIPSIS_MAP:
				dict.parseEllipsisMap(val); break;
			case FONT_PATH:
				dict.parseFontPath(val); break;
			case HIGHLIGHT:
				dict.parseHighlightWord(val); break;
			case NOTE:
				dict.note = val; break;
			case EDITABLE:
				dict.editable = val; break;
			case VERSION:
				assert false;
			case NAME:
				Cursor cursor = db.query(DB_TABLE_INFO, null, "dict_name=?", new String[]{val}, null, null, null);
				if (cursor.getCount()>0) {
					cursor.close();
					return STATUS.ERR_KEY_CONFLICT;
				}
				cursor.close();
				//剩下的交给 Dict.updateDict() 处理
			default:
				break;
		}
		dict.updateDict(key, val);
		return STATUS.SUCCESS;
	}
	
	public static String getAttr(String name, KEY key) {
		Dict dict = dicts.get(name);
		assert dict != null;
		switch (key) {
			case NAME: return dict.name;
			case REPLACE_MAP: return dict.replaceMapRaw;
			case ELLIPSIS_MAP: return dict.ellipsisMapRaw;
			case FONT_PATH: return dict.fontPathRaw;
			case HIGHLIGHT: return dict.highlightWordRaw;
			case NOTE: return dict.note;
			case EDITABLE: return dict.editable;
		}
		return null;
	}
	public static String getAttr(int i, KEY key) { return getAttr(dictsName.get(i), key); }
	public static String getSelectingAttr(KEY key) { return getAttr(selectingDictIndex, key); }
	
	public static ArrayListMap<String> getMap(String name, KEY key) {
		Dict dict = dicts.get(name);
		assert dict != null;
		switch (key) {
			case REPLACE_MAP: return dict.map.replaceMap;
			case ELLIPSIS_MAP: return dict.map.ellipsisMap;
			case HIGHLIGHT: return dict.map.highlightWord;
		}
		return null;
	}
	public static ArrayListMap<String> getMap(int i, KEY key) { return getMap(dictsName.get(i), key); }
	public static ArrayListMap<String> getSelectingMap(KEY key) { return getMap(selectingDictIndex, key); }
	
	public static Typeface getFont(String name) {
		Dict dict = dicts.get(name);
		assert dict != null;
		if (TextUtils.isEmpty(dict.fontPathRaw)) { return null; }
		if (dict.font==null) { dict.parseFonts(); }
		return dict.font.getFont();
	}
	public static Typeface getFont(int i) { return getFont(dictsName.get(i)); }
	public static Typeface getSelectingFont() { return getFont(selectingDictIndex); }
	
	static class Dict {
		SettingsMap map = new SettingsMap();
		String editable;
		String name, note;
		String fontPathRaw;
		String replaceMapRaw, ellipsisMapRaw, highlightWordRaw;
		FontUtil.Fonts font = null;
		
		static class SettingsMap {
			ArrayListMap<String> replaceMap = new ArrayListMap<>();
			ArrayListMap<String> ellipsisMap = new ArrayListMap<>();
			ArrayListMap<String> highlightWord = new ArrayListMap<>();
		}
		
		void setName(String str) {
			name = str==null ? "" : str;
		}
		void setNote(String str) {
			note = str==null ? "" : str;
		}
		void setEditable(String str) {
			editable = str==null ? "" : str;
		}
		void parseReplaceMap(String str) {
			str = str==null ? "" : str;
			replaceMapRaw = str;
			map.replaceMap = parseMap(str, s -> s.contains(","));
		}
		void parseEllipsisMap(String str) {
			str = str==null ? "" : str;
			ellipsisMapRaw = str;
			map.ellipsisMap = parseMap(str, s -> s.contains(","));
		}
		void parseHighlightWord(String str) {
			str = str==null ? "" : str;
			highlightWordRaw = str;
			map.highlightWord = parseMap(str, s -> s.matches("#?[^#]+(,(bi?|ib)?(#[0-9A-Fa-f]{6})?)?"));
			int s = 0;
			for (int i=0; i<map.highlightWord.getCount(); i++) {
				if (TextUtils.isEmpty(map.highlightWord.val(i))) { s++; }
			}
			int ith = 0;
			for (int i=0; i<map.highlightWord.getCount(); i++) {
				if (TextUtils.isEmpty(map.highlightWord.val(i))) {
					map.highlightWord.val.set(i, ColorUtil.ithColorInHsv(++ith, s));
				}
			}
		}
		void parseFontPath(String str) {
			str = str==null?"":str;
			fontPathRaw = str;
		}
		void parseFonts() {
			String[] paths = fontPathRaw.split(";");
			ArrayList<File> files = new ArrayList<>(paths.length);
			for (String path: paths) {
				if (TextUtils.isEmpty(path)) { continue; }
				File file = new File(path);
				if (!file.isFile()) { continue; }
				files.add(file);
			}
			font = new FontUtil.Fonts(files);
		}
		
		
		private interface Validator {
			boolean validate(String s);
		}
		private static ArrayListMap<String> parseMap(String str, Validator validator) {
			ArrayListMap<String> result = new ArrayListMap<>();
			String[] lines = str.split("\r?\n");
			for (String line: lines) {
				if ("".equals(line)) continue;
				if (line.startsWith("//")) continue;
				//Log.i(TAG, "parseMap, line = " + line + ", VALIDATION: " + validator.validate(line));
				if (!validator.validate(line)) { continue; }
				
				try {
					if (!line.contains(",")) {
						Pattern.compile(line.trim());
						result.add(line.trim(), "");
					} else {
						String[] match = line.split(",", 2);
						Pattern.compile(match[0].trim());
						result.add(match[0].trim(), match[1].trim());
					}
				} catch (PatternSyntaxException ignored) {
					Log.w(TAG, "parseMap: invalid pattern: " + line);
				}
			}
			return result;
		}
		
		void updateDict(KEY key, String val) {
			switch (key) {
				case REPLACE_MAP:
				case ELLIPSIS_MAP:
				case FONT_PATH:
				case HIGHLIGHT:
				case NOTE:
				case EDITABLE:
				case NAME:
					String sql = "UPDATE " + DB_TABLE_INFO + " SET " + key + "=? WHERE `dict_name`=?;";
					Log.i(TAG, "updateDict, sql = \n" + sql + ", var = " + val + " " + name);
					db.execSQL(sql, new String[]{val, name});
					break;
				default:
					assert false;
			}
			if (key == KEY.NAME) {
				db.execSQL("ALTER TABLE `" + name + "` RENAME TO `" + val + "`;");
				name = val;
			}
		}
	}
	
	
	static class ArrayListMap<T> {
		ArrayList<T> key = new ArrayList<>();
		ArrayList<T> val = new ArrayList<>();
		//HashMap<Integer, Map.Entry<T, T>> index = new HashMap<>();
		
		void add(T k, T v) {
			key.add(k);
			val.add(v);
		}
		void clear() {
			key.clear();
			val.clear();
		}
		T key(int index) { return key.get(index); }
		T val(int index) { return val.get(index); }
		Map.Entry<T, T> get(int i) {
			return new AbstractMap.SimpleEntry<>(key.get(i), val.get(i));
		}
		int getCount() {
			return key.size();
		}
	}
}
