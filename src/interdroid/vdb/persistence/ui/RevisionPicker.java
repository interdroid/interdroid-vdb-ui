package interdroid.vdb.persistence.ui;

import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.ui.RevisionsView.GroupType;
import interdroid.vdb.persistence.ui.RevisionsView.OnRevisionClickListener;

import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class RevisionPicker extends Activity implements OnRevisionClickListener {
	private static final Logger logger = LoggerFactory.getLogger(RevisionPicker.class);

	public static final String ALLOW_LOCAL_BRANCHES = "ALLOW_LOCAL_BRANCHES";
	public static final String ALLOW_REMOTE_BRANCHES = "ALLOW_REMOTE_BRANCHES";
	public static final String ALLOW_COMMITS = "ALLOW_COMMITS";

	private VdbRepository vdbRepo_;
	private RevisionsView revView_;

	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.getData() == null) {
			if (logger.isErrorEnabled())
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
        if (intent.getBooleanExtra(ALLOW_COMMITS, true)) {
        	vGroups.add(GroupType.COMMITS);
        }

        Uri repoUri = intent.getData();
        if (!VdbMainContentProvider.AUTHORITY.equals(repoUri.getAuthority())) {
        	throw new IllegalArgumentException("Invalid authority " + repoUri.getAuthority());
        }
        List<String> pathSegments = repoUri.getPathSegments();
        if (pathSegments.size() != 1) {
			if (logger.isErrorEnabled())
				logger.error("Bad repository URI, need content://authority/repository_name .");
            finish();
            return;
        }
        vdbRepo_ = VdbRepositoryRegistry.getInstance().getRepository(pathSegments.get(0));
        revView_ = new RevisionsView(getApplicationContext(), vdbRepo_, vGroups.toArray(new GroupType[0]));
        setContentView(revView_);

        revView_.setOnRevisionClickListener(this);
	}

	@Override
	public void onRevisionClick(RevisionsView view, Uri revUri, SelectAction type) {
		if (type == SelectAction.CLICK) {
			setResult(RESULT_OK, new Intent(Intent.ACTION_DEFAULT, revUri));
			finish();
		}
	}
}
