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
import interdroid.vdb.persistence.content.PeerRegistry;
import interdroid.vdb.persistence.content.PeerRegistry.Peer;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
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

	static final int ACTIVITY_SET_PREFERENCES = 0x10101;

	private EditText mEmail;
	private EditText mName;
	private SimpleAdapter mAdapter;

	private Uri mUri;

	private List<Map<String, Object>> mRepos;

	private VdbProviderRegistry mProviderRegistry;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// TODO: Make this a full edit activity
		Intent intent = getIntent();
		mUri = intent.getData();
		logger.debug("Got request to edit peer: {}", mUri);

		String email;
		String name;

		if (mUri == null) {
			Bundle extras = intent.getExtras();
			email = extras.getString(VdbPreferences.PREF_EMAIL);
			name = extras.getString(VdbPreferences.PREF_NAME);

			if (name == null) {
				Toast.makeText(this, R.string.error_no_name, Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			if (email == null) {
				Toast.makeText(this, R.string.error_no_email, Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			// Does this peer already exist?
			Cursor c = getContentResolver().query(PeerRegistry.URI, null, Peer.EMAIL + "=?", new String[] {email}, null);
			if (c != null && c.moveToFirst()) {
				logger.warn("Request to add existing peer.");
					mUri = Uri.withAppendedPath(PeerRegistry.URI,
							String.valueOf(c.getInt(c.getColumnIndex(Peer._ID))));
			} else {
				logger.debug("Adding to peers: {} {}", name, email);
				ContentValues values = new ContentValues();
				values.put(Peer.NAME, name);
				values.put(Peer.EMAIL, email);
				mUri = getContentResolver().insert(PeerRegistry.URI, values);
			}
			if (c != null) {
				c.close();
			}
		} else {
			logger.debug("Querying for peer.");
			Cursor c = getContentResolver().query(mUri, null, null, null, null);
			if (c != null && c.getCount() == 1 && c.moveToFirst()) {
				name = c.getString(c.getColumnIndex(Peer.NAME));
				email = c.getString(c.getColumnIndex(Peer.EMAIL));
				c.close();
			} else {
				Toast.makeText(this, R.string.error_managing_peer, Toast.LENGTH_LONG).show();
				if (c != null) {
					c.close();
				}
				finish();
				return;
			}
		}

		// TODO: Register preference change listener
		// Make sure name preference exists
		getLocalName(this);

		try {
			mProviderRegistry = new VdbProviderRegistry(this);
		} catch (IOException e) {
			logger.error("Unable to get provider registry: ", e);
			Toast.makeText(this, R.string.error_fetching_repos, Toast.LENGTH_LONG).show();
			finish();
		}

		buildUI(name, email);
	}

	protected void onPause() {
		super.onPause();

		// Save name back to peer.
		ContentValues values = new ContentValues();
		values.put(Peer.NAME, mName.getText().toString());
		getContentResolver().update(mUri, values, null, null);
	}

	static String getLocalName(Activity context) {
		String localName = null;
		SharedPreferences prefs = context.getSharedPreferences(VdbPreferences.PREFERENCES_NAME, MODE_PRIVATE);
		if (prefs.contains(VdbPreferences.PREF_EMAIL)) {
			localName = prefs.getString(VdbPreferences.PREF_EMAIL, null);
		} else {
			Toast.makeText(context, R.string.prefs_not_set, Toast.LENGTH_LONG).show();
			context.startActivityForResult(new Intent(context, VdbPreferences.class), ACTIVITY_SET_PREFERENCES);
		}
		return localName;
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
				addPeerToRepository(this, mName.getText().toString(), mEmail.getText().toString(), repo);
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

	/**
	 * May launch SET_PREFERENCES preference activity for result. Callers must be able to handle this result.
	 *
	 * @param context The activity calling this method
	 * @param userName The users name
	 * @param userEmail The users email used as the address for a ss:/ uri
	 * @param localName Our name on the remote
	 * @param repo The repository to add to
	 * @return true If we successfully added.
	 * @throws URISyntaxException
	 */
	public static boolean addPeerToRepository(Activity context, String userName, String userEmail, VdbRepository repo)
			throws URISyntaxException {
		boolean result = false;
		String localName = getLocalName(context);

		RemoteInfo info = new RemoteInfo();
		info.setType(RemoteInfo.RemoteType.HUB);

		if (userEmail == null || !Repository.isValidRefName("refs/remote/" + userEmail)) {
			Toast.makeText(context, R.string.error_invalid_remote, Toast.LENGTH_LONG).show();
		} else {

			info.setDescription(userName);
			info.setName(userEmail);
			info.setOurNameOnRemote(localName);
			URIish uri = new URIish().setScheme("ss").setHost(userEmail).setPath("/" + repo.getName());
			logger.debug("URI is: {}", uri);
			info.setRemoteUri(uri);
			try {
				repo.saveRemote(info);
				logger.info("Repo added.");
				result = true;
			} catch (Exception e) {
				logger.error("Error saving remote", e);
				Toast.makeText(context, R.string.error_toggling_peer, Toast.LENGTH_LONG).show();
			}
		}
		return result;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		getLocalName(this);
	}
}
