package interdroid.vdb.persistence.ui;

import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import java.io.IOException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

import interdroid.vdb.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddBranchActivity extends Activity implements OnClickListener {
	private static final int REQUEST_PICK_VERSION = 1;

	private UriMatch chosenBase_;
	private VdbRepository vdbRepo_;
	private EditText editRevision_, editName_;
	private Button btnCreate_, btnCancel_, btnPick_;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
        UriMatch match = EntityUriMatcher.getMatch(intent.getData());

        if (match.type != MatchType.REPOSITORY) {
        	throw new RuntimeException("Invalid URI, can only add branches to a repository. "
        			+ intent.getData());
        }
        vdbRepo_ = VdbRepositoryRegistry.getInstance().getRepository(match.repositoryName);

		buildUI();
	}

	private void buildUI()
	{
		setTitle("Create new branch");
		setContentView(R.layout.add_branch_dialog);

        editRevision_ = (EditText) findViewById(R.id.revision);
        editName_ = (EditText) findViewById(R.id.branch_name);

        btnCreate_ = (Button) findViewById(R.id.create);
        btnCancel_ = (Button) findViewById(R.id.cancel);
        btnPick_ = (Button) findViewById(R.id.pick_revision);

        btnCreate_.setOnClickListener(this);
        btnCancel_.setOnClickListener(this);
        btnPick_.setOnClickListener(this);
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK && requestCode == REQUEST_PICK_VERSION) {
			chosenBase_ = EntityUriMatcher.getMatch(data.getData());
			String text;
			switch(chosenBase_.type) {
			case COMMIT:
				text = "sha1: " + chosenBase_.reference;
				break;
			case LOCAL_BRANCH:
				text = "local: " + chosenBase_.reference;
				break;
			case REMOTE_BRANCH:
				text = "remote: " + chosenBase_.reference;
				break;
			default:
				throw new IllegalStateException("Invalid URI returned by RevisionPicker");
			}
			editRevision_.setText(text);
		}
	}

	@Override
	public void onClick(View v)
	{
		if (v == btnCreate_) {
//			VdbRepositoryImpl impl = (VdbRepositoryImpl) vdbRepo_;
			if (chosenBase_ == null) {
				Toast.makeText(this, "Please pick a base revision.",
						Toast.LENGTH_SHORT).show();
				return;
			}
			String name = editName_.getText().toString();
			if (!Repository.isValidRefName(Constants.R_REFS + name)
					|| name.contains("/")) {
				Toast.makeText(this, "Invalid branch name.", Toast.LENGTH_SHORT)
						.show();
				return;
			}
			try {
				if (vdbRepo_.listBranches().contains(name)) {
					Toast.makeText(this, "Branch " + name + " already exists.",
							Toast.LENGTH_SHORT).show();
					return;
				}
				vdbRepo_.createBranch(name, chosenBase_.reference);

				Intent result = new Intent(Intent.ACTION_DEFAULT,
						EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY, vdbRepo_.getName(), name));
				setResult(RESULT_OK, result);
				finish();
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		} else if (v == btnPick_) {
			/* launch the RevisionPicker activity */
			Intent intent = new Intent(Intent.ACTION_PICK,  getIntent().getData());
	        startActivityForResult(intent, REQUEST_PICK_VERSION);
		} else if (v == btnCancel_) {
			finish();
		}
	}
}
