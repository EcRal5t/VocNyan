package cc.ecisr.vocnyan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import cc.ecisr.vocnyan.struct.DictManager;
import cc.ecisr.vocnyan.struct.ResultEntryManager;
import cc.ecisr.vocnyan.utils.DbUtil;
import cc.ecisr.vocnyan.utils.ToastUtil;


public class MainActivity extends AppCompatActivity {
	private static final String TAG = "`MainActivity";
	
	EditText inputEditText;
	Button btnQueryClear;
	ResultFragment resultFragment;
	Spinner spnSelectDict;
	FloatingActionButton btnAddEntry;
	SharedPreferences sp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			resultFragment = new ResultFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.result_fragment, resultFragment).commit();
		} else {
			resultFragment = (ResultFragment) getSupportFragmentManager().getFragment(savedInstanceState, "result_fragment");
		}
		
		/* initialize inner data */
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		new DbUtil(this, Integer.parseInt(getString(R.string.app_version_database)));
		DictManager.initialize();
		DictManager.setSelectingDictIndex(sp.getInt("selecting_dict_index", 0));
		ResultEntryManager.initialize();
		
		inputEditText = findViewById(R.id.edit_text_input);
		btnQueryClear = findViewById(R.id.btn_clear);
		spnSelectDict = findViewById(R.id.spinner_select);
		btnAddEntry = findViewById(R.id.btn_add_entry);
		inputEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if ("".equals(s.toString())) {
					btnQueryClear.setVisibility(View.GONE);
				} else {
					btnQueryClear.setVisibility(View.VISIBLE);
					search(s.toString());
				}
			}
			@Override
			public void afterTextChanged(Editable s) { }
			
		});
		btnQueryClear.setVisibility(View.GONE);
		btnQueryClear.setOnClickListener(v -> inputEditText.setText(""));
		setInterfaceStatus();
		btnAddEntry.setOnClickListener(view -> executeAddAlert());
		spnSelectDict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
				if (DictManager.dictCount() > 0) {
					DictManager.selectingDictIndex = i;
					btnAddEntry.setVisibility("1".equals(DictManager.getSelectingAttr(DictManager.KEY.EDITABLE)) ? View.VISIBLE : View.GONE);
					sp.edit().putInt("selecting_dict_index", i).apply();
					search(inputEditText.getText().toString());
				} else {
					ToastUtil.msg(MainActivity.this, getString(R.string.tips_empty_dict_storage));
				}
			}
			@Override public void onNothingSelected(AdapterView<?> adapterView) { }
		});
	}
	
	void search(String s) {
		ResultItemAdapter.ResultInfo.clearItem();
		ResultEntryManager.ResultEntry[] resultEntries =  ResultEntryManager.search(s);
		for (ResultEntryManager.ResultEntry entry : resultEntries) {
			if (ResultItemAdapter.ResultInfo.isAddedEntry(entry.id())) { continue; }
			entry.termHighlightColor(getResources().getColor(R.color.blue_500));
			ResultItemAdapter.ResultInfo.addItem(entry);
		}
		resultFragment.refreshResult();
	}
	
	void executeAddAlert() {
		View main = getLayoutInflater().inflate(R.layout.layout_edit_alertdialog, null);
		View preview = getLayoutInflater().inflate(R.layout.layout_result_list_item, null);
		LinearLayout lyPreview = main.findViewById(R.id.ly_item_preview);
		lyPreview.addView(preview);
		EditText left = main.findViewById(R.id.et_content_left);
		EditText upper = main.findViewById(R.id.et_content_upper);
		EditText bottom = main.findViewById(R.id.et_content_bottom);
		Button btnPreview = main.findViewById(R.id.btn_preview);
		Button btnConfirm = main.findViewById(R.id.btn_confirm);
		Button btnContinue = main.findViewById(R.id.btn_confirm_and_next);
		TextView leftP = preview.findViewById(R.id.content_left);
		TextView upperP = preview.findViewById(R.id.content_upper);
		TextView bottomP = preview.findViewById(R.id.content_bottom);
		Typeface tf = DictManager.getSelectingFont();
		leftP.setTypeface(tf);
		upperP.setTypeface(tf);
		bottomP.setTypeface(tf);
		AlertDialog alert = new AlertDialog.Builder(MainActivity.this).setView(main).create();
		alert.setTitle(getResources().getString(R.string.entry_menu_add_with_name, DictManager.getSelectingAttr(DictManager.KEY.NAME)));
		alert.show();
		View.OnClickListener l = view -> {
			if (left.getText().toString().equals("")) {
				ToastUtil.msg(MainActivity.this, getString(R.string.entry_menu_add_err_no_name));
				return;
			}
			ResultEntryManager.ResultEntry entry = new ResultEntryManager.ResultEntry(
					DictManager.selectingDictIndex,
					left.getText().toString(),
					upper.getText().toString(),
					bottom.getText().toString())
					.termHighlightColor(getResources().getColor(R.color.blue_500));
			if (view.getId() == R.id.btn_preview) {
				leftP.setText(entry.printLeft());
				upperP.setText(entry.printUpper());
				bottomP.setText(entry.printBottom());
			} else if (view.getId() == R.id.btn_confirm) {
				entry.addEntry();
				ToastUtil.msg(MainActivity.this, getString(R.string.entry_menu_add_success_with_term, entry.printLeft()));
				alert.dismiss();
			} else if (view.getId() == R.id.btn_confirm_and_next) {
				entry.addEntry();
				ToastUtil.msg(MainActivity.this, getString(R.string.entry_menu_add_success_with_term, entry.printLeft()));
				leftP.setText("");
				upperP.setText("");
				bottomP.setText("");
				left.setText("");
				upper.setText("");
				bottom.setText("");
			}
		};
		btnPreview.setOnClickListener(l);
		btnConfirm.setOnClickListener(l);
		btnContinue.setOnClickListener(l);
	}
	
	
	void setInterfaceStatus() {
		ArrayAdapter<String> starAdapter;
		spnSelectDict.setPrompt("选择词典");
		if (DictManager.dictCount() > 0) {
			starAdapter = new ArrayAdapter<>(this, R.layout.spinner_drop_down_item, DictManager.dictsName);
		} else {
			starAdapter = new ArrayAdapter<>(this, R.layout.spinner_drop_down_item, new String[]{"（空）"});
		}
		starAdapter.setDropDownViewResource(R.layout.spinner_drop_down_item_selected);
		spnSelectDict.setAdapter(starAdapter);
		
		if (DictManager.dictCount() > 0) {
			spnSelectDict.setEnabled(true);
			inputEditText.setEnabled(true);
			btnAddEntry.setVisibility(View.VISIBLE);
			spnSelectDict.setSelection(DictManager.selectingDictIndex);
			search(inputEditText.getText().toString());
		} else {
			spnSelectDict.setEnabled(false);
			inputEditText.setEnabled(false);
			btnAddEntry.setVisibility(View.GONE);
			search("");
		}
	}
	
	
	
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy() called");
		DbUtil.closeLink();
		super.onDestroy();
	}
	
	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (resultFragment != null) {
			getSupportFragmentManager().putFragment(outState, "result_fragment", resultFragment);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.menu_action_notes) {
			startActivity(new Intent(MainActivity.this, InfoActivity.class));
		} else if (id == R.id.menu_action_sets) {
			startActivityForResult(new Intent(MainActivity.this, SettingActivity.class), 0);
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0) {
			setInterfaceStatus();
		}
	}
}