package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;
import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.persistence.api.RemoteInfo;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class EditPeerActivity extends Activity implements OnItemClickListener {
	private static final Logger logger = LoggerFactory
	.getLogger(EditPeerActivity.class);

	private static final int SET_PREFERENCES = 0;

	private EditText mEmail;
	private EditText mName;
	private SimpleAdapter mAdapter;

	private List<Map<String, Object>> mRepos;

	private VdbProviderRegistry mProviderRegistry;

	private String mLocalName;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// TODO: Make this a full edit activity
		String email = getIntent().getExtras().getString(VdbPreferences.PREF_EMAIL);
		String name = getIntent().getExtras().getString(VdbPreferences.PREF_NAME);
		if (name == null) {
			Toast.makeText(this, R.string.error_no_name, Toast.LENGTH_LONG).show();
			finish();
		}
		if (email == null) {
			Toast.makeText(this, R.string.error_no_email, Toast.LENGTH_LONG).show();
			finish();
		}
		try {
			mProviderRegistry = new VdbProviderRegistry(this);
		} catch (IOException e) {
			logger.error("Unable to get provider registry: ", e);
			Toast.makeText(this, R.string.error_fetching_repos, Toast.LENGTH_LONG).show();
			finish();
		}
		// TODO: Register preference change listener
		setupLocalName();

		buildUI(name, email);
	}

	private void setupLocalName() {
		SharedPreferences prefs = getSharedPreferences(VdbPreferences.PREFERENCES_NAME, MODE_PRIVATE);
		if (prefs.contains(VdbPreferences.PREF_EMAIL)) {
			mLocalName = prefs.getString(VdbPreferences.PREF_EMAIL, null);
			if (mLocalName == null
					|| !Repository.isValidRefName("refs/remote/" + mLocalName)) {
				Toast.makeText(this, R.string.prefs_not_set, Toast.LENGTH_LONG).show();
				startActivityForResult(new Intent(this, VdbPreferences.class), SET_PREFERENCES);
			}
		} else {
			Toast.makeText(this, R.string.prefs_not_set, Toast.LENGTH_LONG).show();
			startActivityForResult(new Intent(this, VdbPreferences.class), SET_PREFERENCES);
		}
	}

	private ListView getListView() {
		return (ListView) findViewById(R.id.add_peer_repo_list);
	}

	private void buildUI(String name, String email)
	{
		setTitle(R.string.title_manage_peer);
		setContentView(R.layout.edit_peer);

		// Has to come before first refresh to setup header before adapter is set.
		setupListHeader(name, email);
		refreshList(email);

		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				logger.error("Got onCreateContextMenu", v);
			}

		});
		getListView().setClickable(true);
		getListView().setOnItemClickListener(this);
	}

	private void refreshList(String email) {
		mRepos = getAllRepos(email);

		mAdapter = new SimpleAdapter(this, mRepos, R.layout.repo_peer_item,
				new String[] {VdbProviderRegistry.REPOSITORY_IS_PEER, VdbProviderRegistry.REPOSITORY_NAME},
				new int[] {R.id.repoIsPeer, R.id.repoName}) {

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

	private void setupListHeader(String name, String email) {
		mEmail = (EditText)findViewById(R.id.add_peer_email);
		mEmail.setText(email);
		mEmail.setEnabled(false);
		mEmail.setFocusable(false);
		mName = (EditText)findViewById(R.id.add_peer_name);

		mName.setText(name);
		mName.setEnabled(true);
		mName.setFocusable(true);
	}

	private List<Map<String, Object>> getAllRepos(String email) {
		List<Map<String, Object>> result = null;
		try {
			result = mProviderRegistry.getPeerRepositories(email);
		} catch (IOException e) {
			logger.error("Error fetching list of repositories: ", e);
			Toast.makeText(this, R.string.error_fetching_repos, Toast.LENGTH_LONG).show();
			finish();
		}
		return result;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		logger.info("Got onItemClick click on: {} {}", id, position);
		Map<String, Object> data = mRepos.get(position);
		boolean isPeer = (Boolean) data.get(VdbProviderRegistry.REPOSITORY_IS_PEER);
		try {
			VdbRepository repo = VdbRepositoryRegistry.getInstance()
			.getRepository(this, (String) data.get(VdbProviderRegistry.REPOSITORY_NAME));
			if (isPeer) {
				repo.deleteRemote(mEmail.getText().toString());
			} else {
				RemoteInfo info = new RemoteInfo();
				info.setType(RemoteInfo.RemoteType.HUB);

				String desc = mName.getText().toString();
				String name = mEmail.getText().toString();
				if (name == null || !Repository.isValidRefName("refs/remote/" + name)) {
					Toast.makeText(this, "Invalid remote name.", Toast.LENGTH_LONG).show();
					return;
				}

				info.setDescription(desc);
				info.setName(name);
				info.setOurNameOnRemote(mLocalName);
				info.setRemoteUri(new URIish("ss://" + mEmail.getText().toString()));
				try {
					repo.saveRemote(info);
					logger.info("Repo added.");
				} catch (Exception e) {
					logger.error("Error saving remote", e);
					Toast.makeText(this, R.string.error_toggling_peer, Toast.LENGTH_LONG).show();
				}
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

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		setupLocalName();
	}
}