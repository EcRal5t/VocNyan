package cc.ecisr.vocnyan;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;

import cc.ecisr.vocnyan.struct.DictManager;
import cc.ecisr.vocnyan.struct.ResultEntryManager;

public class ResultItemAdapter extends RecyclerView.Adapter<ResultItemAdapter.LinearViewHolder>  {
	private final Context mContext;
	private final iOnItemClickListener mListener;
	//private static Typeface tf = null;
	ResultItemAdapter(Context context, iOnItemClickListener listener) {
		this.mContext = context; // 主activity
		this.mListener = listener; // 提供給fragment的監聽器
		//tf = Typeface.createFromAsset(mContext.getAssets(), "HuanZhuangziSong.otf");
	}
	
	@NonNull
	@Override
	public ResultItemAdapter.LinearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new LinearViewHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_result_list_item, parent, false));
	}
	
	@Override
	public void onBindViewHolder(@NonNull ResultItemAdapter.LinearViewHolder holder, final int position) {
		ArrayList<Spanned> item = ResultInfo.list.get(position);
		
		Spanned left = item.get(ResultInfo.LEFT);
		Spanned upper = item.get(ResultInfo.UPPER);
		Spanned bottom = item.get(ResultInfo.BOTTOM);
		
		holder.tvLeft.setText(left);
		if (TextUtils.isEmpty(upper) && bottom.length()<20) {
			holder.tvUpper.setText(bottom);
			holder.tvBottom.setText("");
		} else {
			holder.tvUpper.setText(upper);
			holder.tvBottom.setText(bottom);
		}
		//holder.tvBottom.setMovementMethod(LinkMovementMethod.getInstance());
		int tvLeftVisibility = (!TextUtils.isEmpty(holder.tvLeft.getText())) ? View.VISIBLE : View.GONE;
		int tvUpperVisibility = (!TextUtils.isEmpty(holder.tvUpper.getText())) ? View.VISIBLE : View.GONE;
		int tvBottomVisibility = (!TextUtils.isEmpty(holder.tvBottom.getText())) ? View.VISIBLE : View.GONE;
		holder.tvLeft.setVisibility(tvLeftVisibility);
		holder.tvUpper.setVisibility(tvUpperVisibility);
		holder.tvBottom.setVisibility(tvBottomVisibility);
		
		holder.itemView.setOnClickListener(v -> mListener.onClick(holder));
		holder.itemView.setOnLongClickListener(v -> mListener.onLongClick(holder));
		
		holder.entry = ResultInfo.entries.get(position);
		
		Typeface tf = DictManager.getFont(holder.entry.dictIndex());
		holder.tvLeft.setTypeface(tf);
		holder.tvUpper.setTypeface(tf);
		holder.tvBottom.setTypeface(tf);
		
		ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
		layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
	}
	
	
	@Override
	public int getItemViewType(int position) { return super.getItemViewType(position); }
	
	@Override
	public int getItemCount() {
		return ResultInfo.list.size();
	}
	
	static class LinearViewHolder extends RecyclerView.ViewHolder {
		LinearLayout lyEntry;
		TextView tvLeft, tvUpper, tvBottom;
		ResultEntryManager.ResultEntry entry;
		
		LinearViewHolder(@NonNull View itemView) {
			super(itemView);
			lyEntry = itemView.findViewById(R.id.item_content);
			tvLeft = itemView.findViewById(R.id.content_left);
			tvUpper = itemView.findViewById(R.id.content_upper);
			tvBottom = itemView.findViewById(R.id.content_bottom);
			//tvUpper.setTypeface(Typeface.SERIF);
		}
		
		String printContent() {
			return tvLeft.getText().toString() + "\t" +
					tvUpper.getText().toString() + "\n" +
					tvBottom.getText().toString();
		}
		String dumpContent() {
			return entry.dumpLeft();
		}
	}
	
	public interface iOnItemClickListener {
		void onClick(@NonNull ResultItemAdapter.LinearViewHolder holder);
		boolean onLongClick(@NonNull ResultItemAdapter.LinearViewHolder holder);
	}
	
	static public class ResultInfo {
		private static final int LEFT = 0, UPPER = 1, BOTTOM = 2;
		
		static ArrayList<ArrayList<Spanned>> list = new ArrayList<>(10);
		static ArrayList<ResultEntryManager.ResultEntry> entries = new ArrayList<>(10);
		static HashSet<Integer> ids = new HashSet<>(10);
		
		ResultInfo() {
			//list = new ArrayList<>(0);
		}
		
		static public boolean isAddedEntry(int id) {
			return ids.contains(id);
		}
		
		static public void addItem(ResultEntryManager.ResultEntry entry) {
			ArrayList<Spanned> item = new ArrayList<>(3);
			item.add(entry.printLeft());
			item.add(entry.printUpper());
			item.add(entry.printBottom());
			list.add(item);
			ids.add(entry.id());
			entries.add(entry);
		}
		
		static void clearItem() {
			list.clear();
			entries.clear();
			ids.clear();
		}
		
		static void delItem(int i) {
			list.remove(i);
			ids.remove(entries.get(i).id());
			entries.remove(i);
		}
		
		static void updateItem(int i, ResultEntryManager.ResultEntry entry) {
			if (i<0 || i> list.size()) return;
			ArrayList<Spanned> item = new ArrayList<>(3);
			item.add(entry.printLeft());
			item.add(entry.printUpper());
			item.add(entry.printBottom());
			list.set(i, item);
			// id won't change
			entries.set(i, entry);
		}
	}
}
