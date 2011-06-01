package interdroid.vdb.persistence.ui;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.smartsockets.naming.NameResolver;
import interdroid.vdb.R;
import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.content.PeerRegistry;
import interdroid.vdb.persistence.content.PeerRegistry.Peer;
import interdroid.vdb.transport.SmartSocketsDaemon;
import interdroid.vdb.transport.SmartSocketsDaemonClient;
import interdroid.vdb.transport.VdbRepositoryResolver;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class GitService extends Service {
	private static final Logger logger = LoggerFactory.getLogger(GitService.class);

	// The default sync interval.
	private static final int DEFAULT_INTERVAL = 60;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = R.string.git_service_started;

	// The notification manager
	private NotificationManager mNM;

	// The smartsockets daemon which is listening
	private SmartSocketsDaemon mSmartSocketsDaemon;

	//	private Daemon mDaemon;
	// The synchronizer service which is runing sync periodically
	private GitSynchronizer mSynchronizer;

	// Listeners for turning on and off the daemons
	private BroadcastReceiver mBackgroundDataChangedListener;
	private BroadcastReceiver mConnectivityActionListener;

	// Flag to indicate if we are running.
	private boolean mRunning;

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
		// Grab the notifications manager we will use to interact with the user
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		// Register handlers so we start/stop/restart stuff at the right times
		mBackgroundDataChangedListener = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				if (mRunning && !connectivityManager.getBackgroundDataSetting()) {
					logger.info("Shutdown due to change in background data setting.");
					shutdown();
				} else if (!mRunning && connectivityManager.getBackgroundDataSetting()) {
					logger.info("Startup due to change in background data setting.");
					startup();
				}
			}

		};
		mConnectivityActionListener = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// Did we loose connectivity?
				if (intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY) && intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true)) {
					logger.info("Shutdown due to loss of connectivity");
					shutdown();
				} else {
					logger.info("Restart due to change in network.");
					restart();
				}
			}
		};
		getApplicationContext().registerReceiver(mBackgroundDataChangedListener, new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
		getApplicationContext().registerReceiver(mConnectivityActionListener, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// Only startup if we are allowed to.
		SharedPreferences prefs = getSharedPreferences(VdbPreferences.PREFERENCES_NAME, MODE_PRIVATE);
		if (connectivityManager.getBackgroundDataSetting() && prefs.getBoolean(VdbPreferences.PREF_SHARING_ENABLED, true)) {
			// Make sure name and email preferences are set
			if (!(prefs.contains(VdbPreferences.PREF_EMAIL) && prefs.contains(VdbPreferences.PREF_NAME)) ) {
				showPrefsNotification();
			} else {
				startup();
			}
		}
	}

	private class GitSynchronizer {
		private ScheduledExecutorService mExecutorService;
		@SuppressWarnings("rawtypes")
		private ScheduledFuture mScannerTask;

		// TODO: Much more smarts here. We should be watching for commits on local repos and only triggering
		// syncs for available partners for which we share a repo.

		public GitSynchronizer(int interval) {
			mRunning = true;
			mExecutorService = Executors.newScheduledThreadPool(1);

			final Runnable scanPeers = new Runnable() {
				@Override
				public void run() {
					try {
						Set<String> names = NameResolver.getDefaultResolver().getAvailableServices();
						for (final String name : names) {
							if (!name.equals(getSocketName())) {
								Runnable sync = new Runnable() {
									public void run() {
										synchronizeWith(name);
									}
								};
								mExecutorService.schedule(sync, 0, TimeUnit.SECONDS);
							}
						}
					} catch (IOException e) {
						logger.error("NameResolver error durring synchronization", e);
					}
				}
			};

			mScannerTask = mExecutorService.scheduleAtFixedRate(scanPeers, interval, interval, TimeUnit.SECONDS);
		}

		protected void synchronizeWith(String serviceName) {
			VdbRepositoryRegistry repos = VdbRepositoryRegistry.getInstance();
			VdbProviderRegistry registry = null;
			try {
				registry = new VdbProviderRegistry(GitService.this);
			} catch (IOException e1) {
				logger.error("Unable to get provider registry", e1);
			}
			if (registry != null) {
				if (hasPeer(serviceName)) {
					for (String name : registry.getAllRepositoryNames()) {
						VdbRepository repo;
						try {
							repo = repos.getRepository(GitService.this, name);
							try {
								Set<String> remotes = repo.listRemotes();
								if (remotes.contains(serviceName)) {
									logger.info("Synching with: {}", serviceName);
									// TODO: Hook progress monitor to notification bar
									repo.pullFromRemote(serviceName, null);
									repo.pushToRemote(serviceName, null);
									logger.info("Finished sync with: {}", serviceName);
								}
							} catch (IOException e) {
								logger.error("Unable to list remotes for repo: " + name, e);
							}
						} catch (IOException e1) {
							logger.error("Unable to get repository." , e1);
						}
					}
				} else {
					logger.info("Found possible new remote: {}", serviceName);
					showNewRemoteNotification(serviceName);
				}
			}
		}

		private boolean hasPeer(String serviceName) {
			Cursor c = null;
			try {
				c = getContentResolver().query(PeerRegistry.URI, null, Peer.EMAIL+"=?", new String[] {serviceName}, null);
				if (c != null && c.moveToFirst()) {
					return true;
				}
			} finally {
				if (c != null) {
					c.close();
				}
			}
			return false;
		}

		public void stop() {
			mScannerTask.cancel(true);
			mExecutorService.shutdown();
		}
	}

	private void startSmartsocketsDaemon() {
		try {
			logger.debug("Initializing smart sockets git daemon");
			mSmartSocketsDaemon = new SmartSocketsDaemon();
			mSmartSocketsDaemon.setRepositoryResolver(new VdbRepositoryResolver<SmartSocketsDaemonClient>(this));
			mSmartSocketsDaemon.start();
			logger.debug("SS Daemon Listening on: {}", mSmartSocketsDaemon.getAddress());
			logger.debug("SS Daemon running: {}", mSmartSocketsDaemon.isRunning());
			NameResolver.getDefaultResolver().register(getSocketName(), mSmartSocketsDaemon.getAddress());
		} catch (Exception e) {
			logger.error("Unable to initialize smart sockets daemon.", e);
		}
	}


	private void stopSmartsocketsDaemon() {
		mSmartSocketsDaemon.stop();
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
		// Unregister our recievers
		getApplicationContext().unregisterReceiver(this.mBackgroundDataChangedListener);
		getApplicationContext().unregisterReceiver(this.mConnectivityActionListener);

		shutdown();
	}


	private void restart() {
		// TODO This should have it's own notification
		shutdown();
		startup();
	}

	private void startup() {
		try {
			mSynchronizer = new GitSynchronizer(DEFAULT_INTERVAL);
			startSmartsocketsDaemon();
			//			startGitDaemon();
		} catch (Exception e) {
			logger.error("Error starting daemon.", e);
		}

		// Display a notification about us starting.  We put an icon in the status bar.
		showNotification();

		mRunning = true;
	}

	private void shutdown() {
		mRunning = false;

		// Stop the service.
		mSynchronizer.stop();

		stopSmartsocketsDaemon();

		// Unregister with the resolver
		String socketName = getSocketName();
		try {
			NameResolver.getDefaultResolver().unregister(socketName);
		} catch (IOException e) {
			logger.error("Unable to unregister service.", e);
		}

		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.git_service_stopped, Toast.LENGTH_SHORT).show();
	}

	private String getSocketName() {
		SharedPreferences prefs = getSharedPreferences(VdbPreferences.PREFERENCES_NAME, MODE_PRIVATE);
		return prefs.getString(VdbPreferences.PREF_EMAIL, "anonymous@localhost");
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
		CharSequence text = getText(R.string.git_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.git, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, VdbPreferences.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.git_service_label),
				text, contentIntent);

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	}

	private void showNewRemoteNotification(String name) {

		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = getText(R.string.git_service_found_new);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.git, text + name,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		Intent intent = new Intent(this, AddPeerActivity.class);
		intent.putExtra(VdbPreferences.PREF_EMAIL, name);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.git_service_label),
				getText(R.string.git_service_click_to_browse), contentIntent);

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	}

	private void showPrefsNotification() {
		CharSequence text = getText(R.string.prefs_not_set);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.git, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, VdbPreferences.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.git_service_label),
				getText(R.string.git_service_set_prefs), contentIntent);

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	}
}
