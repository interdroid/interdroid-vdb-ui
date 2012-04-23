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
			vdbBranch_ = VdbRepositoryRegistry.getInstance().getRepository(this, match.repositoryName)
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
