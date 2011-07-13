package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.Actions;
import interdroid.vdb.R;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.content.VdbProviderRegistry;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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

	protected void onListItemClick(ListView lv, View v, int position, long id) {

		final String repoName = (String) repos.get(position).get(VdbProviderRegistry.REPOSITORY_NAME);

		final CharSequence[] items = {getString(R.string.action_open_repo),
				getString(R.string.action_manage_repo), getString(R.string.action_delete_repo)};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.title_repository_action));
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				switch (item) {
				case 0:
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
				case 1:
					try {
						logger.debug("Launching manage for: {}", repoName);
						startActivity(new Intent(Actions.ACTION_MANAGE_REPOSITORY,
								EntityUriBuilder.repositoryUri(VdbMainContentProvider.AUTHORITY, repoName)));
					} catch (Exception e) {
						logger.error("Error managing repository", e);
						Toast.makeText(ManageRepositoriesActivity.this, getString(R.string.error_managing_repo), Toast.LENGTH_LONG).show();
					}
					break;
				case 2:
					// TODO: Add support for deleting repositories
					Toast.makeText(ManageRepositoriesActivity.this, "Not yet supported", Toast.LENGTH_LONG).show();
					break;
				}
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
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
			Intent addIntent = new Intent(Intent.ACTION_INSERT);
			addIntent.setType(
					"vnd.android.cursor.item/vnd.org.apache.avro.RecordDef");
			logger.debug("Adding Repository");
			this.startActivityForResult(addIntent, CREATE_REPOSITORY);
			return true;
		case MENU_ITEM_PREFS:
			Intent prefsIntent = new Intent(Intent.ACTION_EDIT);
			prefsIntent.setClass(this, VdbPreferences.class);
			startActivity(prefsIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		switch (requestCode) {
		case CREATE_REPOSITORY:
			if (resultCode == RESULT_OK) {
				refreshList();
			}
			break;
		case PICK_BRANCH:
			if (resultCode == RESULT_OK) {
				try {
					PackageManager pm = getPackageManager();
					logger.debug("Starting native view for: {}", data.getData());
					Intent i = new Intent(Intent.ACTION_VIEW, EntityUriBuilder.toNative(data.getData()));
					List<ResolveInfo> infos = pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
					logger.debug("Got infos: {}", infos.size());
					for (ResolveInfo info : infos) {
						logger.debug("Match: {}", info.activityInfo.applicationInfo.className);
					}
					startActivity(i);
				} catch (Exception e) {
					try {
						logger.debug("Starting non-native view for: {}", data.getData());
						startActivity(new Intent(Intent.ACTION_VIEW, data.getData()));
					} catch (Exception e1) {
						logger.error("View activity not found.", e);
						Toast.makeText(this, R.string.error_activity_not_found, Toast.LENGTH_LONG).show();
					}
				}
			}
			break;
		}
	}

	private void refreshList() {
		List<Map<String, Object>> temp = getAllRepos();
		repos.clear();
		repos.addAll(temp);
		mAdapter.notifyDataSetChanged();
	}

}
