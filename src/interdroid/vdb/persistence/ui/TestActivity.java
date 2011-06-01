package interdroid.vdb.persistence.ui;


import interdroid.vdb.Actions;
import interdroid.vdb.R;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbMainContentProvider;
//import interdroid.vdb.content.VdbMainContentProvider;
//import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.impl.MergeHelper;
import interdroid.vdb.persistence.impl.VdbRepositoryImpl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class TestActivity extends Activity {
	private static final Logger logger = LoggerFactory
			.getLogger(TestActivity.class);

	private static final int REQUEST_PICK_VERSION = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        // Intent intent = new Intent(Intent.ACTION_PICK,
        //		Uri.parse("content://" + VdbMainContentProvider.AUTHORITY + "/notes"));
        // startActivityForResult(intent, REQUEST_PICK_VERSION);

		Intent intent = new Intent(Actions.ACTION_MANAGE_REPOSITORY,
				EntityUriBuilder.repositoryUri(VdbMainContentProvider.AUTHORITY, "google.notes"));
		startActivity(intent);
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK && requestCode == REQUEST_PICK_VERSION) {
			TextView v = new TextView(this);
			v.setText(data.getDataString());
			setContentView(v);

			try {
				MergeHelper helper = new MergeHelper(
						VdbRepositoryRegistry.getInstance().getRepository(this, "google.notes")
						.getBranch("temp"));
				//helper.diff2("google.notes", "google.notes");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				VdbRepositoryImpl reg = (VdbRepositoryImpl)
						VdbRepositoryRegistry.getInstance().getRepository(this, "google.notes");
			} catch (IOException e) {
				logger.error("Error getting repository", e);
				Toast.makeText(this, R.string.error_opening_repo, Toast.LENGTH_LONG);
			}

			Intent intent;

			intent = new Intent(Actions.ACTION_MANAGE_REPOSITORY,
					EntityUriBuilder.repositoryUri(VdbMainContentProvider.AUTHORITY, "google.notes"));
			startActivity(intent);

			if (false) {
				Uri notesUri = Uri.withAppendedPath(data.getData(), "google.notes");

				intent = new Intent(Intent.ACTION_PICK, notesUri);
				startActivity(intent);
			}
		}
	}
}
