package interdroid.vdb.persistence.ui;

import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.ui.RevisionsView.OnRevisionClickListener.SelectAction;

import java.io.IOException;
import java.sql.Date;
import java.util.Vector;

import org.eclipse.jgit.revwalk.RevCommit;

import interdroid.vdb.R;

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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView.OnChildClickListener;

public class RevisionsView extends ExpandableListView
implements OnItemLongClickListener, OnChildClickListener {
	public enum GroupType {
		LOCAL_BRANCHES,
		REMOTE_BRANCHES,
		COMMITS
	}

	private final VdbRepository vdbRepo_;
	private Vector<RevCommit> commits_;
	private Vector<String> branches_, remoteBranches_;
	private GroupType[] groups_;
	private MyExpandableListAdapter adapter_;

	class MyExpandableListAdapter extends BaseExpandableListAdapter {

		public MyExpandableListAdapter() {}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			// don't need any data
			return null;
		}

		View buildCommitItemView(RevCommit c, View convertView, ViewGroup parent) {
			final java.text.DateFormat dateFormat = DateFormat.getDateFormat(getContext());
			final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(getContext());

			Date creationDate = new Date(1000 * c.getCommitTime());

			View view = LayoutInflater.from(getContext()).inflate(R.layout.commitlist_item, parent, false);
			((TextView)view.findViewById(R.id.title)).setText(c.getShortMessage());
			((TextView)view.findViewById(R.id.commit_sha1)).setText(c.getName());
			((TextView)view.findViewById(R.id.created_at)).setText(
					"Created at  " + dateFormat.format(creationDate)
					+ " " + timeFormat.format(creationDate));
			((TextView)view.findViewById(R.id.created_by)).setText(
					"by " + c.getAuthorIdent().getEmailAddress());
			return view;
		}

		View buildBranchItemView(int childPosition, View convertView, ViewGroup parent) {
			View view = LayoutInflater.from(getContext()).inflate(R.layout.branchlist_item, parent, false);
			((TextView)view.findViewById(R.id.name)).setText(branches_.get(childPosition));
			return view;
		}

		View buildRemoteItemView(int childPosition, View convertView, ViewGroup parent) {
			View view = LayoutInflater.from(getContext()).inflate(R.layout.remotelist_item, parent, false);
			((TextView)view.findViewById(R.id.name)).setText(remoteBranches_.get(childPosition));
			return view;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			switch(groups_[groupPosition]) {
			case LOCAL_BRANCHES:
				return buildBranchItemView(childPosition, convertView, parent);
			case REMOTE_BRANCHES:
				return buildRemoteItemView(childPosition, convertView, parent);
			case COMMITS:
				return buildCommitItemView(commits_.get(childPosition), convertView, parent);
			}
			throw new IllegalStateException("Invalid group position");
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			switch(groups_[groupPosition]) {
			case LOCAL_BRANCHES:
				return branches_.size();
			case REMOTE_BRANCHES:
				return remoteBranches_.size();
			case COMMITS:
				return commits_.size();
			}
			throw new IllegalStateException("Invalid group position");
		}

		@Override
		public Object getGroup(int groupPosition) {
			// don't need any data
			return null;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView view = new TextView(getContext());
			view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 64));
			view.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			view.setPadding(36, 0, 0, 0);
			view.setTextSize(14);

			switch(groups_[groupPosition]) {
			case LOCAL_BRANCHES:
				view.setText("Local branches");
				break;
			case REMOTE_BRANCHES:
				view.setText("Remote branches");
				break;
			case COMMITS:
				view.setText("Revision history");
				break;
			}
			return view;
		}

		@Override
		public int getGroupCount() {
			return groups_.length;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return getPackedPositionForGroup(groupPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return getPackedPositionForChild(groupPosition, childPosition);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}

	public void refresh()
	{
		refreshCommits();
		adapter_.notifyDataSetChanged();
	}

	private void refreshCommits() {
		commits_ = new Vector<RevCommit>();
		branches_ = new Vector<String>();
		remoteBranches_ = new Vector<String>();
		try {
			branches_.addAll(vdbRepo_.listBranches());
			remoteBranches_.addAll(vdbRepo_.listRemoteBranches());
			for (RevCommit c : vdbRepo_.enumerateCommits(branches_.toArray(new String[0]))) {
				commits_.add(c);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public RevisionsView(Context context, VdbRepository vdbRepo, GroupType... groupsToShow) {
		super(context);

		vdbRepo_ = vdbRepo;
		if (groupsToShow.length == 0) {
			groupsToShow = new GroupType[] {
					GroupType.LOCAL_BRANCHES, GroupType.REMOTE_BRANCHES, GroupType.COMMITS};
		}
		groups_ = groupsToShow;

		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		setItemsCanFocus(false);
		setChoiceMode(CHOICE_MODE_SINGLE);
		refreshCommits();
		adapter_ = new MyExpandableListAdapter();
		setAdapter(adapter_);

		// This will be the favored list, so expand it by default
		expandGroup(0);

        setOnChildClickListener(this);
		setOnItemLongClickListener(this);
	}

	public static interface OnRevisionClickListener {
		public enum SelectAction {
			CLICK, LONG_CLICK;
		}
		public void onRevisionClick(RevisionsView view, Uri revUri, SelectAction type);
	}

	private OnRevisionClickListener theListener_;

	public void setOnRevisionClickListener(OnRevisionClickListener listener) {
		if (theListener_ != null) {
			throw new IllegalStateException("Only one listener is supported.");
		}
		theListener_ = listener;
	}

	public Uri getSelectedUri()
	{
		long id = getSelectedPosition();
		if (id ==  INVALID_POSITION || getPackedPositionType(id) != PACKED_POSITION_TYPE_CHILD) {
			return null;
		}
		int groupPosition = getPackedPositionGroup(id);
		int childPosition = getPackedPositionChild(id);
		return getUriAt(groupPosition, childPosition);
	}

	private Uri getUriAt(int groupPosition, int childPosition)
	{
		switch(groups_[groupPosition]) {
		case LOCAL_BRANCHES:
			return EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY, vdbRepo_.getName(), branches_.get(childPosition));
		case REMOTE_BRANCHES:
			return EntityUriBuilder.remoteBranchUri(VdbMainContentProvider.AUTHORITY, vdbRepo_.getName(),
					remoteBranches_.get(childPosition));
		case COMMITS:
			return EntityUriBuilder.commitUri(VdbMainContentProvider.AUTHORITY, vdbRepo_.getName(),
					commits_.get(childPosition).getName());
		}
		return null;
	}

	private void fireClick(int groupPosition, int childPosition, SelectAction type)
	{
		if (theListener_ != null) {
			theListener_.onRevisionClick(this,
					getUriAt(groupPosition, childPosition), type);
		}
	}

	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id)
	{
		fireClick(groupPosition, childPosition, OnRevisionClickListener.SelectAction.CLICK);
		return true;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position,
			long id) {
		if (getPackedPositionType(id) != PACKED_POSITION_TYPE_CHILD) {
			// only items are selectable
			return false;
		}
		if (theListener_ != null) {
			int groupPosition = getPackedPositionGroup(id);
			int childPosition = getPackedPositionChild(id);
			fireClick(groupPosition, childPosition,
					OnRevisionClickListener.SelectAction.LONG_CLICK);
		}
		return false;
	}
}
