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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;
import interdroid.vdb.persistence.content.PeerRegistry.Peer;
import interdroid.vdb.persistence.ui.EditPeerActivity.PeerInfo;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;


public class EditPeerDetailsActivity extends Activity {
	private static final Logger logger = LoggerFactory
	.getLogger(EditPeerDetailsActivity.class);

	private EditText mEmail;
	private EditText mName;
	private EditText mDevice;

	private PeerInfo mPeerInfo;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		Uri uri = intent.getData();
		logger.debug("Got request to edit peer: {}", uri);

		mPeerInfo = EditPeerActivity.getPeerInfo(this, uri, intent.getExtras());

		buildUI();
	}

	public void onResume() {
		super.onResume();
		EditPeerActivity.getLocalName(this);
	}

	protected void onPause() {
		super.onPause();

		// Save name back to peer.
		ContentValues values = new ContentValues();
		values.put(Peer.NAME, mName.getText().toString());
		values.put(Peer.DEVICE, mDevice.getText().toString());
		getContentResolver().update(mPeerInfo.uri, values, null, null);
	}

	private void buildUI()
	{
		setContentView(R.layout.edit_peer);

		mDevice = (EditText)findViewById(R.id.add_peer_device);
		mDevice.setText(mPeerInfo.device);
		mDevice.setEnabled(false);
		mDevice.setFocusable(false);

		mEmail = (EditText)findViewById(R.id.add_peer_email);
		mEmail.setText(mPeerInfo.email);
		mEmail.setEnabled(false);
		mEmail.setFocusable(false);

		mName = (EditText)findViewById(R.id.add_peer_name);
		mName.setText(mPeerInfo.name);
		mName.setEnabled(true);
		mName.setFocusable(true);
	}
}
