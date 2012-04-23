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

import interdroid.vdb.Actions;
import interdroid.vdb.R;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.ui.BranchExpandableListAdapter.GroupType;
import interdroid.vdb.persistence.ui.BranchExpandableListAdapter.OnRevisionClickListener;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;
import android.widget.Toast;

public class ManageLocalBranchesActivity extends Activity implements OnRevisionClickListener {
	private static final Logger logger = LoggerFactory
	.getLogger(ManageLocalBranchesActivity.class);

	private VdbRepository vdbRepo_;
	private static int REQUEST_ADD_BRANCH = 1;

	private ExpandableListView revView_;
	private BranchExpandableListAdapter revViewAdapter_;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		logger.debug("Managing local branches of: {}", intent.getData());
		UriMatch match = EntityUriMatcher.getMatch(intent.getData());

		if (match.type != MatchType.REPOSITORY) {
			throw new RuntimeException("Invalid URI, can only add branches to a repository. "
					+ intent.getData());
		}
		logger.debug("Getting repository for: {}", match.repositoryName);
		try {
			vdbRepo_ = VdbRepositoryRegistry.getInstance().getRepository(this, match.repositoryName);
		} catch (IOException e) {
			logger.error("Unable to get repository: " + match.repositoryName, e);
			Toast.makeText(this, R.string.error_managing_repo, Toast.LENGTH_LONG);
		}
		logger.debug("Got repository: {}", vdbRepo_);

		buildUI();
	}

	private void buildUI()
	{
		revView_ = new ExpandableListView(getBaseContext());
		setContentView(revView_);
		revView_.setOnCreateContextMenuListener(this);
		revViewAdapter_ = new BranchExpandableListAdapter(getBaseContext(), vdbRepo_, GroupType.LOCAL_BRANCHES, GroupType.REMOTE_BRANCHES);
		revView_.setAdapter(revViewAdapter_);
		revViewAdapter_.setOnRevisionClickListener(this);
	}

	// Menu item ids
	public static final int MENU_ITEM_ADD = Menu.FIRST;

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ITEM_ADD, 0, getString(R.string.action_add_branch))
		.setShortcut('1', 'a')
		.setIcon(android.R.drawable.ic_menu_add);

		return true;
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_ADD_BRANCH && resultCode == RESULT_OK) {
			revViewAdapter_.notifyDataSetChanged();
		}
	}

	private void runViewActivity(Uri uri)
	{
		try {
			Uri localUri = EntityUriBuilder.toNative(uri);
			logger.debug("Launching native URI: {}", localUri);
			Intent intent = new Intent(Intent.ACTION_VIEW, localUri);
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			try {
				logger.debug("Launching default URI: {}", uri);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			} catch (ActivityNotFoundException e2) {
				logger.error("No activity found.");
				Toast.makeText(this, R.string.error_activity_not_found, Toast.LENGTH_LONG);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_ITEM_ADD) {
			Intent addIntent = new Intent(Actions.ACTION_ADD_BRANCH,
					getIntent().getData());
			startActivityForResult(addIntent, REQUEST_ADD_BRANCH);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRevisionClick(Uri revUri)
	{
		// Short click just open
		logger.debug("Revision click {}", revUri);
		runViewActivity(revUri);
	}

	@Override
	public void onRevisionLongClick(final Uri revUri) {
		logger.debug("Revision long click {}", revUri);
		// Long click show an alert menu
		final CharSequence[] items = {getString(R.string.action_view_branch),
				getString(R.string.action_add_branch), getString(R.string.action_delete_branch)};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.title_branch_action));
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				switch (item) {
				case 0:
					runViewActivity(revUri);
					break;
				case 1:
					Intent addIntent = new Intent(Actions.ACTION_ADD_BRANCH,
							getIntent().getData());
					addIntent.putExtra(AddBranchActivity.EXTRA_SELECTED_VERSION, revUri.toString());
					startActivityForResult(addIntent, REQUEST_ADD_BRANCH);
					break;
				case 2:
					UriMatch match = EntityUriMatcher.getMatch(revUri);
					if (match.type != MatchType.LOCAL_BRANCH) {
						Toast.makeText(ManageLocalBranchesActivity.this, R.string.error_cant_delete_nonlocal_branch, Toast.LENGTH_LONG).show();
					} else {
						try {
							vdbRepo_.deleteBranch(match.reference);
							revViewAdapter_.notifyDataSetChanged();
						} catch (IOException e) {
							Toast.makeText(ManageLocalBranchesActivity.this, R.string.error_delete_branch, Toast.LENGTH_LONG).show();
						}
					}
					break;
				}
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

}
