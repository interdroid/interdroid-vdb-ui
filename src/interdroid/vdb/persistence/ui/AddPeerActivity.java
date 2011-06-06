package interdroid.vdb.persistence.ui;

import android.app.Activity;
import android.os.Bundle;

public class AddPeerActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		buildUI();
	}

	private void buildUI()
	{
		setTitle("Add a Peer");
	}

}
