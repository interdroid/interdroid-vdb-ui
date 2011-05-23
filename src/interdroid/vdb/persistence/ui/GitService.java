package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Random;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jgit.transport.Daemon;
import org.eclipse.jgit.transport.DaemonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;
import interdroid.vdb.transport.SmartSocketsDaemon;
import interdroid.vdb.transport.SmartSocketsDaemonClient;
import interdroid.vdb.transport.VdbRepositoryResolver;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class GitService extends Service {
	private static final Logger logger = LoggerFactory.getLogger(GitService.class);
	private NotificationManager mNM;

    public final static String SMARTSOCKETS_TYPE = "_smartsockets_git._tcp.local.";
    public final static String STANDARD_TYPE = "_git._tcp.local.";

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = R.string.git_service_started;
	private SmartSocketsDaemon mSmartSocketsDaemon;
	private Daemon mDaemon;
	private JmDNS jmdns;

	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class GitBinder extends Binder {
		GitService getService() {
			return GitService.this;
		}
	}

	@Override
	public void onCreate() {
		try {
			initJmDNS();
			startSmartsocketsDaemon();
//			startGitDaemon();
		} catch (Exception e) {
			logger.error("Error starting daemon.", e);
		}

		// Display a notification about us starting.  We put an icon in the status bar.
		showNotification();
	}

    private static class GitListener implements ServiceListener {
    	private String mLocalName;
    	private String mType;

    	private static HashMap<String, ServiceInfo> mServices = new HashMap<String, ServiceInfo>();

        public GitListener(String serviceType, String serviceName) {
        	if (serviceName == null) {
        		throw new IllegalArgumentException("Service Name is null");
        	}
        	if (serviceType == null) {
        		throw new IllegalArgumentException("Service Type is null");
        	}
        	logger.debug("Listening for service: {} of type: {}", serviceName, serviceType);
			mLocalName = serviceName;
			mType = serviceType;
		}

		@Override
        public void serviceAdded(ServiceEvent event) {
            logger.info("Service added: " + mLocalName + ": "+ event.getName() + "." + event.getType());
            // If this isn't our registration being seen
            if (isNonLocalService(event)) {
            	logger.info("Non local service.");
            }
        }

		private boolean isNonLocalService(ServiceEvent event) {
			return !mType.equals(event.getType()) && !mLocalName.equals(event.getName());
		}

        @Override
        public void serviceRemoved(ServiceEvent event) {
            logger.info("Service removed: " + event.getName() + "." + event.getType());
            if (isNonLocalService(event)) {
            	logger.info("Non local service.");
            	mServices.remove(event.getName());
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            logger.info("Service resolved: " + event.getInfo());
            if (isNonLocalService(event)) {
            	logger.info("Non local service.");
            	mServices.put(event.getName(), event.getInfo());
            }
        }
    }

	private void initJmDNS() {
		logger.debug("Opening JmDNS...");
		try {
			jmdns = JmDNS.create();
		} catch (IOException e) {
			logger.error("Unable to init jmDns", e);
		}
		logger.debug("Opened JmDNS!");
	}

	private String registerService(String type, String address, int port) {
		String serviceName = null;
		if (jmdns != null) {
			try {
				Random random = new Random();
				int id = random.nextInt(100000);

				final HashMap<String, String> values = new HashMap<String, String>();
				values.put("DvNm", "Android-" + id);
				values.put("Adrs", address);
				values.put("txtvers", "1");
				byte[] pair = new byte[8];
				random.nextBytes(pair);
				values.put("Pair", toHex(pair));

				byte[] name = new byte[20];
				random.nextBytes(name);
				serviceName = toHex(name);
				logger.info("Requesting pairing for " + serviceName);
				ServiceInfo pairservice = ServiceInfo.create(type, toHex(name), port, 0, 0, values);
				jmdns.registerService(pairservice);
			} catch (IOException e) {
				throw new RuntimeException("Unable to register service.", e);
			}
		}
		return serviceName;
	}

	private static final char[] _nibbleToHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static String toHex(byte[] code) {
		StringBuilder result = new StringBuilder(2 * code.length);

		for (int i = 0; i < code.length; i++) {
			int b = code[i] & 0xFF;
			result.append(_nibbleToHex[b / 16]);
			result.append(_nibbleToHex[b % 16]);
		}

		return result.toString();
	}

	private void startGitDaemon() {
		try {
			logger.debug("Initializing normal git daemon");
			mDaemon = new Daemon(new InetSocketAddress(Daemon.DEFAULT_PORT));
			mDaemon.setRepositoryResolver(new VdbRepositoryResolver<DaemonClient>(this));
			mDaemon.start();
			logger.debug("Daemon Listening on: {}", mDaemon.getAddress());
			logger.debug("Daemon running: {}", mDaemon.isRunning());
			registerListener(STANDARD_TYPE, registerService(STANDARD_TYPE, mDaemon.getAddress().toString(), mDaemon.getAddress().getPort()));
		} catch (IOException e) {
			logger.error("Error initializing standard daemon", e);
		}
	}

	private void startSmartsocketsDaemon() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		try {
			logger.debug("Initializing smart sockets git daemon");
			mSmartSocketsDaemon = new SmartSocketsDaemon();
			mSmartSocketsDaemon.setRepositoryResolver(new VdbRepositoryResolver<SmartSocketsDaemonClient>(this));
			mSmartSocketsDaemon.start();
			logger.debug("SS Daemon Listening on: {}", mSmartSocketsDaemon.getAddress());
			logger.debug("SS Daemon running: {}", mSmartSocketsDaemon.isRunning());
			registerListener(SMARTSOCKETS_TYPE, registerService(SMARTSOCKETS_TYPE, mSmartSocketsDaemon.getAddress().toString(), mSmartSocketsDaemon.getAddress().port()));
		} catch (Exception e) {
			logger.error("Unable to initialize smart sockets daemon.", e);
		}
	}

	private void registerListener(String type,
			String serviceName) {
        jmdns.addServiceListener(type, new GitListener(type, serviceName));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.debug("Received start id {} : {}", + startId, intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (jmdns != null) {
			jmdns.unregisterAllServices();
			try {
				jmdns.close();
			} catch (IOException e) {
				logger.warn("Error closing jmdns.", e);
			}
		}

		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.git_service_stopped, Toast.LENGTH_SHORT).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new GitBinder();

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = getText(R.string.git_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.git, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, EditRemoteActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.git_service_label),
				text, contentIntent);

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	}
}
