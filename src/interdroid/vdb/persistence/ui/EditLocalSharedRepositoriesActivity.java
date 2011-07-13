package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import interdroid.vdb.R;
import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.ui.EditPeerActivity.PeerInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class EditLocalSharedRepositoriesActivity extends Activity implements OnItemClickListener {
	private static final Logger logger = LoggerFactory
			.getLogger(EditLocalSharedRepositoriesActivity.class);

	public static final String EMAIL = "email";
	public static final String DEVICE = "device";

	private SimpleAdapter mAdapter;
	private List<Map<String, Object>> mRepos;

	private VdbProviderRegistry mProviderRegistry;

	private PeerInfo mPeerInfo;

	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		setupRegistry();
		buildUI();
	}

	public void onResume() {
		super.onResume();

		refreshList();
	}

	private void setupRegistry() {
		try {
			mProviderRegistry = new VdbProviderRegistry(this);
		} catch (IOException e) {
			logger.error("Unable to get provider registry: ", e);
			Toast.makeText(this, R.string.error_fetching_repos, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private void buildUI() {
		setContentView(R.layout.list_local_repositories);

		getListView().setClickable(true);
		getListView().setOnItemClickListener(this);
	}

	private void refreshList() {
		mPeerInfo = EditPeerActivity.getPeerInfo(this, getIntent().getData(), getIntent().getExtras());
		String remoteName = VdbPreferences.makeLocalName(mPeerInfo.device, mPeerInfo.email);

		mRepos = getAllRepos(remoteName);
		mAdapter = new SimpleAdapter(this, mRepos, R.layout.repo_peer_item,
				new String[] {VdbProviderRegistry.REPOSITORY_IS_PUBLIC, VdbProviderRegistry.REPOSITORY_IS_PEER, VdbProviderRegistry.REPOSITORY_NAME},
				new int[] {R.id.repoIsPublic, R.id.repoIsPeer, R.id.repoName}) {

			public boolean areAllItemsEnabled() {
				logger.debug("All items enabled!");
				return true;
			}

			public boolean isEnabled(int position) {
				logger.debug("Item enabled: {}", position);
				return true;
			}
		};
		setListAdapter(mAdapter);
	}

	private void setListAdapter(SimpleAdapter adapter) {
		getListView().setAdapter(adapter);
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

	private List<Map<String, Object>> getAllRepos(String email) {
		List<Map<String, Object>> result = null;
		try {
			result = mProviderRegistry.getAllRepositories(email);
		} catch (IOException e) {
			logger.error("Error fetching list of repositories: ", e);
			Toast.makeText(this, R.string.error_fetching_repos, Toast.LENGTH_LONG).show();
			finish();
		}
		return result;
	}

	private ListView getListView() {
		return (ListView) findViewById(R.id.add_peer_repo_list);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Ensure that the local name is set properly.
		EditPeerActivity.getLocalName(this);
	}
}
