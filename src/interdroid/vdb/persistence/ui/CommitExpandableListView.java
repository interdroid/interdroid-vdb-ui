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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.ui.BranchExpandableListAdapter.OnRevisionClickListener;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class CommitExpandableListView extends ExpandableListView {
	private static final Logger logger = LoggerFactory
			.getLogger(CommitExpandableListView.class);

	private Context mContext;
	private CommitExpandableListAdapter mCommitAdapter;
	private TextView mHeader;

	public CommitExpandableListView(Context context) {
		super(context);
		mContext = context;
	}

	public CommitExpandableListView(Context context, AttributeSet attributes) {
		super(context, attributes);
		mContext = context;
	}

	public void setup(OnRevisionClickListener listener, VdbRepository repo, String branchName) {
		if (mCommitAdapter == null) {
			mCommitAdapter = new CommitExpandableListAdapter(mContext, repo, branchName);
			// Make the adapter listen for clicks
			setOnChildClickListener(mCommitAdapter);
			setOnItemClickListener(mCommitAdapter);
			setOnItemLongClickListener(mCommitAdapter);

			mHeader = (TextView) LayoutInflater.from(mContext).inflate(R.layout.branch_list_header, null);
			mHeader.setText(branchName);
			addHeaderView(mHeader);
			setAdapter(mCommitAdapter);
			mCommitAdapter.setRevisionClickListener(listener);

		} else {
			mHeader.setText(branchName);
			mCommitAdapter.setRepoAndBranch(repo, branchName);
			mCommitAdapter.setRevisionClickListener(listener);
			postInvalidate();
		}
	}

	public void setBranchName(String branchName) {
		mHeader.setText(branchName);
		mCommitAdapter.setBranchName(branchName);
		postInvalidate();
	}

	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		// Let our parent figure it out most measurements for us
		super.onMeasure( widthMeasureSpec, heightMeasureSpec );
		logger.debug("onMeasure "+this+
				": width: "+decodeMeasureSpec( widthMeasureSpec )+
				"; height: "+decodeMeasureSpec( heightMeasureSpec )+
				"; measuredHeight: "+getMeasuredHeight()+
				"; measuredWidth: "+getMeasuredWidth() );

		int height = getMeasuredHeight() + mCommitAdapter.getHeaderViewHeight(widthMeasureSpec, heightMeasureSpec);
		logger.debug("Header height is: {}", height);
		if (this.isGroupExpanded(0)) {
			int childHeight = mCommitAdapter.getChildViewHeight(widthMeasureSpec, heightMeasureSpec);
			int childCount = mCommitAdapter.getChildrenCount(0);
			logger.debug("Children: {} {}",  childCount, childHeight);
			height += childCount * childHeight;
		}

		logger.debug("Setting measured dimension to: {}x{}", getMeasuredWidth(), height);

		setMeasuredDimension( getMeasuredWidth(), height );
	}

	private String decodeMeasureSpec( int measureSpec ) {
		int mode = View.MeasureSpec.getMode( measureSpec );
		String modeString = "<> ";
		switch( mode ) {
		case View.MeasureSpec.UNSPECIFIED:
			modeString = "UNSPECIFIED ";
			break;

		case View.MeasureSpec.EXACTLY:
			modeString = "EXACTLY ";
			break;

		case View.MeasureSpec.AT_MOST:
			modeString = "AT_MOST ";
			break;
		}
		return modeString+Integer.toString( View.MeasureSpec.getSize( measureSpec ) );
	}
}
