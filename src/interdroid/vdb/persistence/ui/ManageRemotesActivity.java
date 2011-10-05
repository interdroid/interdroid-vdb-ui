package interdroid.vdb.persistence.ui;

import interdroid.vdb.Actions;
import interdroid.vdb.Authority;
import interdroid.vdb.R;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.persistence.api.RemoteInfo;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.content.PeerRegistry;
import interdroid.vdb.persistence.content.PeerRegistry.Peer;
import interdroid.vdb.persistence.ui.EditPeerActivity.PeerInfo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class ManageRemotesActivity extends ListActivity implements OnItemClickListener {
	private static final Logger logger = LoggerFactory
	.getLogger(ManageRemotesActivity.class);

	private VdbRepository vdbRepo_;

	private List<Map<String, Object>> mData;

	private SimpleAdapter mAdapter;
	private static final int REQUEST_MODIFY_REMOTES = 1;
	private static final int REQUEST_ADD_PEER = 2;
	private static final int DIALOG_SYNCHRONIZE = 1;
	private static final int MSG_PROGRESS = 1, MSG_START = 2, MSG_DONE = 3;
	// TODO: This should be a preference?
	private static final int CANCEL_TIMEOUT = 5000 /* ms */;

	private static final String REPO_DESC = "desc";
	private static final String REPO_NAME = "name";
	private static final String REPO_TYPE = "type";
	private static final String REPO_ADDRESS = "addr";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final UriMatch match = EntityUriMatcher.getMatch(intent.getData());

		if (match.type != MatchType.REPOSITORY) {
			throw new RuntimeException("Invalid URI, can only add branches to a repository. "
					+ intent.getData());
		}
		try {
			vdbRepo_ = VdbRepositoryRegistry.getInstance().getRepository(this, match.repositoryName);
		} catch (IOException e) {
			logger.error("Error getting repository", e);
			Toast.makeText(this, R.string.error_opening_repo, Toast.LENGTH_LONG);
			finish();
			return;
		}
		buildUI();
	}

	private void refreshList() {
		updateRemotesMap(mData);
		mAdapter.notifyDataSetChanged();
	}

	private void buildList()
	{
		mData =  updateRemotesMap(new ArrayList<Map<String, Object>>());
		mAdapter = new SimpleAdapter(this, mData,
				R.layout.manage_repo_item,
				new String [] {REPO_TYPE, REPO_NAME, REPO_DESC, REPO_ADDRESS},
				new int [] {R.id.manage_repo_type, R.id.manage_repo_name, R.id.manage_repo_desc, R.id.manage_repo_address});
		setListAdapter(mAdapter);
	}

	private List<Map<String, Object>> updateRemotesMap(List<Map<String, Object>> list) {
		list.clear();
		try {
			for (String remote : vdbRepo_.listRemotes()) {
				final RemoteInfo info = vdbRepo_.getRemoteInfo(remote);
				Map<String, Object> data = new HashMap<String, Object>();
				data.put(REPO_DESC, info.getDescription());
				data.put(REPO_NAME, info.getName());
				logger.debug("Type: {}", info.getRemoteUri().getScheme());
				data.put(REPO_TYPE,
						// TODO: Constant here for SmartSocketsScheme?
						"ss".equals(info.getRemoteUri().getScheme())  ?
								getString(R.string.label_peer) : getString(R.string.label_hub));
				data.put(REPO_ADDRESS, info.getRemoteUri());
				list.add(data);
			}
		} catch (IOException e) {
			Toast.makeText(this, R.string.error_loading_remotes, Toast.LENGTH_LONG).show();
			finish();
		}
		return list;
	}

	private void buildUI()
	{
		buildList();
		getListView().setOnCreateContextMenuListener(this);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		getListView().setOnItemClickListener(this);

		syncDialog_ = new ProgressDialog(this);
		syncDialog_.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		syncDialog_.setTitle("Synchronizing");
		syncDialog_.setMessage("");
		syncDialog_.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
				new AlertDialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				syncThread_.cancel();
				syncDialog_.cancel();
			}
		});
		syncDialog_.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
				new AlertDialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				syncDialog_.dismiss();
			}
		});
	}

	// Menu item ids
	public static final int MENU_ITEM_ADD_PEER = Menu.FIRST;
	public static final int MENU_ITEM_ADD_HUB = Menu.FIRST + 1;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ITEM_ADD_PEER, 0, getString(R.string.action_add_peer))
		.setShortcut('1', 'h')
		.setIcon(android.R.drawable.ic_menu_add);

		menu.add(1, MENU_ITEM_ADD_HUB, 0, getString(R.string.action_add_hub))
		.setShortcut('2', 'p')
		.setIcon(android.R.drawable.ic_menu_add);

		return true;
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (requestCode == EditPeerActivity.ACTIVITY_SET_PREFERENCES) {
			EditPeerActivity.getLocalName(this);
		} else if (requestCode == REQUEST_ADD_PEER) {
			if (resultCode == RESULT_OK) {
				try {
					PeerInfo peerInfo = new PeerInfo(data.getStringExtra(Peer.NAME), data.getStringExtra(Peer.EMAIL), data.getStringExtra(Peer.DEVICE));
					EditPeerActivity.addPeerToRepository(this, peerInfo, vdbRepo_);
					refreshList();
				} catch (URISyntaxException e) {
					logger.error("URI Syntax error while adding peer.", e);
					Toast.makeText(this, R.string.error_toggling_peer, Toast.LENGTH_LONG).show();
				}
			}
		} else if (requestCode == REQUEST_MODIFY_REMOTES) {
			refreshList();
		}
	}

	private ProgressDialog syncDialog_;
	private SyncThread syncThread_;

	final Handler syncHandler_ = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_PROGRESS:
				syncDialog_.setMax(msg.arg2);
				syncDialog_.setProgress(msg.arg1);
				syncDialog_.setMessage(msg.obj.toString());
				break;
			case MSG_START:
				syncDialog_.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
				syncDialog_.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				break;
			case MSG_DONE:
				syncDialog_.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
				syncDialog_.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
				syncDialog_.setMessage(msg.obj.toString());
				break;
			}
		}
	};

	private class SyncThread extends Thread implements ProgressMonitor {
		private boolean wasCanceled_, isDone_, startMessageSent_;
		private Handler uiHandler_;
		private String taskTitle_ = "Connecting";
		private int taskWork_ = 0, taskTotalWork_ = 1;
		private int currentTask_ = 0, totalTasks_ = 1;
		private String remoteName_;
		private long killTimeMillis;

		public SyncThread(Handler uiHandler, String remoteName)
		{
			uiHandler_ = uiHandler;
			remoteName_ = remoteName;
		}

		public void cancel()
		{
			wasCanceled_ = true;
			killTimeMillis = System.currentTimeMillis() + CANCEL_TIMEOUT;
			updateUi();
			uiHandler_ = null;
		}

		public boolean isDoneOrCanceled()
		{
			if (isDone_) {
				return true;
			}
			if (wasCanceled_ && isAlive()) {
				if (killTimeMillis < System.currentTimeMillis()) {
					interrupt();
					return true;
				} else {
					// If we have a stale thread .. we should try to interrupt it at least.
					return false;
				}
			}
			return wasCanceled_;
		}

		@Override
		public void run()
		{
			updateUi();
			try {
				vdbRepo_.pullFromRemote(remoteName_, this);
				vdbRepo_.pushToRemote(remoteName_, this);
				taskTitle_ = "Done";
			} catch(IOException e) {
				taskTitle_ = e.getMessage();
			}
			// we use a separate isDone_ flag next to the normal isAlive
			// in order to prevent race conditions, our last updateUi has to
			// arrive when we are finishing.
			isDone_ = true;
			updateUi();
		}

		private void updateUi()
		{
			if (uiHandler_ == null) {
				// in case of cancel we may have disconnected from the UI
				return;
			}
			if (!startMessageSent_) {
				uiHandler_.obtainMessage(MSG_START).sendToTarget();
				startMessageSent_ = true;
			}
			if (isDone_) {
				uiHandler_.obtainMessage(MSG_DONE, wasCanceled_ ? "Sync canceled" : taskTitle_)
				.sendToTarget();
			} else {
				String msg = String.format("[%d/%d] %s", currentTask_, totalTasks_, taskTitle_);
				uiHandler_.obtainMessage(MSG_PROGRESS, taskWork_, taskTotalWork_, msg)
				.sendToTarget();
			}
		}

		@Override
		public void beginTask(String title, int totalWork)
		{
			taskTitle_ = title;
			taskWork_ = 0;
			taskTotalWork_ = totalWork < 1 ? 1 : totalWork;
			updateUi();
		}

		@Override
		public void endTask()
		{
			taskWork_ = taskTotalWork_;
			++currentTask_;
			updateUi();
		}

		@Override
		public boolean isCancelled()
		{
			return wasCanceled_;
		}

		@Override
		public void start(int totalTasks)
		{
			totalTasks_ = totalTasks < 1 ? 1 : totalTasks;
			updateUi();
		}

		@Override
		public void update(int completed)
		{
			taskWork_ += completed;
			taskWork_ = taskWork_ < 0 ? 0 : taskWork_;
			taskWork_ = taskWork_ > taskTotalWork_ ? taskTotalWork_ : taskWork_;
			updateUi();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_SYNCHRONIZE:
			return syncDialog_;
		default:
			return null;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_ITEM_ADD_PEER) {
			Intent addPeerIntent = new Intent(Intent.ACTION_PICK, PeerRegistry.URI);
			startActivityForResult(addPeerIntent, REQUEST_ADD_PEER);
			return true;
		}
		if (item.getItemId() == MENU_ITEM_ADD_HUB) {
			Intent addHubIntent = new Intent(Actions.ACTION_ADD_REMOTE,
					getIntent().getData());
			startActivityForResult(addHubIntent, REQUEST_MODIFY_REMOTES);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		final int EDIT = 0;
		final int SYNC = 1;
		final int REMOVE = 2;

		@SuppressWarnings("unchecked")
		final Map<String, Object> data = (Map<String, Object>) getListAdapter().getItem(position);
		final String remote = (String) data.get(REPO_NAME);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.title_peer_actions));
		final Intent editIntent;

		CharSequence[] items;
		if (data.get(REPO_TYPE).equals(getString(R.string.label_peer))) {
			items = new String[] {getString(R.string.action_edit_peer),
					getString(R.string.action_sync_peer), getString(R.string.action_remove_peer)};
			editIntent = new Intent(Intent.ACTION_EDIT, PeerRegistry.URI);
		} else {
			items = new String[] {getString(R.string.action_edit_hub),
					getString(R.string.action_sync_hub), getString(R.string.action_remove_hub)};
			editIntent = new Intent(Intent.ACTION_EDIT,
					EntityUriBuilder.remoteUri(Authority.VDB, vdbRepo_.getName(), remote));
		}

		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				switch (item) {
				case EDIT:
					startActivityForResult(editIntent, REQUEST_MODIFY_REMOTES);
					break;
				case SYNC:
					// TODO: Queue sync with service instead.
					if (syncThread_ != null && !syncThread_.isDoneOrCanceled()) {
						// do not allow multiple syncs at the same time
						Toast.makeText(ManageRemotesActivity.this, R.string.error_sync_in_progress, Toast.LENGTH_LONG).show();
					} else {
						syncThread_ = new SyncThread(syncHandler_, remote);
						syncThread_.start();
						showDialog(DIALOG_SYNCHRONIZE);
					}
					break;
				case REMOVE:
					try {
						vdbRepo_.deleteRemote(remote);
						refreshList();
					} catch (IOException e) {
						logger.error("Unable to remove remote.", e);
						Toast.makeText(ManageRemotesActivity.this, R.string.error_removing_remote, Toast.LENGTH_LONG).show();
					}
					break;
				}
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

}
