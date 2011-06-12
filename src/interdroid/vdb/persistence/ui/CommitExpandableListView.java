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
