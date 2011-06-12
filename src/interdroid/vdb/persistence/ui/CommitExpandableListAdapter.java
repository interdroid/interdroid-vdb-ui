package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;

import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.ui.BranchExpandableListAdapter.OnRevisionClickListener;

import android.content.Context;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView.OnChildClickListener;



public class CommitExpandableListAdapter extends BaseExpandableListAdapter implements OnChildClickListener, OnItemClickListener, OnItemLongClickListener {
	private static final Logger logger = LoggerFactory
	.getLogger(CommitExpandableListAdapter.class);

	private Context mContext;
	private VdbRepository mRepo;
	private String mBranchName;
	private OnRevisionClickListener mListener;

	private ArrayList<RevCommit> mCommits = new ArrayList<RevCommit>();

	private int mHeaderViewHeight;
	private int mChildViewHeight;

	private Uri getCommitUri(int childPosition) {
		return EntityUriBuilder.commitUri(VdbMainContentProvider.AUTHORITY, mRepo.getName(), mCommits.get(childPosition).getName());
	}


	private Uri getBranchUri() {
		return EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY,mRepo.getName(), mBranchName);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		logger.debug("onChildClick: "+ groupPosition + " : " + childPosition + " : " + id);
		if (mListener != null) {
			mListener.onRevisionClick(getCommitUri(childPosition));
			return true;
		}
		return false;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		logger.debug("onItemLongClick: {} {}", position, id);
		if (mListener != null) {
			// position zero is the branch, position 1 is the commit dropdown and position 2 is the first commit
			if (position == 0) {
				mListener.onRevisionLongClick(getBranchUri());
				return true;
			} else if (position > 1) {
				mListener.onRevisionLongClick(getCommitUri(position - 2));
				return true;
			}
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		logger.debug("onItemClick: {} {}", position, id);
		if (mListener != null) {
			mListener.onRevisionClick(getBranchUri());
		}
	}

	@Override
	public void notifyDataSetChanged() {
		refreshCommits();
		super.notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetInvalidated() {
		refreshCommits();
		super.notifyDataSetInvalidated();
	}

	public CommitExpandableListAdapter(Context context, VdbRepository repo, String branchName) {
		mContext = context;
		mRepo = repo;
		mBranchName = branchName;
		refreshCommits();
	}

	public void setRevisionClickListener(OnRevisionClickListener listener) {
		mListener = listener;
	}

	public void setBranchName(String branchName) {
		mBranchName = branchName;
		notifyDataSetChanged();
	}

	private void refreshCommits() {
		mCommits.clear();
		try {
			for (RevCommit c : mRepo.enumerateCommits(mBranchName)) {
				mCommits.add(c);
			}
		} catch (IOException e) {
			logger.error("Error enumerating commits for branch: " + mBranchName);
			throw new RuntimeException("Error enumerating commits", e);
		}
	}

	@Override
	public int getGroupCount() {
		return 1;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mCommits.size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return null;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return mCommits.get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return 1;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		TextView view;
		if (convertView == null) {
			view = new TextView(mContext);
			view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 60));
			view.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			view.setPadding(60, 0, 0, 0);
			view.setTextSize(14);
		} else {
			view = (TextView)convertView;
		}

		view.setText(R.string.label_commits);

		return view;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = buildCommitItemView(parent);
		} else {
			view = convertView;
		}
		setupCommitItemView(view, childPosition);

		return view;
	}


	private void setupCommitItemView(View view, int childPosition) {
		final java.text.DateFormat dateFormat = DateFormat.getDateFormat(mContext);
		final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(mContext);

		RevCommit c = mCommits.get(childPosition);

		Date creationDate = new Date(1000 * c.getCommitTime());

		((TextView)view.findViewById(R.id.title)).setText(c.getShortMessage());
		((TextView)view.findViewById(R.id.commit_sha1)).setText(c.getName());
		((TextView)view.findViewById(R.id.created_at)).setText(
				mContext.getString(R.string.label_creation_time) + " " + dateFormat.format(creationDate)
				+ " " + timeFormat.format(creationDate));
		((TextView)view.findViewById(R.id.created_by)).setText(
				mContext.getString(R.string.label_by) + " " + c.getAuthorIdent().getEmailAddress());
	}

	private View buildCommitItemView(ViewGroup parent) {
		View view = LayoutInflater.from(mContext).inflate(R.layout.commitlist_item, parent, false);
		return view;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public int getHeaderViewHeight(int widthMeasureSpec, int heightMeasureSpec) {
		if (mHeaderViewHeight == 0) {
			View view = getGroupView(0, false, null, null);
			view.measure(widthMeasureSpec, heightMeasureSpec);
			mHeaderViewHeight = view.getMeasuredHeight();
		}
		return mHeaderViewHeight;
	}

	public int getChildViewHeight(int widthMeasureSpec, int heightMeasureSpec) {
		if (mChildViewHeight == 0) {
			View view = buildCommitItemView(null);
			view.measure(widthMeasureSpec, heightMeasureSpec);
			mChildViewHeight = view.getMeasuredHeight();
		}
		return mChildViewHeight;
	}

	public void setRepoAndBranch(VdbRepository repo, String branchName) {
		mRepo = repo;
		mBranchName = branchName;
		notifyDataSetChanged();
	}

}
