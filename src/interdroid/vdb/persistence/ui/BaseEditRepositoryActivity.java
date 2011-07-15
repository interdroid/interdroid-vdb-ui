package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.util.ToastOnUI;
import interdroid.util.view.AsyncTaskWithProgressDialog;
import interdroid.vdb.R;
import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.persistence.ui.EditPeerActivity.PeerInfo;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public abstract class BaseEditRepositoryActivity extends Activity implements OnItemClickListener {
	private static final Logger logger = LoggerFactory
			.getLogger(BaseEditRepositoryActivity.class);

	protected PeerInfo mPeerInfo;
	protected String mPeerName;
	protected VdbProviderRegistry mProviderRegistry;

	protected SimpleAdapter mAdapter;

	protected List<Map<String, Object>> mRepos;

	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		buildUI();
		setupRegistry();

		mPeerInfo = EditPeerActivity.getPeerInfo(this, getIntent().getData(), getIntent().getExtras());
		mPeerName = VdbPreferences.makeLocalName(mPeerInfo.device, mPeerInfo.email);
	}

	public void onResume() {
		super.onResume();

		new AsyncTaskWithProgressDialog<Void, Void, Void>(this, getString(R.string.label_loading), getString(R.string.label_wait)) {

			@Override
			protected Void doInBackground(Void... params) {
				refreshList();
				return null;
			}

			@Override
			protected void onPostExecute(Void v) {
				resetListAdapter();
				super.onPostExecute(v);
			}

		}.execute();
	}

	protected abstract void refreshList();

	private void buildUI() {
		setContentView(R.layout.list_peer_repositories);

		getListView().setClickable(true);
		getListView().setOnItemClickListener(this);
	}

	protected void setListAdapter(SimpleAdapter adapter) {
		getListView().setAdapter(adapter);
	}

	private ListView getListView() {
		return (ListView) findViewById(R.id.repo_list);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Ensure that the local name is set properly.
		EditPeerActivity.getLocalName(this);
	}

	protected void setupRegistry() {
		try {
			mProviderRegistry = new VdbProviderRegistry(this);
		} catch (IOException e) {
			logger.error("Unable to get provider registry: ", e);
			ToastOnUI.show(BaseEditRepositoryActivity.this, R.string.error_fetching_repos, Toast.LENGTH_LONG);
			finish();
		}
	}

	protected List<Map<String, Object>> getLocalRepositories(String email) {
		List<Map<String, Object>> result = null;
		try {
			result = mProviderRegistry.getAllRepositories(email);
		} catch (IOException e) {
			logger.error("Error fetching list of repositories: ", e);

			finish();
		}
		return result;
	}

	protected void resetListAdapter() {
		mAdapter = new SimpleAdapter(this, mRepos, R.layout.list_repo_peer_item,
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

}