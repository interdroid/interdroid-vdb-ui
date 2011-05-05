package interdroid.vdb.persistence.ui;


import interdroid.vdb.Actions;
import interdroid.vdb.content.EntityUriBuilder;
//import interdroid.vdb.content.VdbMainContentProvider;
//import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.impl.MergeHelper;
import interdroid.vdb.persistence.impl.VdbRepositoryImpl;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class TestActivity extends Activity {
	private static final int REQUEST_PICK_VERSION = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        // Intent intent = new Intent(Intent.ACTION_PICK,
        //		Uri.parse("content://" + VdbMainContentProvider.AUTHORITY + "/notes"));
        // startActivityForResult(intent, REQUEST_PICK_VERSION);

		Intent intent = new Intent(Actions.ACTION_MANAGE_REPOSITORY,
				EntityUriBuilder.repositoryUri("google.notes"));
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
						VdbRepositoryRegistry.getInstance().getRepository("google.notes")
						.getBranch("temp"));
				//helper.diff2("google.notes", "google.notes");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			VdbRepositoryImpl reg = (VdbRepositoryImpl)
					VdbRepositoryRegistry.getInstance().getRepository("google.notes");

			Intent intent;

			intent = new Intent(Actions.ACTION_MANAGE_REPOSITORY,
					EntityUriBuilder.repositoryUri("google.notes"));
			startActivity(intent);

			if (false) {
				Uri notesUri = Uri.withAppendedPath(data.getData(), "google.notes");

				intent = new Intent(Intent.ACTION_PICK, notesUri);
				startActivity(intent);
			}
		}
	}
}
