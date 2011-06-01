package interdroid.vdb.persistence.ui;

import interdroid.vdb.Actions;
import interdroid.vdb.R;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ManageRemotesActivity extends ListActivity {
	private static final Logger logger = LoggerFactory
			.getLogger(ManageRemotesActivity.class);

	private VdbRepository vdbRepo_;
	private static final int REQUEST_MODIFY_REMOTES = 1;
	private static final int DIALOG_SYNCHRONIZE = 1;
	private static final int MSG_PROGRESS = 1, MSG_START = 2, MSG_DONE = 3;
	private static final int CANCEL_TIMEOUT = 5000 /* ms */;

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

	private List<String> buildRemotesList()
	{
		Set<String> remotes;
		try {
			remotes = vdbRepo_.listRemotes();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return Collections.list(Collections.enumeration(remotes));
	}

	private void refreshList()
	{
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, buildRemotesList());
		setListAdapter(adapter);
	}

	private void buildUI()
	{
		refreshList();
		getListView().setOnCreateContextMenuListener(this);
    	getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

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
	public static final int MENU_ITEM_DELETE = Menu.FIRST;
    public static final int MENU_ITEM_ADD = Menu.FIRST + 1;
    public static final int MENU_ITEM_EDIT = Menu.FIRST + 2;
    public static final int MENU_ITEM_SYNC = Menu.FIRST + 3;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_ITEM_ADD, 0, "Add")
		        .setShortcut('3', 'a')
		        .setIcon(android.R.drawable.ic_menu_add);

        menu.add(1, MENU_ITEM_EDIT, 0, "Edit")
		        .setShortcut('0', 'e')
		        .setIcon(android.R.drawable.ic_menu_edit);

        menu.add(2, MENU_ITEM_SYNC, 0, "Sync")
		        .setShortcut('0', 's')
		        .setIcon(android.R.drawable.ic_menu_share);

        menu.add(3, MENU_ITEM_DELETE, 0, "Delete")
		        .setShortcut('0', 'd')
		        .setIcon(android.R.drawable.ic_menu_delete);

        return true;
    }

    @Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK) {
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
    	if (item.getItemId() == MENU_ITEM_ADD) {
        	Intent addIntent = new Intent(Actions.ACTION_ADD_REMOTE,
        			getIntent().getData());
            startActivityForResult(addIntent, REQUEST_MODIFY_REMOTES);
            return true;
    	}
    	// otherwise we need a selection
    	if (getSelectedItemPosition() < 0) {
    		return true;
    	}
    	String remote = (String) getListAdapter().getItem(getSelectedItemPosition());
        switch (item.getItemId()) {
        case MENU_ITEM_EDIT:
        	Intent editIntent = new Intent(Intent.ACTION_EDIT,
        			EntityUriBuilder.remoteUri(VdbMainContentProvider.AUTHORITY, vdbRepo_.getName(), remote));
            startActivityForResult(editIntent, REQUEST_MODIFY_REMOTES);
            return true;
        case MENU_ITEM_DELETE:
        	try {
				vdbRepo_.deleteRemote(remote);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			refreshList();
        	return true;
        case MENU_ITEM_SYNC:
        	if (syncThread_ != null && !syncThread_.isDoneOrCanceled()) {
        		// do not allow multiple syncs at the same time
        		return true;
        	}
        	syncThread_ = new SyncThread(syncHandler_, remote);
        	syncThread_.start();
        	showDialog(DIALOG_SYNCHRONIZE);
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
