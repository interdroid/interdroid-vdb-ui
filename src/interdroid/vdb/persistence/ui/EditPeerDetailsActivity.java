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
