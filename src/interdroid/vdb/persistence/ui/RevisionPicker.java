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

import interdroid.vdb.Authority;
import interdroid.vdb.R;
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
		if (!Authority.VDB.equals(repoUri.getAuthority())) {
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
