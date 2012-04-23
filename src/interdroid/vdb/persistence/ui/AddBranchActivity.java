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

import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import java.io.IOException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.Authority;
import interdroid.vdb.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddBranchActivity extends Activity implements OnClickListener {
	private static final Logger logger = LoggerFactory
			.getLogger(AddBranchActivity.class);

	private static final int REQUEST_PICK_VERSION = 1;

	static final String EXTRA_SELECTED_VERSION = "version";

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
		try {
			vdbRepo_ = VdbRepositoryRegistry.getInstance().getRepository(this, match.repositoryName);
		} catch (IOException e) {
			logger.error("Unable to get repository", e);
			Toast.makeText(this, R.string.error_adding_branch, Toast.LENGTH_LONG);
			finish();
			return;
		}

		buildUI();
		if (intent.hasExtra(EXTRA_SELECTED_VERSION)) {
			Uri version = Uri.parse(intent.getStringExtra(EXTRA_SELECTED_VERSION));
			setChosenRevision(version);
		}
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
			setChosenRevision(data.getData());
		}
	}

	private void setChosenRevision(Uri version) {
		chosenBase_ = EntityUriMatcher.getMatch(version);
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
			Toast.makeText(this, R.string.error_invalid_version, Toast.LENGTH_LONG).show();
			editRevision_.setText(null);
			chosenBase_ = null;
			return;
		}
		editRevision_.setText(text);
	}

	@Override
	public void onClick(View v)
	{
		if (v == btnCreate_) {
//			VdbRepositoryImpl impl = (VdbRepositoryImpl) vdbRepo_;

			String name = editName_.getText().toString();
			if (verifyCreateInputs(name)) {
				try {
					vdbRepo_.createBranch(name, chosenBase_.reference);

					Intent result = new Intent(Intent.ACTION_DEFAULT,
							EntityUriBuilder.branchUri(Authority.VDB, vdbRepo_.getName(), name));
					setResult(RESULT_OK, result);
					finish();
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}
		} else if (v == btnPick_) {
			/* launch the RevisionPicker activity */
			Intent intent = new Intent(Intent.ACTION_PICK,  getIntent().getData());
			startActivityForResult(intent, REQUEST_PICK_VERSION);
		} else if (v == btnCancel_) {
			finish();
		}
	}

	private boolean verifyCreateInputs(String name) {
		if ( verifyBaseRevision() && verifyBranchName(name) ) {
			return true;
		}
		return false;
	}

	private boolean verifyBaseRevision() {
		if (chosenBase_ == null) {
			Toast.makeText(this, "Please pick a base revision.",
					Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	private boolean verifyBranchName(String name) {
		if (!Repository.isValidRefName(Constants.R_REFS + name)
				|| name.contains("/")) {
			Toast.makeText(this, "Invalid branch name.", Toast.LENGTH_SHORT)
			.show();
			return false;
		}
		try {
			if (vdbRepo_.listBranches().contains(name)) {
				Toast.makeText(this, "Branch " + name + " already exists.",
						Toast.LENGTH_SHORT).show();
				return false;
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		return true;
	}
}
