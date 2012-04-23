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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

public class ManageRepositoryPropertiesActivity extends Activity {
	private static final Logger logger = LoggerFactory
		.getLogger(ManageRepositoryPropertiesActivity.class);

	private CheckBox mIsPublic;
	private VdbRepository mRepository;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mRepository = getRepository( getUriMatchFromIntent( getIntent() ) );
		buildUI();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mIsPublic.setChecked(mRepository.isPublic());
	}

	private UriMatch getUriMatchFromIntent(Intent intent) {
		logger.debug("Managing repository: {}", intent.getData());
		UriMatch match = EntityUriMatcher.getMatch(intent.getData());

		if (match.type != MatchType.REPOSITORY) {
			throw new RuntimeException("Not a vdb repository URI: "
					+ intent.getData());
		}
		return match;
	}

	private void buildUI() {
		setContentView(R.layout.manage_repo_properties);
		mIsPublic = (CheckBox) findViewById(R.id.is_public);
		mIsPublic.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				logger.debug("Setting repo public: {} {}", mRepository.getName(), isChecked);
				try {
					mRepository.setIsPublic(isChecked);
				} catch (IOException e) {
					Toast.makeText(ManageRepositoryPropertiesActivity.this, getText(R.string.error_toggling_is_public), Toast.LENGTH_LONG).show();
					mIsPublic.setChecked(!isChecked);
				}
			}
		});
	}

	private VdbRepository getRepository(UriMatch match) {
		logger.debug("Getting repository for: {}", match.repositoryName);
		VdbRepository repo = null;
		try {
			repo = VdbRepositoryRegistry.getInstance().getRepository(this, match.repositoryName);
		} catch (IOException e) {
			logger.error("Unable to get repository: " + match.repositoryName, e);
			Toast.makeText(this, R.string.error_managing_repo, Toast.LENGTH_LONG);
		}
		logger.debug("Got repository: {}", repo);

		return repo;
	}

}
