package interdroid.vdb.persistence.ui;

import interdroid.vdb.R;
import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.ui.BranchExpandableListAdapter;
import interdroid.vdb.persistence.ui.BranchExpandableListAdapter.GroupType;
import interdroid.vdb.persistence.ui.BranchExpandableListAdapter.OnRevisionClickListener;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ExpandableListView;
import android.widget.Toast;

public class RevisionPicker extends Activity implements OnRevisionClickListener {
	private static final Logger logger = LoggerFactory.getLogger(RevisionPicker.class);

	public static final String ALLOW_LOCAL_BRANCHES = "ALLOW_LOCAL_BRANCHES";
	public static final String ALLOW_REMOTE_BRANCHES = "ALLOW_REMOTE_BRANCHES";
	public static final String ALLOW_COMMITS = "ALLOW_COMMITS";

	private VdbRepository vdbRepo_;
	private ExpandableListView revView_;
	private BranchExpandableListAdapter revViewAdapter_;

	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.title_pick_branch));

        Intent intent = getIntent();
        if (intent.getData() == null) {
        	logger.error("Need repository URI, exiting");
            finish();
            return;
        }

        Vector<GroupType> vGroups = new Vector<GroupType>();
        if (intent.getBooleanExtra(ALLOW_LOCAL_BRANCHES, true)) {
        	vGroups.add(GroupType.LOCAL_BRANCHES);
        }
        if (intent.getBooleanExtra(ALLOW_REMOTE_BRANCHES, true)) {
        	vGroups.add(GroupType.REMOTE_BRANCHES);
        }

        Uri repoUri = intent.getData();
        if (!VdbMainContentProvider.AUTHORITY.equals(repoUri.getAuthority())) {
        	throw new IllegalArgumentException("Invalid authority " + repoUri.getAuthority());
        }
        List<String> pathSegments = repoUri.getPathSegments();
        if (pathSegments.size() != 1) {
        	logger.error("Bad repository URI, need content://authority/repository_name .");
            finish();
            return;
        }
        try {
			vdbRepo_ = VdbRepositoryRegistry.getInstance().getRepository(this, pathSegments.get(0));
		} catch (IOException e) {
			logger.error("Error getting repository", e);
			Toast.makeText(this, R.string.error_opening_repo, Toast.LENGTH_LONG);
		}
        revView_ = new ExpandableListView(getApplicationContext());
        revViewAdapter_ = new BranchExpandableListAdapter(getBaseContext(), vdbRepo_, GroupType.LOCAL_BRANCHES, GroupType.REMOTE_BRANCHES);
        revView_.setAdapter(revViewAdapter_);
        setContentView(revView_);

        revViewAdapter_.setOnRevisionClickListener(this);
	}

	@Override
	public void onRevisionClick(Uri uri) {
		setResult(RESULT_OK, new Intent(Intent.ACTION_DEFAULT, uri));
		finish();
	}

	@Override
	public void onRevisionLongClick(Uri uri) {
		// Ignored
	}
}
