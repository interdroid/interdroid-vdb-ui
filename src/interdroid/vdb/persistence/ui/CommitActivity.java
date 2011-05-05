package interdroid.vdb.persistence.ui;

import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.persistence.api.MergeInProgressException;
import interdroid.vdb.persistence.api.VdbCheckout;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import java.io.IOException;


import interdroid.vdb.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class CommitActivity extends Activity implements OnClickListener {
	private EditText editAuthorName_, editAuthorEmail_, editMessage_;
	private VdbCheckout vdbBranch_;
	private Button btnCommit_, btnCancel_;
	private SharedPreferences prefs_;

	private final String PREF_AUTHOR_NAME = "authorName";
	private final String PREF_AUTHOR_EMAIL = "authorEmail";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        UriMatch match = EntityUriMatcher.getMatch(intent.getData());

        if (match.type != MatchType.LOCAL_BRANCH) {
        	throw new RuntimeException("Invalid URI, can only commit on a local branch. "
        			+ intent.getData());
        }

        try {
			vdbBranch_ = VdbRepositoryRegistry.getInstance().getRepository(match.repositoryName)
					.getBranch(match.reference);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		prefs_ = getPreferences(MODE_PRIVATE);
		buildUI();
	}

	protected void buildUI()
	{
        setContentView(R.layout.commit_dialog);

        editAuthorName_ = (EditText) findViewById(R.id.author_name);
        editAuthorEmail_ = (EditText) findViewById(R.id.author_email);
        editMessage_ = (EditText) findViewById(R.id.message);

        if (!prefs_.contains(PREF_AUTHOR_NAME)) {
        	editAuthorName_.selectAll();
        } else {
        	editAuthorName_.setText(prefs_.getString(PREF_AUTHOR_NAME, ""));
        }

        if (!prefs_.contains(PREF_AUTHOR_EMAIL)) {
        	editAuthorEmail_.selectAll();
        } else {
        	editAuthorEmail_.setText(prefs_.getString(PREF_AUTHOR_EMAIL, ""));
        }
        editMessage_.selectAll();

        btnCommit_ = (Button) findViewById(R.id.commit);
        btnCancel_ = (Button) findViewById(R.id.cancel);

        btnCommit_.setOnClickListener(this);
        btnCancel_.setOnClickListener(this);
	}

	@Override
	public void onClick(View v)
	{
		Toast t = null;
		if (v == btnCommit_) {
			try {
				String authorName = editAuthorName_.getText().toString();
				String authorEmail = editAuthorEmail_.getText().toString();
				String message = editMessage_.getText().toString();

				prefs_.edit().putString(PREF_AUTHOR_NAME, authorName)
					.putString(PREF_AUTHOR_EMAIL, authorEmail).commit();
				vdbBranch_.commit(authorName, authorEmail, message);
				t = Toast.makeText(this, "Commit was successful.", Toast.LENGTH_SHORT);
			} catch (IOException e) {
				t = Toast.makeText(this, "Commit error " + e.getMessage(),
						Toast.LENGTH_LONG);
			} catch (MergeInProgressException e) {
				t = Toast.makeText(this, "Commit canceled - unresolved merge is in progress.",
						Toast.LENGTH_LONG);
			}
		} else {
			t = Toast.makeText(this, "Commit canceled.", Toast.LENGTH_SHORT);
		}
		t.show();
		CommitActivity.this.finish();
	}
}
