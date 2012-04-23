/*
 * Copyright (c) 2008-2012 Vrije Universiteit, The Netherlands All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the Vrije Universiteit nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package interdroid.vdb.persistence.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.util.DbUtil;
import interdroid.vdb.R;
import interdroid.vdb.persistence.content.PeerRegistry;
import interdroid.vdb.persistence.content.PeerRegistry.Peer;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

// TODO: Need to change view when using ss:// protocol.
public class ManagePeersActivity extends Activity implements OnItemClickListener {
	private static final Logger logger = LoggerFactory
			.getLogger(ManagePeersActivity.class);

	static final int REQUEST_ADD_PEER = 1;
	private List<Map<String, Object>> mPeers;

	private boolean mPickMode = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent data = getIntent();
		if (Intent.ACTION_PICK.equals(data.getAction())) {
			logger.debug("Running in pick mode!");
			mPickMode = true;
			// Assume we are canceled
			setResult(RESULT_CANCELED);
		}

		buildUI();
	}

	private void buildUI()
	{
		if (mPickMode) {
			setTitle(R.string.title_pick_peer);
		} else {
			setTitle(R.string.title_peer_manager);
		}

		setContentView(R.layout.peer_manager_dialog);

		ListView list = (ListView) findViewById(R.id.peer_list);
		mPeers = getAllPeers();
		list.setAdapter(new SimpleAdapter(this, mPeers, R.layout.peer_item,
				new String[] {Peer.NAME, Peer.EMAIL, Peer.DEVICE}, new int[] {R.id.peer_name, R.id.peer_email, R.id.peer_device}));
		list.setClickable(true);
		list.setOnItemClickListener(this);

		// Set the empty view.
		TextView empty = new TextView(this);
		empty.setText("Peers are added automatically as they are discovered.");
		empty.setGravity(Gravity.CENTER);
		list.setEmptyView(empty);
		((ViewGroup) list.getParent()).addView(empty);
	}

	public List<Map<String, Object>> getAllPeers() {
		Cursor c = null;
		ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		try {
			c = getContentResolver().query(PeerRegistry.URI, new String[]{Peer._ID, Peer.NAME, Peer.EMAIL, Peer.DEVICE}, Peer.STATE + "!=?", new String[] {String.valueOf(Peer.STATE_REJECTED)}, null);
			if (c != null) {
				int idIndex =  DbUtil.getFieldIndex(c, Peer._ID);
				int nameIndex =  DbUtil.getFieldIndex(c, Peer.NAME);
				int emailIndex =  DbUtil.getFieldIndex(c, Peer.EMAIL);
				int deviceIndex =  DbUtil.getFieldIndex(c, Peer.DEVICE);
				while (c.moveToNext()) {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					HashMap<String, Object> data = new HashMap();
					data.put(Peer._ID, c.getInt(idIndex));
					data.put(Peer.NAME, c.getString(nameIndex));
					data.put(Peer.EMAIL, c.getString(emailIndex));
					data.put(Peer.DEVICE, c.getString(deviceIndex));
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mPickMode) {
			logger.debug("Picked: {} {}", position, mPeers.get(position).get(Peer._ID));
			Intent result = new Intent();
			result.setData(Uri.withAppendedPath(PeerRegistry.URI, String.valueOf(mPeers.get(position).get(Peer._ID))));
			result.putExtra(Peer.NAME, (String)mPeers.get(position).get(Peer.NAME));
			result.putExtra(Peer.EMAIL, (String)mPeers.get(position).get(Peer.EMAIL));
			result.putExtra(Peer.DEVICE, (String)mPeers.get(position).get(Peer.DEVICE));
			setResult(RESULT_OK, result);
			finish();
		} else {
			logger.debug("Got request to manage: {} {}", position, mPeers.get(position).get(Peer._ID));
			startActivity(new Intent(Intent.ACTION_EDIT, Uri.withAppendedPath(PeerRegistry.URI,
					String.valueOf(mPeers.get(position).get(Peer._ID)))));
		}
	}

	public static final int MENU_ITEM_PREFS = Menu.FIRST;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ITEM_PREFS, 0, "Preferences")
		.setShortcut('2', 'p')
		.setIcon(android.R.drawable.ic_menu_preferences);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_PREFS:
			Intent prefsIntent = new Intent(Intent.ACTION_EDIT);
			prefsIntent.setClass(this, VdbPreferences.class);
			startActivity(prefsIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
