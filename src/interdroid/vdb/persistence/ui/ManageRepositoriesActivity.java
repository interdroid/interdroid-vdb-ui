package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.util.view.AsyncTaskWithProgressDialog;
import interdroid.vdb.Actions;
import interdroid.vdb.R;
import interdroid.vdb.avro.AvroSchema;
import interdroid.vdb.avro.view.AvroBaseEditor;
import interdroid.vdb.avro.view.AvroBaseList;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.content.VdbProviderRegistry;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class ManageRepositoriesActivity extends ListActivity {
	private static final Logger logger = LoggerFactory
			.getLogger(ManageRepositoriesActivity.class);

	private List<Map<String, Object>> repos;
	private SimpleAdapter mAdapter;

	// Intent requests
	private static final int CREATE_REPOSITORY = 1;
	private static final int PICK_BRANCH = 2;
	private static final int INIT_DB = 3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		new AsyncTaskWithProgressDialog<Void, Void, Void>(this, getString(R.string.label_loading), getString(R.string.label_wait)) {

			@Override
			protected Void doInBackground(Void... params) {
				startService(new Intent(Actions.GIT_SERVICE));
				return null;
			}

		}.execute();

		// Inform the list we provide context menus for items
		getListView().setOnCreateContextMenuListener(this);

		buildUI();
	}

	protected void onResume() {
		super.onResume();
		repos = getAllRepos();
		mAdapter = new SimpleAdapter(this, repos, R.layout.repo_item,
				new String[] {VdbProviderRegistry.REPOSITORY_NAME}, new int[] {R.id.repoName});
		setListAdapter(mAdapter);
	}

	private void buildUI() {
		setTitle(getString(R.string.title_manage_repositories));
	}

	private List<Map<String, Object>> getAllRepos() {
		List<Map<String, Object>> result = null;
		try {
			result = new VdbProviderRegistry(this).getAllRepositories();
		} catch (IOException e) {
			logger.error("Error fetching list of repositories: ", e);
		}
		return result;
	}

	private static final int OPEN_MASTER = 0;
	private static final int OPEN_BRANCH = 1;
	private static final int EDIT_SCHEMA = 2;
	private static final int MANAGE_REPO = 3;
	private static final int DELETE_REPO = 4;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		// Setup the menu header
		menu.setHeaderTitle(getString(R.string.title_repository_action));
		menu.add(0, OPEN_MASTER, 0, getString(R.string.action_open_repo_master));
		menu.add(0, OPEN_BRANCH, 0, getString(R.string.action_open_repo_branch_or_commit));
		menu.add(0, EDIT_SCHEMA, 0, getString(R.string.action_edit_repo_schema));
		menu.add(0, MANAGE_REPO, 0, getString(R.string.action_manage_repo));
		menu.add(0, DELETE_REPO, 0, getString(R.string.action_delete_repo));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			logger.error("bad menuInfo", e);
			return false;
		}

		final String repoName = (String) repos.get((int)info.id).get(VdbProviderRegistry.REPOSITORY_NAME);

		switch (item.getItemId()) {
		case OPEN_MASTER:
			try {
				launchListActivity(EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY, repoName, "master"));
			} catch (Exception e) {
				logger.error("Error opening repository", e);
				Toast.makeText(ManageRepositoriesActivity.this, getString(R.string.error_opening_repo), Toast.LENGTH_LONG).show();
			}
			break;
		case OPEN_BRANCH:
			try {
				logger.debug("Launching pick activity for: {}", repoName);
				startActivityForResult(new Intent(Intent.ACTION_PICK,
						EntityUriBuilder.repositoryUri(VdbMainContentProvider.AUTHORITY, repoName)),
						PICK_BRANCH);
			} catch (Exception e) {
				logger.error("Error opening repository", e);
				Toast.makeText(ManageRepositoriesActivity.this, getString(R.string.error_opening_repo), Toast.LENGTH_LONG).show();
			}
			break;
		case EDIT_SCHEMA:
			Cursor c = null;
			try {
				c = getContentResolver().query(
						EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY, AvroSchema.NAMESPACE, "master/" + AvroSchema.RECORD_DEFINITION),
					new String[] {"_id"}, "namespace=?", new String[] {repoName}, null);
				if (c != null && c.moveToFirst()) {
					try {
						launchEditSchemaActivity(c.getString(0));
					}catch (Exception e) {
						logger.error("Error editing repository schema", e);
						Toast.makeText(ManageRepositoriesActivity.this, getString(R.string.error_editing_repo_schema), Toast.LENGTH_LONG).show();
					}
				}
			} finally {
				try {
					if (c != null) {
						c.close();
					}
				} catch (Exception e) {
					logger.error("Caught exception closing cursor.", e);
				}
			}
			break;
		case MANAGE_REPO:
			try {
				logger.debug("Launching manage for: {}", repoName);
				startActivity(new Intent(Actions.ACTION_MANAGE_REPOSITORY,
						EntityUriBuilder.repositoryUri(VdbMainContentProvider.AUTHORITY, repoName)));
			} catch (Exception e) {
				logger.error("Error managing repository", e);
				Toast.makeText(ManageRepositoriesActivity.this, getString(R.string.error_managing_repo), Toast.LENGTH_LONG).show();
			}
			break;
		case DELETE_REPO:
			// TODO: Add support for deleting repositories
			Toast.makeText(ManageRepositoriesActivity.this, "Not yet supported", Toast.LENGTH_LONG).show();
			break;
		default:
			logger.error("Unknown context menu option: {}", info);
			return false;
		}
		return true;
	}

	protected void onListItemClick(ListView lv, View v, int position, long id) {

		final String repoName = (String) repos.get(position).get(VdbProviderRegistry.REPOSITORY_NAME);

		try {
			launchListActivity(EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY, repoName, "master"));
		} catch (Exception e) {
			logger.error("Error opening repository", e);
			Toast.makeText(ManageRepositoriesActivity.this, getString(R.string.error_opening_repo), Toast.LENGTH_LONG).show();
		}
	}

	// Menu item ids
	public static final int MENU_ITEM_ADD = Menu.FIRST;
	public static final int MENU_ITEM_PREFS = Menu.FIRST + 1;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ITEM_ADD, 0, "Add")
		.setShortcut('1', 'a')
		.setIcon(android.R.drawable.ic_menu_add);

		menu.add(0, MENU_ITEM_PREFS, 0, "Preferences")
		.setShortcut('2', 'p')
		.setIcon(android.R.drawable.ic_menu_preferences);

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_ADD:
			launchEditSchemaActivity(null);
			return true;
		case MENU_ITEM_PREFS:
			Intent prefsIntent = new Intent(Intent.ACTION_EDIT);
			prefsIntent.setClass(this, VdbPreferences.class);
			startActivity(prefsIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void launchEditSchemaActivity(String id) {
		logger.debug("Editing record: {}", id);
		Intent addIntent = new Intent(id == null ? Intent.ACTION_INSERT : Intent.ACTION_EDIT,
				EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY, AvroSchema.NAMESPACE, "master"));
		addIntent.putExtra(AvroBaseEditor.ENTITY, AvroSchema.RECORD_DEFINITION + (id == null ? "" : "/" + id));
		addIntent.putExtra(AvroBaseEditor.SCHEMA, AvroSchema.RECORD.toString());
		logger.debug("Adding Repository");
		this.startActivityForResult(addIntent, CREATE_REPOSITORY);
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		switch (requestCode) {
		case INIT_DB:
			if (resultCode == RESULT_OK) {
				refreshList();
			} else {
				Toast.makeText(this, R.string.error_initializing_database, Toast.LENGTH_LONG).show();
			}
			break;
		case CREATE_REPOSITORY:
			if (resultCode == RESULT_OK) {
				logger.debug("Attempting to make: {}", data.getData());
				Intent makeIt = new Intent(Actions.ACTION_INIT_DB, data.getData());
				startActivityForResult(makeIt, INIT_DB);
			}
			break;
		case PICK_BRANCH:
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				launchListActivity(uri);
			}
			break;
		}
	}

	private void launchListActivity(Uri uri) {
		String action = Intent.ACTION_VIEW;
		try {
			PackageManager pm = getPackageManager();
			logger.debug("Starting native list for: {}", uri);
			Intent i = new Intent(action, EntityUriBuilder.toNative(uri));
			List<ResolveInfo> infos = pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
			logger.debug("Got infos: {}", infos.size());
			for (ResolveInfo info : infos) {
				logger.debug("Match: {}", info.activityInfo.applicationInfo.className);
			}
			startActivity(i);
		} catch (Exception e) {
			try {
				logger.debug("Starting non-native list for: {}", uri);
				Intent intent = new Intent(action, uri);
				intent.setClassName(this, AvroBaseList.class.getName());
				startActivity(intent);
			} catch (Exception e1) {
				logger.error("View activity not found.", e);
				Toast.makeText(this, R.string.error_activity_not_found, Toast.LENGTH_LONG).show();
			}
		}
	}

	private void refreshList() {
		List<Map<String, Object>> temp = getAllRepos();
		repos.clear();
		repos.addAll(temp);
		mAdapter.notifyDataSetChanged();
	}

}
