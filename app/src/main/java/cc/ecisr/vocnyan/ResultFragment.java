package cc.ecisr.vocnyan;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import cc.ecisr.vocnyan.struct.DictManager;
import cc.ecisr.vocnyan.struct.ResultEntryManager;
import cc.ecisr.vocnyan.utils.ToastUtil;

public class ResultFragment extends Fragment {
	private static final String TAG = "`ResultFragment";
	
	static private RecyclerView mRvMain;  // TODO: 不加static会显示两个View // a SHITTY method
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View selfView = inflater.inflate(R.layout.fragment_result, container, false);
		mRvMain = selfView.findViewById(R.id.result_list);
		
		ResultItemAdapter marketItemAdapter = new ResultItemAdapter(getActivity(), new ResultItemAdapter.iOnItemClickListener() {
			@Override
			public void onClick(@NonNull ResultItemAdapter.LinearViewHolder holder) {
				ArrayList<String> selectionList = new ArrayList<>();
				selectionList.add(getString(R.string.entry_menu_copy));
				if ("1".equals(DictManager.getSelectingAttr(DictManager.KEY.EDITABLE))) {
					selectionList.add(getString(R.string.entry_menu_edit));
					selectionList.add(getString(R.string.entry_menu_del));
				}
				final String[] selections = selectionList.toArray(new String[0]);
				new AlertDialog.Builder(getContext())
						.setItems(selections, (dialogInterface, i) -> {
							if (i == 0) {
								View view = inflater.inflate(R.layout.layout_copy_alertdialog, null);
								TextView tv = view.findViewById(R.id.dialog_box_tv);
								tv.setText(holder.printContent());
								tv.setTypeface(DictManager.getFont(holder.entry.dictIndex()));
								//tv.setTypeface(Typeface.createFromAsset(inflater.getContext().getAssets(), "HuanZhuangziSong.otf"));
								new AlertDialog.Builder(getContext())
										.setView(view).setPositiveButton(R.string.button_confirm, null).show();
							} else if (i == 1) {
								executeEditionAlert(inflater, holder);
							} else if (i == 2) {
								executeDeletionAlert(inflater, holder);
							}
						}).create().show();
				
			}
			
			@Override
			public boolean onLongClick(@NonNull ResultItemAdapter.LinearViewHolder holder) {
				if ("1".equals(DictManager.getSelectingAttr(DictManager.KEY.EDITABLE))) {
					executeEditionAlert(inflater, holder);
				}
				return false;
			}
		});
		mRvMain.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
		mRvMain.setItemAnimator(new DefaultItemAnimator());
		mRvMain.setAdapter(marketItemAdapter);
		//mRvMain.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
		if (savedInstanceState!= null) {
			//rawReceivedData = savedInstanceState.getString("received_data");
			// TODO: refreshResult();
		}
		
		return selfView;
	}
	
	void executeEditionAlert(LayoutInflater inflater, @NonNull ResultItemAdapter.LinearViewHolder holder) {
		View main = inflater.inflate(R.layout.layout_edit_alertdialog, null);
		View preview = inflater.inflate(R.layout.layout_result_list_item, null);
		LinearLayout lyPreview = main.findViewById(R.id.ly_item_preview);
		lyPreview.addView(preview);
		EditText left = main.findViewById(R.id.et_content_left);
		EditText upper = main.findViewById(R.id.et_content_upper);
		EditText bottom = main.findViewById(R.id.et_content_bottom);
		Button btnPreview = main.findViewById(R.id.btn_preview);
		Button btnConfirm = main.findViewById(R.id.btn_confirm);
		Button btnContinue = main.findViewById(R.id.btn_confirm_and_next);
		left.setText(holder.entry.dumpLeft());
		upper.setText(holder.entry.dumpUpper());
		bottom.setText(holder.entry.dumpBottom());
		TextView leftP = preview.findViewById(R.id.content_left);
		TextView upperP = preview.findViewById(R.id.content_upper);
		TextView bottomP = preview.findViewById(R.id.content_bottom);
		Typeface tf = DictManager.getFont(holder.entry.dictIndex());
		leftP.setTypeface(tf);
		upperP.setTypeface(tf);
		bottomP.setTypeface(tf);
		AlertDialog alert = new AlertDialog.Builder(getContext()).setView(main).create();
		alert.setTitle(R.string.entry_menu_edit);
		alert.show();
		View.OnClickListener l = view -> {
			if (left.getText().toString().equals("")) {
				ToastUtil.msg(getContext(), getString(R.string.entry_menu_add_err_no_name));
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
			} else if (view.getId() == R.id.btn_confirm_and_next) {
				entry.id(holder.entry.id()).updateEntry();
				ResultItemAdapter.ResultInfo.updateItem(holder.getAdapterPosition(), entry);
				refreshResult();
				ToastUtil.msg(getContext(), getString(R.string.entry_menu_add_success_with_term, entry.printLeft()));
				alert.dismiss();
			}
		};
		btnPreview.setOnClickListener(l);
		btnContinue.setOnClickListener(l);
		btnContinue.setText(R.string.entry_menu_edit);
		btnConfirm.setVisibility(View.GONE);
	}
	void executeDeletionAlert(LayoutInflater inflater, @NonNull ResultItemAdapter.LinearViewHolder holder) {
		View main = inflater.inflate(R.layout.layout_edit_alertdialog, null);
		View preview = inflater.inflate(R.layout.layout_result_list_item, null);
		LinearLayout lyPreview = main.findViewById(R.id.ly_item_preview);
		lyPreview.addView(preview);
		main.findViewById(R.id.ly_item_content).setVisibility(View.GONE);
		TextView leftP = preview.findViewById(R.id.content_left);
		TextView upperP = preview.findViewById(R.id.content_upper);
		TextView bottomP = preview.findViewById(R.id.content_bottom);
		leftP.setText(holder.tvLeft.getText());
		upperP.setText(holder.tvUpper.getText());
		bottomP.setText(holder.tvBottom.getText());
		Typeface tf = DictManager.getFont(holder.entry.dictIndex());
		leftP.setTypeface(tf);
		upperP.setTypeface(tf);
		bottomP.setTypeface(tf);
		AlertDialog.Builder alert = new AlertDialog.Builder(getContext()).setView(main);
		alert.setPositiveButton(R.string.button_del, (dialogInterface, i) -> {
			holder.entry.deleteEntry();
			ResultItemAdapter.ResultInfo.delItem(holder.getAdapterPosition());
			refreshResult();
		});
		alert.setTitle(R.string.entry_menu_del);
		alert.setNegativeButton(R.string.button_cancel, null);
		alert.create().show();
	}
	
	public void refreshResult() {
		if (mRvMain==null || mRvMain.getAdapter()==null) { return; }
		//mRvMain.getAdapter().notifyItemRangeChanged(0, ResultItemAdapter.ResultInfo.list.size());
		mRvMain.getAdapter().notifyDataSetChanged();
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		Log.d(TAG, "onSaveInstanceState: " + System.identityHashCode(this));
		super.onSaveInstanceState(outState);
		//outState.putString("received_data", rawReceivedData);
	}
}