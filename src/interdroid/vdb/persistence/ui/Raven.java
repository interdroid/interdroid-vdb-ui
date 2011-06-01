package interdroid.vdb.persistence.ui;

import interdroid.vdb.Actions;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class Raven extends TabActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final TabHost tabHost = getTabHost();

	    tabHost.addTab(tabHost.newTabSpec("repo-tab")
	            .setIndicator("Repositories")
	            .setContent(new Intent(
	            		Actions.ACTION_MANAGE_REPOSITORIES, null)));

	    tabHost.addTab(tabHost.newTabSpec("peer-tab")
	            .setIndicator("Peers")
	            .setContent(new Intent(
	            		Actions.ACTION_MANAGE_PEERS, null)));
	}

}
