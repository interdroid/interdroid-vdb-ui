package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.smartsockets.util.MalformedAddressException;
import ibis.smartsockets.virtual.InitializationException;
import interdroid.util.ToastOnUI;
import interdroid.vdb.R;
import interdroid.vdb.transport.SmartSocketsTransport;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class EditRemoteSharedRepositoriesActivity extends BaseEditRepositoryActivity implements OnItemClickListener {
	private static final Logger logger = LoggerFactory
			.getLogger(EditRemoteSharedRepositoriesActivity.class);

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

	}

	@Override
	protected void refreshList() {
		String localName = EditPeerActivity.getLocalName(this);
		mRepos = new ArrayList<Map<String, Object>>();
		try {
			mRepos = SmartSocketsTransport.getRepositories(localName, mPeerName);
		} catch (MalformedAddressException e) {
			logger.error("Inavlid peer name.", e);
			ToastOnUI.show(this, R.string.error_invalid_uri, Toast.LENGTH_LONG);
		} catch (IOException e) {
			logger.error("IOError communicating with peer", e);
			ToastOnUI.show(this, R.string.error_contacting_peer, Toast.LENGTH_LONG);
		} catch (InitializationException e) {
			logger.error("Initializing communication error", e);
			ToastOnUI.show(this, R.string.error_contacting_peer, Toast.LENGTH_LONG);
		}

	}
}
