package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.smartsockets.util.MalformedAddressException;
import ibis.smartsockets.virtual.InitializationException;
import interdroid.util.ToastOnUI;
import interdroid.vdb.R;
import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.transport.SmartSocketsTransport;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class EditRemoteSharedRepositoriesActivity extends BaseEditRepositoryActivity implements OnItemClickListener {
	private static final Logger logger = LoggerFactory
			.getLogger(EditRemoteSharedRepositoriesActivity.class);

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		logger.info("Got onItemClick click on: {} {}", view.getId(), position);
		Map<String, Object> data = mRepos.get(position);
		boolean isPeer = (Boolean) data.get(VdbProviderRegistry.REPOSITORY_IS_PEER);
		logger.debug("repoIsPeer: " + R.id.repoIsPeer + " Public: " + R.id.repoIsPublic + " view: " + view.getId());
		try {
			VdbRepository repo = VdbRepositoryRegistry.getInstance()
					.getRepository(this, (String) data.get(VdbProviderRegistry.REPOSITORY_NAME));
			String remoteName = VdbPreferences.makeLocalName(mPeerInfo.device, mPeerInfo.email);

			if (isPeer) {
				if (repo != null) {
					repo.deleteRemote(remoteName);
				}
				data.put(VdbProviderRegistry.REPOSITORY_IS_PEER, !isPeer);
				mAdapter.notifyDataSetChanged();
			} else {
				if (repo == null) {
					// We need to clone the repository from them
					logger.debug("Creating repository.");
					VdbRepositoryRegistry.getInstance().addRepository(this, (String)data.get(VdbProviderRegistry.REPOSITORY_NAME), null);
					// Now pull the repo out
					logger.debug("Fetching repository.");
					repo = VdbRepositoryRegistry.getInstance()
							.getRepository(this, (String) data.get(VdbProviderRegistry.REPOSITORY_NAME));
					// Add them as a peer that we push to.
					logger.debug("Adding a peer");
					EditPeerActivity.addPeerToRepository(this, mPeerInfo, repo);
					// Set should it be public
					repo.setIsPublic((Boolean)data.get(VdbProviderRegistry.REPOSITORY_IS_PUBLIC));
					logger.debug("Pulling from remote.");
					// TODO: Make this an async task with progress dialog.
					repo.pullFromRemote(remoteName, null);
				} else {
					// Add them as a peer that we push to.
					EditPeerActivity.addPeerToRepository(this, mPeerInfo, repo);
				}
			}

		} catch (IOException e) {
			logger.error("Unable to add peer.", e);
			Toast.makeText(this, R.string.error_toggling_peer, Toast.LENGTH_LONG).show();
		} catch (URISyntaxException e) {
			logger.error("Unable to add peer.", e);
			Toast.makeText(this, R.string.error_toggling_peer, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void refreshList() {
		String localName = EditPeerActivity.getLocalName(this);
		mRepos = new ArrayList<Map<String, Object>>();
		try {
			mRepos = SmartSocketsTransport.getRepositories(localName, mPeerName);
		} catch (MalformedAddressException e) {
			logger.error("Inavlid peer name.", e);
			ToastOnUI.show(this, R.string.error_invalid_uri, Toast.LENGTH_LONG);
		} catch (IOException e) {
			logger.error("IOError communicating with peer", e);
			ToastOnUI.show(this, R.string.error_contacting_peer, Toast.LENGTH_LONG);
		} catch (InitializationException e) {
			logger.error("Initializing communication error", e);
			ToastOnUI.show(this, R.string.error_contacting_peer, Toast.LENGTH_LONG);
		}

	}
}
