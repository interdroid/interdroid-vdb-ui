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
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class ManageRepositoriesActivity extends ListActivity {
	private static final Logger logger = LoggerFactory
	.getLogger(ManageRepositoriesActivity.class);

	private List<Map<String, Object>> repos;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		buildUI();
	}

	private void buildUI() {
		setTitle(getString(R.string.title_manage_repositories));

		repos = getAllRepos();
		setListAdapter(new SimpleAdapter(this, repos, R.layout.repo_item,
				new String[] {VdbProviderRegistry.REPOSITORY_NAME}, new int[] {R.id.repoName}));
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
						startActivity(new Intent(Intent.ACTION_PICK,
								EntityUriBuilder.repositoryUri(VdbMainContentProvider.AUTHORITY, repoName)));
					} catch (Exception e) {
						logger.error("Error opening repository", e);
						Toast.makeText(ManageRepositoriesActivity.this, getString(R.string.error_opening_repo), Toast.LENGTH_LONG);
					}
					break;
				case 1:
					try {
						startActivity(new Intent(Actions.ACTION_MANAGE_REPOSITORY,
							EntityUriBuilder.repositoryUri(VdbMainContentProvider.AUTHORITY, repoName)));
					} catch (Exception e) {
						logger.error("Error managing repository", e);
						Toast.makeText(ManageRepositoriesActivity.this, getString(R.string.error_managing_repo), Toast.LENGTH_LONG);
					}
					break;
				case 2:
					// TODO: Add support for deleting repositories
					Toast.makeText(ManageRepositoriesActivity.this, "Not yet supported", Toast.LENGTH_LONG);
					break;
				}
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

}
