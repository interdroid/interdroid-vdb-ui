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

import java.net.URISyntaxException;

import interdroid.vdb.Actions;
import interdroid.vdb.R;
import interdroid.vdb.persistence.api.RemoteInfo;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.content.PeerRegistry;
import interdroid.vdb.persistence.content.PeerRegistry.Peer;


import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.TabActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.Toast;

public class EditPeerActivity extends TabActivity {
	private static final Logger logger = LoggerFactory
			.getLogger(EditPeerActivity.class);

	static final int ACTIVITY_SET_PREFERENCES = 0x10101;

	public static class PeerInfo {
		public PeerInfo() { }

		public PeerInfo(String name, String email,
				String device) {
			this.name = name;
			this.email = email;
			this.device = device;
			this.state = Peer.STATE_ADDED;
		}

		public String name;
		public String email;
		public String device;
		public Uri uri;
		public int state;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		Uri uri = intent.getData();
		logger.debug("Got request to edit peer: {}", uri);

		setTitle(getText(R.string.title_manage_peer));

		// Make sure local prefs are set
		getLocalName(this);

		final TabHost tabHost = getTabHost();

		tabHost.addTab(tabHost.newTabSpec("details")
				.setIndicator(getString(R.string.label_peer_info))
				.setContent(new Intent(
						Actions.ACTION_MANAGE_PEER_INFO,
						uri)));

		tabHost.addTab(tabHost.newTabSpec("local")
				.setIndicator(getString(R.string.label_local_repositories))
				.setContent(new Intent(
						Actions.ACTION_MANAGE_LOCAL_SHARING,
						uri)));

		tabHost.addTab(tabHost.newTabSpec("remote")
				.setIndicator(getString(R.string.label_remote_repositories))
				.setContent(new Intent(
						Actions.ACTION_MANAGE_REMOTE_SHARING,
						uri)));

	}

	static String getLocalName(Activity context) {
		String localName = null;
		SharedPreferences prefs = context.getSharedPreferences(VdbPreferences.PREFERENCES_NAME, MODE_PRIVATE);
		if (prefs.contains(VdbPreferences.PREF_EMAIL) && prefs.contains(VdbPreferences.PREF_DEVICE)) {
			localName = VdbPreferences.makeLocalName(prefs.getString(VdbPreferences.PREF_DEVICE, null), prefs.getString(VdbPreferences.PREF_EMAIL, null));
		} else {
			Toast.makeText(context, R.string.prefs_not_set, Toast.LENGTH_LONG).show();
			context.startActivityForResult(new Intent(context, VdbPreferences.class), ACTIVITY_SET_PREFERENCES);
		}
		return localName;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Make sure the local name was set properly.
		getLocalName(this);
	}

	public static PeerInfo getPeerInfo(Activity context, Uri uri, Bundle extras) {
		PeerInfo peerInfo = new PeerInfo();

		if (uri == null) {
			peerInfo.email = extras.getString(VdbPreferences.PREF_EMAIL);
			peerInfo.name = extras.getString(VdbPreferences.PREF_NAME);
			peerInfo.device = extras.getString(VdbPreferences.PREF_DEVICE);

			if (peerInfo.name == null) {
				Toast.makeText(context, R.string.error_no_name, Toast.LENGTH_LONG).show();
				return null;
			}
			if (peerInfo.email == null) {
				Toast.makeText(context, R.string.error_no_email, Toast.LENGTH_LONG).show();
				return null;
			}
			// Does this peer already exist?
			Cursor c = context.getContentResolver().query(PeerRegistry.URI, null,
					Peer.EMAIL + "=? AND " + Peer.DEVICE + "=?", new String[] {peerInfo.email, peerInfo.device}, null);
			if (c != null && c.moveToFirst()) {
				logger.warn("Request to add existing peer.");
					peerInfo.uri = Uri.withAppendedPath(PeerRegistry.URI,
							String.valueOf(c.getInt(c.getColumnIndex(Peer._ID))));
					peerInfo.state = c.getInt(c.getColumnIndex(Peer.STATE));
			} else {
				logger.debug("Adding to peers: {} {}", peerInfo.name, peerInfo.email);
				logger.debug("Device: {}", peerInfo.device);
				ContentValues values = new ContentValues();
				values.put(Peer.NAME, peerInfo.name);
				values.put(Peer.EMAIL, peerInfo.email);
				values.put(Peer.DEVICE, peerInfo.device);
				values.put(Peer.STATE, Peer.STATE_NEW);
				peerInfo.uri = context.getContentResolver().insert(PeerRegistry.URI, values);
			}
			if (c != null) {
				c.close();
			}
		} else {
			logger.debug("Querying for peer.");
			peerInfo.uri = uri;
			Cursor c = context.getContentResolver().query(uri, null, null, null, null);
			if (c != null && c.getCount() == 1 && c.moveToFirst()) {
				peerInfo.name = c.getString(c.getColumnIndex(Peer.NAME));
				peerInfo.email = c.getString(c.getColumnIndex(Peer.EMAIL));
				peerInfo.device = c.getString(c.getColumnIndex(Peer.DEVICE));
				peerInfo.state = c.getInt(c.getColumnIndex(Peer.STATE));
				c.close();
			} else {
				Toast.makeText(context, R.string.error_managing_peer, Toast.LENGTH_LONG).show();
				if (c != null) {
					c.close();
				}
				return null;
			}
		}

		return peerInfo;
	}

	// TODO: This belongs somewhere else I think. But where?

	/**
	 * May launch SET_PREFERENCES preference activity for result. Callers must be able to handle this result.
	 *
	 * @param context The activity calling this method
	 * @param userName The users name
	 * @param userEmail The users email used as the address for a ss:/ uri
	 * @param device The name of the device
	 * @param localName Our name on the remote
	 * @param repo The repository to add to
	 * @return true If we successfully added.
	 * @throws URISyntaxException
	 */
	public static boolean addPeerToRepository(Activity context, PeerInfo peerInfo, VdbRepository repo)
			throws URISyntaxException {
		boolean result = false;
		String localName = EditPeerActivity.getLocalName(context);

		RemoteInfo info = new RemoteInfo();
		info.setType(RemoteInfo.RemoteType.HUB);
		String remoteName = VdbPreferences.makeLocalName(peerInfo.device, peerInfo.email);

		if (peerInfo.email == null || !Repository.isValidRefName("refs/remote/" + peerInfo.email)) {
			Toast.makeText(context, R.string.error_invalid_remote, Toast.LENGTH_LONG).show();
		} else {

			info.setDescription(peerInfo.name);
			info.setName(remoteName);
			info.setOurNameOnRemote(localName);
			URIish uri = new URIish().setScheme("ss").setHost(remoteName).setPath("/" + repo.getName());
			logger.debug("URI is: {}", uri);
			info.setRemoteUri(uri);
			try {
				repo.saveRemote(info);
				logger.info("Repo added.");
				result = true;
			} catch (Exception e) {
				logger.error("Error saving remote", e);
				Toast.makeText(context, R.string.error_toggling_peer, Toast.LENGTH_LONG).show();
			}
		}
		return result;
	}
}
