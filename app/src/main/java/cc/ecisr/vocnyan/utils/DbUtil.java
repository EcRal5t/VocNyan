package cc.ecisr.vocnyan.utils;

import static cc.ecisr.vocnyan.struct.DictManager.DB_TABLE_INFO;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Arrays;


public class DbUtil extends SQLiteOpenHelper {
	private static final String TAG = "`DbUtil";
	
	//声明数据库的实例
	private static SQLiteDatabase db = null;
	
	public static final String DB_INNER_FILE = "dict.db";
	
	public static final String LIKE = "LIKE";
	public static final String REG = "REGEXP";
	
	//static boolean updateNeeded = false;
	
	public DbUtil(@Nullable Context context, int version) {
		super(context, DB_INNER_FILE, null, version);
		db = this.getWritableDatabase();
		
		Cursor c = db.query("_info_", null, null, null, null, null, null);
		Log.i(TAG, "DbUtil: " + Arrays.toString(c.getColumnNames()));
		while (c.moveToNext()) {
			Log.i(TAG, "DbUtil: " + c.getString(0) + ", " + c.getString(1)
					+ ", " + c.getString(2) + ", " + c.getString(3)
					+ ", "+ c.getString(4) + ", " + c.getString(5)
					+ ", " + c.getString(6) + ", " + c.getString(7) + ", ");
		}
		c.close();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(TAG, "onCreate, " + db.getVersion());
		db.execSQL("CREATE TABLE IF NOT EXISTS `" + DB_TABLE_INFO + "` ( " +
				"dict_name VARCHAR PRIMARY KEY, " +
				"replace_map VARCHAR, " +
				"ellipsis_map VARCHAR, " +
				"font_path VARCHAR, " +
				"highlight_word VARCHAR, " +
				"dict_note VARCHAR, " +
				"editable VARCHAR, " +
				"version TEXT (6));");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "onUpgrade: " + oldVersion + " -> " + newVersion);
		//updateNeeded = true;
	}
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "onUpgrade: " + oldVersion + " -> " + newVersion);
		//updateNeeded = true;
	}
	
	public static SQLiteDatabase getDb() {
		if (db == null) {
			Log.e(TAG, "trying to get NULL db");
		}
		return db;
	}
	
	
	//关闭数据库的读连接
	public static void closeLink() {
		if (db != null && db.isOpen()) {
			db.close();
			db = null;
		}
	}
	
	

}
