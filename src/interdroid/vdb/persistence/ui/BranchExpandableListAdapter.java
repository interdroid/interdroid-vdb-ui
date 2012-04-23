/*
 * Copyright (c) 2008-2012 Vrije Universiteit, The Netherlands All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the Vrije Universiteit nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;
import interdroid.vdb.persistence.api.VdbRepository;

import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AbsListView.LayoutParams;

public class BranchExpandableListAdapter extends BaseExpandableListAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(BranchExpandableListAdapter.class);

	private Context mContext;
	private VdbRepository mRepo;
	private ArrayList<String> mLocalBranches = new ArrayList<String>();
	private ArrayList<String> mRemoteBranches = new ArrayList<String>();
	private GroupType[] mGroups;

	private OnRevisionClickListener mListener;
	private OnRevisionClickListener mInternalListener = new OnRevisionClickListener() {
		@Override
		public void onRevisionClick(Uri uri) {
			if (mListener != null) {
				mListener.onRevisionClick(uri);
			}
		}
		@Override
		public void onRevisionLongClick(Uri uri) {
			if (mListener != null) {
				mListener.onRevisionLongClick(uri);
			}
		}
	};

	public interface OnRevisionClickListener {
		public void onRevisionClick(Uri uri);
		public void onRevisionLongClick(Uri uri);
	}

	public void setOnRevisionClickListener(OnRevisionClickListener listener) {
		mListener = listener;
	}

	public enum GroupType {
		LOCAL_BRANCHES,
		REMOTE_BRANCHES
	}

	@Override
	public void notifyDataSetChanged() {
		refreshBranches();
		super.notifyDataSetChanged();
	}

	public void notifyDataSetInvalidated() {
		refreshBranches();
		super.notifyDataSetInvalidated();
	}

	public BranchExpandableListAdapter(Context context, VdbRepository repo,  GroupType... groupsToShow) {
		mContext = context;
		if (repo == null) {
			throw new IllegalArgumentException("Repo is null.");
		}
		mRepo = repo;
		if (groupsToShow == null || groupsToShow.length < 1 || groupsToShow.length > 2) {
			throw new IllegalArgumentException("Inalid group count.");
		}
		mGroups = groupsToShow;
		refreshBranches();
	}

	private void refreshBranches() {
		try {
			for (GroupType group : mGroups) {
				switch (group) {
				case LOCAL_BRANCHES:
					logger.debug("Loading local branches");
					mLocalBranches.clear();
					mLocalBranches.addAll(mRepo.listBranches());
					logger.debug("Loaded: {}", mLocalBranches.size());
					break;
				case REMOTE_BRANCHES:
					logger.debug("Loading remote branches");
					mRemoteBranches.clear();
					mRemoteBranches.addAll(mRepo.listRemoteBranches());
					logger.debug("Loaded: {}", mRemoteBranches.size());
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getGroupCount() {
		return mGroups.length;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		switch(mGroups[groupPosition]) {
		case LOCAL_BRANCHES:
			return mLocalBranches.size();
		case REMOTE_BRANCHES:
			return mRemoteBranches.size();
		default:
			throw new IllegalArgumentException("Unknow group position: " + groupPosition);
		}
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mGroups[groupPosition];
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		switch(mGroups[groupPosition]) {
		case LOCAL_BRANCHES:
			return mLocalBranches.get(childPosition);
		case REMOTE_BRANCHES:
			return mRemoteBranches.get(childPosition);
		default:
			throw new IllegalArgumentException("Unknown child position: " + groupPosition + " : " + childPosition);
		}
	}

	@Override
	public long getGroupId(int groupPosition) {
		return mGroups[groupPosition].ordinal();
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		switch(mGroups[groupPosition]) {
		case LOCAL_BRANCHES:
			return mLocalBranches.get(childPosition).hashCode() + (getGroupId(groupPosition) * 11);
		case REMOTE_BRANCHES:
			return mRemoteBranches.get(childPosition).hashCode() + (getGroupId(groupPosition) * 11);
		default:
			throw new IllegalArgumentException("Unknown child position: " + groupPosition + " : " + childPosition);
		}
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

		switch(mGroups[groupPosition]) {
		case LOCAL_BRANCHES:
			view.setText(R.string.label_local_branches);
			break;
		case REMOTE_BRANCHES:
			view.setText(R.string.label_remote_branches);
			break;
			default:
				throw new IllegalArgumentException("Unknown group: " + groupPosition);
		}

		return view;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		logger.debug("Building child view: {} {}", groupPosition, childPosition);
		// TODO: Recover with convertView;
		switch(mGroups[groupPosition]) {
		case LOCAL_BRANCHES:
			return buildBranchItemView(childPosition, convertView, parent, false);
		case REMOTE_BRANCHES:
			return buildBranchItemView(childPosition, convertView, parent, true);
			default:
				throw new IllegalArgumentException("Unknown group: " + groupPosition);
		}
	}

	private View buildBranchItemView(int childPosition, View convertView,
			ViewGroup parent, boolean remote) {
		View view;
		if (convertView == null) {
			view = LayoutInflater.from(mContext).inflate(R.layout.branchlist_item, parent, false);
		} else {
			view = convertView;
		}
		ImageView folder = (ImageView) view.findViewById(R.id.folder);
		CommitExpandableListView commits = (CommitExpandableListView) view.findViewById(R.id.commits_list);
		if (remote) {
			folder.setImageResource(R.drawable.remotefolder);
			commits.setup(mInternalListener, mRepo, mRemoteBranches.get(childPosition));
		} else {
			folder.setImageResource(R.drawable.homefolder);
			commits.setup(mInternalListener, mRepo, mLocalBranches.get(childPosition));
		}

		return view;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}
