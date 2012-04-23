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
import java.net.URISyntaxException;
import java.util.Map;

import interdroid.vdb.R;
import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class EditLocalSharedRepositoriesActivity extends BaseEditRepositoryActivity implements OnItemClickListener {
	static final Logger logger = LoggerFactory
			.getLogger(EditLocalSharedRepositoriesActivity.class);

	@Override
	protected void refreshList() {
		mRepos = getLocalRepositories(mPeerName);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		logger.info("Got onItemClick click on: {} {}", view.getId(), position);
		Map<String, Object> data = mRepos.get(position);
		boolean isPeer = (Boolean) data.get(VdbProviderRegistry.REPOSITORY_IS_PEER);
		logger.debug("repoIsPeer: " + R.id.repoIsPeer + " Public: " + R.id.repoIsPublic + " view: " + view.getId());
		try {
			VdbRepository repo = VdbRepositoryRegistry.getInstance()
					.getRepository(this, (String) data.get(VdbProviderRegistry.REPOSITORY_NAME));
			if (isPeer) {
				String remoteName = VdbPreferences.makeLocalName(mPeerInfo.device, mPeerInfo.email);
				repo.deleteRemote(remoteName);

				// TODO: We should queue up a message to the remote letting them know not to push to us anymore.

			} else {
				EditPeerActivity.addPeerToRepository(this, mPeerInfo, repo);
			}
			data.put(VdbProviderRegistry.REPOSITORY_IS_PEER, !isPeer);
			mAdapter.notifyDataSetChanged();
		} catch (IOException e) {
			logger.error("Unable to add peer.", e);
			Toast.makeText(this, R.string.error_toggling_peer, Toast.LENGTH_LONG).show();
		} catch (URISyntaxException e) {
			logger.error("Unable to add peer.", e);
			Toast.makeText(this, R.string.error_toggling_peer, Toast.LENGTH_LONG).show();
		}
	}
}
