package cc.ecisr.vocnyan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;

import cc.ecisr.vocnyan.struct.Configs;
import cc.ecisr.vocnyan.struct.DictManager;
import cc.ecisr.vocnyan.utils.ToastUtil;

public class SettingActivity extends AppCompatActivity {
	private static final String TAG = "`SettingActivity";
	
	Button btnAddDict, btnImportDict, btnExportDict, btnSelectDict;
	Button btnReplaceMap, btnEllipsisMap, btnSetFontPath, btnHighlightField;
	Button btnDisplayNote, btnDeleteDict;
	Button btnInfo;
	SwitchCompat switchDictEditable, switchSortRule;
	SharedPreferences sp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		btnAddDict = findViewById(R.id.btn_add_dict);
		btnImportDict = findViewById(R.id.btn_import_dict);
		btnExportDict = findViewById(R.id.btn_export_dict);
		btnSelectDict = findViewById(R.id.btn_select_dict);
		btnReplaceMap = findViewById(R.id.btn_replace_map);
		btnEllipsisMap = findViewById(R.id.btn_ellipsis_map);
		btnSetFontPath = findViewById(R.id.btn_font_path);
		btnHighlightField = findViewById(R.id.btn_highlight_field);
		btnDisplayNote = findViewById(R.id.btn_display_note);
		btnDeleteDict = findViewById(R.id.btn_delete_dict);
		switchDictEditable = findViewById(R.id.switch_dict_editable);
		switchSortRule = findViewById(R.id.switch_sort_rule);
		
		btnAddDict.setOnClickListener(view -> settingBtnListener(view.getId()));
		btnImportDict.setOnClickListener(view -> settingBtnListener(view.getId()));
		btnExportDict.setOnClickListener(view -> settingBtnListener(view.getId()));
		btnSelectDict.setOnClickListener(view -> settingBtnListener(view.getId()));
		btnSelectDict.setOnLongClickListener(view -> settingBtnLongClickListener(view.getId()));
		btnReplaceMap.setOnClickListener(view -> settingBtnListener(view.getId()));
		btnEllipsisMap.setOnClickListener(view -> settingBtnListener(view.getId()));
		btnSetFontPath.setOnClickListener(view -> settingBtnListener(view.getId()));
		btnHighlightField.setOnClickListener(view -> settingBtnListener(view.getId()));
		btnDisplayNote.setOnClickListener(view -> settingBtnListener(view.getId()));
		btnDeleteDict.setOnClickListener(view -> settingBtnListener(view.getId()));
		
		switchDictEditable.setOnCheckedChangeListener((compoundButton, b) -> {
			if (compoundButton.isPressed()) {
				DictManager.updateDict(DictManager.KEY.EDITABLE, b ? "1" : "0");
			}
		});
		switchSortRule.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				Configs.sortRule = Configs.SORT_RULE.LENGTH;
				switchSortRule.setText(getResources().getText(R.string.dict_manager_sort_rule_length));
			} else {
				Configs.sortRule = Configs.SORT_RULE.UNICODE;
				switchSortRule.setText(getResources().getText(R.string.dict_manager_sort_rule_unicode));
			}
			if (compoundButton.isPressed()) {
				sp.edit().putBoolean("settings_sort_rule", b).apply();
			}
		});
		switchSortRule.setChecked(sp.getBoolean("settings_sort_rule", true));
		//switchSortRule.setOnLongClickListener(view -> settingBtnLongClickListener(view.getId()));
		if (DictManager.dictCount() > 0) {
			switchDictEditable.setChecked("1".equals(DictManager.getSelectingAttr(DictManager.KEY.EDITABLE)));
		}
		
		btnInfo = findViewById(R.id.btn_info);
		btnInfo.setText(getString(R.string.dict_manager_btn_info, getString(R.string.app_version_main)));
		setSelectDictView();
	}
	
	boolean settingBtnLongClickListener(int viewId) {
		View v = LayoutInflater.from(SettingActivity.this).inflate(R.layout.layout_alertdialog_with_input, null);
		AlertDialog.Builder alertWithInput = new AlertDialog.Builder(this)
				.setView(v)
				.setNegativeButton(R.string.button_cancel, null);
		final TextView vTips = v.findViewById(R.id.alertdialog_tips);
		final EditText vInput = v.findViewById(R.id.alertdialog_input);
		
		if (viewId == R.id.btn_select_dict) {
			vTips.setText("更改词典名称");
			vInput.setText(DictManager.getSelectingAttr(DictManager.KEY.NAME));
			alertWithInput.setTitle(getResources().getString(R.string.dict_manager_rename_dict_with_entry, DictManager.getSelectingAttr(DictManager.KEY.NAME)))
					.setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
						if (!TextUtils.isEmpty(vInput.getText().toString())) {
							DictManager.STATUS resultStatus = DictManager.updateDict(DictManager.KEY.NAME,  vInput.getText().toString());
							judgeSettingResult(resultStatus);
						} else {
							judgeSettingResult(DictManager.STATUS.ERR_EMPTY_INPUT);
						}
					});
			alertWithInput.create().show();
		} else if (viewId == R.id.switch_sort_rule) {
			vTips.setText("排序时，排除的字符\n以正则表达式表示\n留空表示不启用");
			vInput.setText(sp.getString("settings_sort_rule_exception", ""));
			alertWithInput.setTitle(getResources().getString(R.string.dict_manager_sort_rule_exception))
					.setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
					
					});
			alertWithInput.create().show();
		}
		return false;
	}
	
	void settingBtnListener(int viewId) {
		View v = LayoutInflater.from(SettingActivity.this).inflate(R.layout.layout_alertdialog_with_input, null);
		AlertDialog.Builder alertWithInput = new AlertDialog.Builder(this)
				.setView(v)
				.setNegativeButton(R.string.button_cancel, null);
		final TextView vTips = v.findViewById(R.id.alertdialog_tips);
		final EditText vInput = v.findViewById(R.id.alertdialog_input);
		
		if (viewId == R.id.btn_add_dict) {
			vTips.setText("输入词典名称");
			alertWithInput.setTitle(R.string.dict_manager_select_dict)
					.setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
						if (!TextUtils.isEmpty(vInput.getText().toString())) {
							DictManager.STATUS resultStatus = DictManager.addDict(vInput.getText().toString());
							judgeSettingResult(resultStatus);
						} else {
							judgeSettingResult(DictManager.STATUS.ERR_EMPTY_INPUT);
						}
					});
			alertWithInput.create().show();
		} else if (viewId == R.id.btn_import_dict) {
			if (requirePermissionIfNotProvided()) {
				vTips.setText("词典导入绝对路径\n必须是.db文件，且格式必须严格符合说明页中的要求");
				vInput.setText(sp.getString("settings_import_path", Environment.getExternalStorageDirectory().getPath()));
				alertWithInput.setTitle(R.string.dict_manager_add_dict)
						.setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
							String input = vInput.getText().toString();
							if (!TextUtils.isEmpty(input)) {
								DictManager.STATUS resultStatus = DictManager.importDicts(input);
								sp.edit().putString("settings_import_path", input).apply();
								judgeSettingResult(resultStatus);
							} else {
								judgeSettingResult(DictManager.STATUS.ERR_EMPTY_INPUT);
							}
						});
				alertWithInput.create().show();
			}
		} else if (viewId == R.id.btn_export_dict) {
			if (requirePermissionIfNotProvided()) {
				vTips.setText("词典导出绝对路径");
				vInput.setText(sp.getString("settings_export_path", Environment.getExternalStorageDirectory().getPath()));
				alertWithInput.setTitle(R.string.dict_manager_add_dict)
						.setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
							String input = vInput.getText().toString();
							if (!TextUtils.isEmpty(input)) {
								DictManager.STATUS resultStatus = DictManager.exportDicts(input);
								sp.edit().putString("settings_export_path", input).apply();
								judgeSettingResult(resultStatus);
							} else {
								judgeSettingResult(DictManager.STATUS.ERR_EMPTY_INPUT);
							}
						});
				alertWithInput.create().show();
			}
		} else if (viewId == R.id.btn_select_dict) {
			String[] dictsName = DictManager.dictsName.toArray(new String[0]);
			new AlertDialog.Builder(this)
					.setTitle(R.string.dict_manager_select_dict)
					.setSingleChoiceItems(dictsName, DictManager.selectingDictIndex, (dialogInterface, i) -> {
						sp.edit().putInt("selecting_dict_index", i).apply();
						DictManager.setSelectingDictIndex(i);
						switchDictEditable.setChecked("1".equals(DictManager.getSelectingAttr(DictManager.KEY.EDITABLE)));
						setSelectDictView();
					})
					.create().show();
		} else if (viewId==R.id.btn_replace_map
				|| viewId==R.id.btn_ellipsis_map
				|| viewId==R.id.btn_font_path
				|| viewId==R.id.btn_highlight_field
				|| viewId==R.id.btn_display_note
		) {
			String tips;
			DictManager.KEY key;
			int titleResource;
			if (viewId==R.id.btn_replace_map) {
				tips = "每行一对，使用半角逗号「,」隔开\n以「//」作行首将忽略该行";
				key = DictManager.KEY.REPLACE_MAP;
				titleResource = R.string.dict_manager_substitute_map;
			} else if (viewId==R.id.btn_ellipsis_map) {
				tips = "每行一对，使用半角逗号「,」隔开\n以「//」作行首将忽略该行";
				key = DictManager.KEY.ELLIPSIS_MAP;
				titleResource = R.string.dict_manager_ellipsis_map;
			} else if (viewId==R.id.btn_font_path) {
				tips = "字体的绝对路径，不使用则留空\nfallback 字体则以半角分号「;」隔开补充于后\n顺序等同 fallback 顺序";
				key = DictManager.KEY.FONT_PATH;
				titleResource = R.string.dict_manager_font_path;
			} else if (viewId==R.id.btn_highlight_field) {
				tips = "每行一个关键字\n可指定颜色（「#000000」）等，使用半角逗号「,」与关键字隔开\n以「//」作行首将忽略该行\n"
					+ "以「#」为首的关键字其着色将持续到下一个「#」出现";
				key = DictManager.KEY.HIGHLIGHT;
				titleResource = R.string.dict_manager_highlight_field;
			} else {
				tips = "";
				key = DictManager.KEY.NOTE;
				titleResource = R.string.dict_manager_dict_note;
			}
			vTips.setText(tips);
			vInput.setText(DictManager.getSelectingAttr(key));
			Log.i(TAG, "DictManager.getSelectingAttr(key): [ " + DictManager.getSelectingAttr(key) + " ], KEY=" + key);
			alertWithInput.setTitle(titleResource)
					.setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
						DictManager.STATUS resultStatus = DictManager.updateDict(key, vInput.getText().toString());
						judgeSettingResult(resultStatus);
					});
			alertWithInput.create().show();
		} else if (viewId == R.id.btn_delete_dict) {
			new AlertDialog.Builder(SettingActivity.this)
					.setTitle(R.string.dict_manager_delete_dict)
					.setMessage("真的要删除吗？此操作不可撤回")
					.setPositiveButton(R.string.button_confirm, (dialogInterface, i) ->
							new AlertDialog.Builder(SettingActivity.this)
							.setTitle(R.string.dict_manager_delete_dict)
							.setMessage("真的要删除吗？此操作不可撤回")
							.setNeutralButton(R.string.button_confirm, (dialogInterface1, i1) ->
								new AlertDialog.Builder(SettingActivity.this)
										.setTitle(R.string.dict_manager_delete_dict)
										.setMessage("真的要删除吗？此操作不可撤回")
										.setPositiveButton(R.string.button_confirm, (dialogInterface2, i2) ->
												judgeSettingResult(DictManager.deleteDict(
														DictManager.getSelectingAttr(DictManager.KEY.NAME))
												))
										.setNegativeButton(R.string.button_cancel, null)
										.create().show())
							.setNegativeButton(R.string.button_cancel, null)
							.create().show())
					.setNegativeButton(R.string.button_cancel, null)
					.create().show();
		}
	}
	
	void judgeSettingResult(DictManager.STATUS status) {
		int tips;
		switch (status) {
			case SUCCESS:              tips = R.string.action_tips_success; break;
			case ERR_INVALID_FILE:     tips = R.string.action_tips_err_invalid_file; break;
			case ERR_INVALID_FORMAT:   tips = R.string.action_tips_err_invalid_format; break;
			case ERR_KEY_CONFLICT:     tips = R.string.action_tips_err_key_conflict; break;
			case ERR_UNALLOWED_ACTION: tips = R.string.action_tips_err_unallowed_action; break;
			case ERR_EMPTY_INPUT:      tips = R.string.action_tips_err_empty_input; break;
			case ERR_KEY_FORMAT:       tips = R.string.action_tips_err_key_format; break;
			case ERR_UNKNOWN:          tips = R.string.action_tips_err_unknown; break;
			default: tips = 0; assert false;
		}
		ToastUtil.msg(this, getString(tips));
		if (status == DictManager.STATUS.SUCCESS) {
			DictManager.retrieveDicts();
			setSelectDictView();
		}
	}
	
	
	void setSelectDictView() {
		Button[] buttons = new Button[]{
				btnSelectDict, btnReplaceMap, btnEllipsisMap,
				btnSetFontPath, btnHighlightField, btnDisplayNote, btnDeleteDict };
		if (DictManager.dictCount() > 0) {
			for (Button btn: buttons) { btn.setEnabled(true); }
			switchDictEditable.setEnabled(true);
			btnSelectDict.setText(getString(R.string.dict_manager_select_dict_with_entry, DictManager.getSelectingAttr(DictManager.KEY.NAME)));
		} else {
			for (Button btn: buttons) { btn.setEnabled(false); }
			switchDictEditable.setEnabled(false);
			btnSelectDict.setText(getString(R.string.dict_manager_select_dict));
		}
	}
	
	
	String[] requiredPermission = new String[]{
			//Manifest.permission.READ_EXTERNAL_STORAGE,
			//Manifest.permission.WRITE_EXTERNAL_STORAGE,
			//Permission.READ_EXTERNAL_STORAGE,
			//Permission.WRITE_EXTERNAL_STORAGE,
			Permission.MANAGE_EXTERNAL_STORAGE,
	};
	boolean requirePermissionIfNotProvided() {
		Activity context = this;
		if (!XXPermissions.isGranted(this, requiredPermission)) {
			XXPermissions.with(this)
					.permission(requiredPermission)
					.request(new OnPermissionCallback() {
						@Override public void onGranted(List<String> permissions, boolean all) {}
						@Override public void onDenied(List<String> permissions, boolean never) {
							OnPermissionCallback.super.onDenied(permissions, never);
							ToastUtil.msg(context, context.getString(R.string.setting_not_provide_permission));
						}
					});
			if (XXPermissions.isPermanentDenied(context, requiredPermission)) {
				XXPermissions.startPermissionActivity(context, requiredPermission);
			}
		}
		return XXPermissions.isGranted(context, requiredPermission);
	}
	
	
}