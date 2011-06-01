package interdroid.vdb.persistence.ui;

import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.persistence.api.RemoteInfo;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.api.RemoteInfo.RemoteType;

import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import interdroid.vdb.Actions;
import interdroid.vdb.R;

public class EditRemoteActivity extends Activity implements OnClickListener {
	private static final Logger logger = LoggerFactory
			.getLogger(EditRemoteActivity.class);

	private VdbRepository vdbRepo_;
	private boolean addingNewRemote;
	private RemoteInfo remoteInfo_;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
        UriMatch match = EntityUriMatcher.getMatch(intent.getData());
        if (match.repositoryName != null) {
	        try {
				vdbRepo_ = VdbRepositoryRegistry.getInstance()
					.getRepository(this, match.repositoryName);
			} catch (IOException e) {
				logger.error("Error getting repository", e);
				Toast.makeText(this, R.string.error_opening_repo, Toast.LENGTH_LONG);
				finish();
				return;
			}
        }

        if (getIntent().getAction().equals(Actions.ACTION_ADD_REMOTE)) {
	        if (match.type != MatchType.REPOSITORY) {
	        	throw new RuntimeException("Invalid URI, need a repository. "
	        			+ intent.getData());
	        }
	        addingNewRemote = true;
	        remoteInfo_ = new RemoteInfo();
        } else if (Actions.ACTION_EDIT_REMOTE.equals(getIntent().getAction())
        		|| Intent.ACTION_EDIT.equals(getIntent().getAction())) {
        	if (match.type != MatchType.REMOTE) {
	        	throw new RuntimeException("Invalid URI, need a remote for edit. "
	        			+ intent.getData());
	        }
        	addingNewRemote = false;
        	try {
				remoteInfo_ = vdbRepo_.getRemoteInfo(match.reference);
				if (remoteInfo_ == null) {
					throw new RuntimeException("Invalid EDIT requested on non existing remote.");
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
        } else {
        	throw new RuntimeException("Invalid action. ");
        }
		buildUI();
		updateData(false);
	}

	private static final String[] protocols__
			= new String[] { "ss", "ssh", "http", "https", "git", "git+bluetooth" };

	private static final String[] type_labels__
			= new String[] { "merge point", "hub mode" };
	private static final RemoteType[] types__
			= new RemoteType[] {RemoteType.MERGE_POINT, RemoteType.HUB};

	private Spinner spnProtocol_, spnType_;
	private EditText editName_, editDescription_, editHostname_, editUsername_,
			editPassword_, editOurNameOnRemote_, editPath_;
	private TextView tvOurNameOnRemote_;
	private Button btnSave_, btnCancel_;

	private void buildUI()
	{
		setTitle("Remote configuration");
		setContentView(R.layout.edit_remote_activity_dialog);

		editName_ = (EditText) findViewById(R.id.name);
		editDescription_ = (EditText) findViewById(R.id.description);
		editHostname_ = (EditText) findViewById(R.id.hostname);
		editUsername_ = (EditText) findViewById(R.id.username);
		editPassword_ = (EditText) findViewById(R.id.password);
		editOurNameOnRemote_ = (EditText) findViewById(R.id.our_name_on_remote);
		editPath_ = (EditText) findViewById(R.id.path);
		tvOurNameOnRemote_ = (TextView) findViewById(R.id.tv_our_name_on_remote);
		btnSave_ = (Button) findViewById(R.id.save);
		btnCancel_ = (Button) findViewById(R.id.cancel);

		editName_.setEnabled(addingNewRemote);
		// TODO(emilian): enabled is not readonly
		{
			spnType_ = (Spinner) findViewById(R.id.type);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item,
					type_labels__);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spnType_.setAdapter(adapter);
			spnType_.setOnItemSelectedListener(new OnItemSelectedListener() {
				private void showHubControls(boolean shown) {
					tvOurNameOnRemote_.setVisibility(shown ? View.VISIBLE : View.GONE);
					editOurNameOnRemote_.setVisibility(shown ? View.VISIBLE : View.GONE);
				}

				@Override
				public void onItemSelected(AdapterView<?> adapter, View view, int pos,
						long id)
				{
					showHubControls(types__[pos] == RemoteType.HUB);
				}

				@Override
				public void onNothingSelected(AdapterView<?> adapter)
				{
					showHubControls(false);
				}
			});
		}

		{
			spnProtocol_ = (Spinner) findViewById(R.id.protocol);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item,
					protocols__);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spnProtocol_ .setAdapter(adapter);
		}

		btnSave_.setOnClickListener(this);
		btnCancel_.setOnClickListener(this);
	}

	private String nullAsEmpty(String val)
	{
		return val == null ? "" : val;
	}

	private String emptyAsNull(String val)
	{
		return val.length() == 0 ? null : val;
	}

	private void updateData(boolean controlsToData)
	{
		if (!controlsToData) {
			editName_.setText(remoteInfo_.getName());
			editDescription_.setText(remoteInfo_.getDescription());

			for (int i = 0; i < types__.length; i++) {
				if (types__[i] == remoteInfo_.getType()) {
					spnType_.setSelection(i);
					break;
				}
			}

			editOurNameOnRemote_.setText(nullAsEmpty(remoteInfo_.getOurNameOnRemote()));
			URIish uri = remoteInfo_.getRemoteUri();
			uri = (uri == null) ? new URIish() : uri;

			editHostname_.setText(nullAsEmpty(uri.getHost()));
			editUsername_.setText(nullAsEmpty(uri.getUser()));
			editPassword_.setText(nullAsEmpty(uri.getPass()));
			editPath_.setText(nullAsEmpty(uri.getPath()));

			for (int i = 0; i < protocols__.length; i++) {
				if (protocols__[i].equals(uri.getScheme())) {
					spnProtocol_.setSelection(i);
					break;
				}
			}
		} else {
			remoteInfo_.setName(editName_.getText().toString());
			remoteInfo_.setDescription(editDescription_.getText().toString());

			RemoteType type = types__[spnType_.getSelectedItemPosition()];
			remoteInfo_.setType(type);
			if (type == RemoteType.HUB) {
				remoteInfo_.setOurNameOnRemote(editOurNameOnRemote_.getText().toString());
			}

			URIish uri = new URIish();
			uri = uri.setScheme(protocols__[spnProtocol_.getSelectedItemPosition()]);
			uri = uri.setHost(editHostname_.getText().toString());
			// these are optional, blank is null
			uri = uri.setUser(emptyAsNull(editUsername_.getText().toString()));
			uri = uri.setPass(emptyAsNull(editPassword_.getText().toString()));
			uri = uri.setPath(emptyAsNull(editPath_.getText().toString()));
			remoteInfo_.setRemoteUri(uri);
		}
	}

	private boolean validate()
	{
		String name = remoteInfo_.getName();
		if (name == null || !Repository.isValidRefName("refs/remote/" + name)) {
			Toast.makeText(this, "Invalid name.", Toast.LENGTH_LONG).show();
			editName_.requestFocus();
			return false;
		}
		if (remoteInfo_.getType() == RemoteType.HUB) {
			String nameOnRemote = remoteInfo_.getOurNameOnRemote();
			if (nameOnRemote == null
					|| !Repository.isValidRefName("refs/remote/" + nameOnRemote)) {
				Toast.makeText(this, "Invalid name.", Toast.LENGTH_LONG).show();
				editOurNameOnRemote_.requestFocus();
				return false;
			}
		}
		URIish uri = remoteInfo_.getRemoteUri();
		if (uri == null) {
			throw new IllegalStateException("Remote URI is null, call updateData(true) first.");
		}
		if (uri.getHost().length() == 0) {
			Toast.makeText(this, "Empty hostname.", Toast.LENGTH_LONG).show();
			editHostname_.requestFocus();
			return false;
		}
		if (uri.getPath() == null || !uri.getPath().startsWith("/")
				|| uri.getPath().length() < 2) {
			Toast.makeText(this, "Path has to be non-empty and starting with /.",
					Toast.LENGTH_LONG).show();
			editPath_.requestFocus();
			return false;
		}
		if (uri.getUser() == null && uri.getPass() != null) {
			Toast.makeText(this, "Need user when password is present.",
					Toast.LENGTH_LONG).show();
			editUsername_.requestFocus();
			return false;
		}
		return true;
	}

	@Override
	public void onClick(View v)
	{
		if (v == btnSave_) {
			updateData(true);
			if (!validate()) {
				return;
			}
			try {
				vdbRepo_.saveRemote(remoteInfo_);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			setResult(RESULT_OK);
			finish();
		} else if (v == btnCancel_) {
			setResult(RESULT_CANCELED);
			finish();
		}
	}
}
