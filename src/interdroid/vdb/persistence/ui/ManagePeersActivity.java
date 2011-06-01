package interdroid.vdb.persistence.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import interdroid.vdb.R;
import interdroid.vdb.persistence.content.PeerRegistry;
import interdroid.vdb.persistence.content.PeerRegistry.Peer;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;


public class ManagePeersActivity extends Activity {

	static final int REQUEST_ADD_PEER = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		buildUI();
	}

	private void buildUI()
	{
		setTitle("Peer Manager");
		setContentView(R.layout.peer_manager_dialog);

		ListView list = (ListView) findViewById(R.id.peer_list);
		list.setAdapter(new SimpleAdapter(this, getAllPeers(), R.layout.peer_item,
				new String[] {Peer.NAME, Peer.EMAIL, Peer.REPOSITORIES}, new int[] {R.id.peer_name, R.id.peer_email, R.id.peer_repos}));
	}

	public List<Map<String, Object>> getAllPeers() {
		Cursor c = null;
		ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		try {
			c = getContentResolver().query(PeerRegistry.URI, new String[]{Peer._ID, Peer.NAME, Peer.EMAIL, Peer.REPOSITORIES}, null, null, null);
			if (c != null) {
				int idIndex = c.getColumnIndex(Peer._ID);
				int nameIndex = c.getColumnIndex(Peer.NAME);
				int emailIndex = c.getColumnIndex(Peer.EMAIL);
				int reposIndex = c.getColumnIndex(Peer.REPOSITORIES);
				while (c.moveToNext()) {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					HashMap<String, Object> data = new HashMap();
					data.put(Peer._ID, c.getInt(idIndex));
					data.put(Peer.NAME, c.getString(nameIndex));
					data.put(Peer.EMAIL, c.getString(emailIndex));
					data.put(Peer.REPOSITORIES, c.getString(reposIndex));
					result.add(data);
				}
			}
		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (Exception e) {};
			}
		}

		return result;
	}

}
