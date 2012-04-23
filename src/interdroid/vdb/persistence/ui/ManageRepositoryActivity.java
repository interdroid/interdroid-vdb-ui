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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.Actions;
import interdroid.vdb.R;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class ManageRepositoryActivity extends TabActivity {
	private static final Logger logger = LoggerFactory
			.getLogger(ManageRepositoryActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		logger.debug("Managing repository: {}", intent.getData());
		UriMatch match = EntityUriMatcher.getMatch(intent.getData());

		if (match.type != MatchType.REPOSITORY) {
			throw new RuntimeException("Not a vdb repository URI: "
					+ intent.getData());
		}

		setTitle(getText(R.string.title_manage_repository) + " " + match.repositoryName);

		final TabHost tabHost = getTabHost();

		tabHost.addTab(tabHost.newTabSpec("branches")
				.setIndicator(getString(R.string.label_branches))
				.setContent(new Intent(
						Actions.ACTION_MANAGE_LOCAL_BRANCHES,
						intent.getData())));

		tabHost.addTab(tabHost.newTabSpec("peers")
				.setIndicator(getString(R.string.label_peers))
				.setContent(new Intent(
						Actions.ACTION_MANAGE_REMOTES,
						intent.getData())));

		tabHost.addTab(tabHost.newTabSpec("properties")
				.setIndicator(getString(R.string.label_sharing))
				.setContent(new Intent(
						Actions.ACTION_MANAGE_REPOSITORY_PROPERTIES,
						intent.getData())));

	}

}
