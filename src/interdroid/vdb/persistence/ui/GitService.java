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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.smartsockets.naming.NameResolver;
import ibis.smartsockets.naming.NameResolver.LocalNamingListener;
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
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class GitService extends Service {
	private static final Logger logger = LoggerFactory.getLogger(GitService.class);

	// The default sync interval.
	private static final int DEFAULT_INTERVAL = 60;
	private static final int DEFAULT_TIMEOUT = 15000;

	private static final String DEFAULT_HUB_ADDRESS = "130.37.29.189-17878~nick";

	private static final String ANON_DEVICE = "unknown";
	private static final String ANON_EMAIL = "anonymous@localhost";
	private static final String ANON_NAME = "anonymous";
	private static final String ANON_SOCKET_NAME = VdbPreferences.makeLocalName(ANON_DEVICE, ANON_EMAIL);

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION_RUNNING = R.string.git_service_started;
	private int NOTIFICATION_PREFS = R.string.git_service_set_prefs;

	// The notification manager
	private NotificationManager mNotificationManager;

	// The smartsockets daemon which is listening
	private SmartSocketsDaemon mSmartSocketsDaemon;

	//	private Daemon mDaemon;
	// The synchronizer service which is runing sync periodically
	private GitSynchronizer mSynchronizer;

	// Flag to indicate if we are running.
	private boolean mRunning;
	// Show a notification when we set this false.
	private boolean mRunnable = true;

	private SharedPreferences mPrefs;
	private SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefsChangeListener;

	// TODO: What to do on 1.5
//	private MulticastLock mLock;

	// Register handlers so we start/stop/restart stuff at the right times
	private BroadcastReceiver mBackgroundDataChangedListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			logger.debug("Got background data changed message.");
			if (mRunning && !connectivityManager.getBackgroundDataSetting()) {
				logger.info("Shutdown due to change in background data setting.");
				shutdown(false);
			} else if (!mRunning && connectivityManager.getBackgroundDataSetting()) {
				logger.info("Startup due to change in background data setting.");
				startup(false);
			}
		}

	};

	private BroadcastReceiver mConnectivityActionListener = new BroadcastReceiver() {
		NetworkInfo mLastNetwork = null;
		@Override
		public void onReceive(Context context, Intent intent) {
			// Did we loose connectivity?
			logger.debug("Got connectivity intent: {} ", intent);
			if (intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY) && intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true)) {
				logger.info("Shutdown due to loss of connectivity");
				shutdown(false);
			} else {
				NetworkInfo newNetwork = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
				logger.debug("New state is: {} ", newNetwork);
				logger.debug("Old state is: {} ", mLastNetwork);
				if (!(newNetwork.getState().equals(NetworkInfo.State.CONNECTED) || newNetwork.getState().equals(NetworkInfo.State.CONNECTING))) {
					logger.debug("Not connected. Shutdown.");
					shutdown(false);
				} else {
					logger.debug("Connected. Starting.");
					startup(false);
				}
				mLastNetwork = newNetwork;
			}
		}
	};

	private LocalNamingListener mLocalDiscoveryListener = new LocalNamingListener() {

		@Override
		public void onLocalNameDiscovered(String serviceName) {
			// Ignore anonymous socket just in case
			if (!ANON_SOCKET_NAME.equals(serviceName)) {
				if (!hasPeer(serviceName)) {
					logger.info("Found possible new remote: {}", serviceName);
					Map<String, String> info;
					String name = null;
					String email = null;
					String device = null;
					logger.debug("Resolving: {}", serviceName);
					info = mSmartSocketsDaemon.getResolver().resolveInfo(serviceName);
					name = info.get(VdbPreferences.PREF_NAME);
					email = info.get(VdbPreferences.PREF_EMAIL);
					device = info.get(VdbPreferences.PREF_DEVICE);
					logger.debug("Resolved to: {}", name);
					logger.info("Showing new remote notification for: {} {}", name, serviceName);
					showNewRemoteNotification(name, email, device);
				}
			}
		}

		/**
		 * Returns true if we have this peer in our list of peers
		 * @param serviceName The name of the peer's service
		 * @return true if the peer is in our list of peers.
		 */
		private boolean hasPeer(String serviceName) {
			Cursor c = null;
			try {
				c = getContentResolver().query(PeerRegistry.URI, null,
						VdbPreferences.makeLocalName(Peer.DEVICE+"||'", "'||" +Peer.EMAIL+"=?"),
						new String[] {serviceName}, null);
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

		@Override
		public void onLocalNameRemoved(String name) {
			// Nothing to do here.
		}
	};


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
		mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mPrefs = getSharedPreferences(VdbPreferences.PREFERENCES_NAME, MODE_PRIVATE);

		mSharedPrefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				mPrefs = sharedPreferences;
				logger.debug("Shared Preference Changed: {}", key);
				// Do we need to turn off the prefs notfication
				if (hasPref(VdbPreferences.PREF_DEVICE)) {
					mNotificationManager.cancel(NOTIFICATION_PREFS);
				}
				if (mPrefs.getBoolean(VdbPreferences.PREF_SHARING_ENABLED, true)) {
					logger.debug("Sharing enabled. Starting.");
					startup(false);
				} else {
					logger.debug("Sharing disabled. Stopping.");
					shutdown(false);
				}
			}

		};
		mPrefs.registerOnSharedPreferenceChangeListener(mSharedPrefsChangeListener);

		if (!isConfigured()) {
			showPrefsNotification();
		} else {
			startup(true);
		}
	}

	private void registerListeners() {
		getApplicationContext().registerReceiver(mBackgroundDataChangedListener,
				new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
		getApplicationContext().registerReceiver(mConnectivityActionListener,
				new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	private synchronized void getMulticastLock() {
//		logger.info("Turning on multicast packet processing.");
//		WifiManager wifi =
//				(WifiManager)getSystemService(android.content.Context.WIFI_SERVICE);
//		mLock = wifi.createMulticastLock("GitServiceLock");
//		mLock.setReferenceCounted(false);
//		mLock.acquire();
//		logger.debug("Got multicast lock.");
	}

	private synchronized void releaseMulticastLock() {
//		if (mLock != null) {
//			logger.info("Releasing multicast lock.");
//			mLock.release();
//		}
	}

	private boolean isConfigured() {
		return hasPref(VdbPreferences.PREF_EMAIL) && hasPref(VdbPreferences.PREF_NAME) && hasPref(VdbPreferences.PREF_DEVICE);
	}

	private boolean hasPref(String prefName) {
		return mPrefs.contains(prefName) && mPrefs.getString(prefName, null) != null && !mPrefs.getString(prefName, "").equals("");
	}

	private class GitSynchronizer {
		private ScheduledExecutorService mExecutorService;
		@SuppressWarnings("rawtypes")
		private ScheduledFuture mScannerTask;

		public GitSynchronizer(int interval) {
			mRunning = true;
			mExecutorService = Executors.newScheduledThreadPool(1);

			final Runnable scanPeers = new Runnable() {
				@Override
				public void run() {
					logger.debug("Scanning repos running...");
					try {
						final VdbProviderRegistry providers = new VdbProviderRegistry(GitService.this);
						for (final String repoName : providers.getAllRepositoryNames()) {
							logger.debug("Checking repository: {}", repoName);
							try {
								final VdbRepositoryRegistry repos = VdbRepositoryRegistry.getInstance();
								final VdbRepository repo = repos.getRepository(GitService.this, repoName);
								// Get the list of all remotes for this repository
								Set<String> remotes = repo.listRemotes();
								for (final String remote : remotes) {
									logger.debug("Checking for peer: {}", remote);
									if (mSmartSocketsDaemon.getResolver().resolve(remote, DEFAULT_TIMEOUT) != null) {
										Runnable sync = new Runnable() {
											public void run() {
												logger.debug("Synchronizing {} with: {}", repoName, remote);
												synchronizeWith(repo, remote);
											}
										};
										logger.debug("Scheduling sync with: {}", remote);
										mExecutorService.schedule(sync, 0, TimeUnit.SECONDS);
									}
								}
							} catch (IOException e) {
								logger.debug("Exception while processing repo", e);
							}
						}
					} catch (IOException e) {
						logger.error("Exception while getting provider registry.", e);
					}
				}
			};
			logger.debug("Scheduling synchronizer service: {}", interval);
			mScannerTask = mExecutorService.scheduleAtFixedRate(scanPeers, interval, interval, TimeUnit.SECONDS);
		}

		protected void synchronizeWith(VdbRepository repo, String serviceName) {
			try {
				repo.pullFromRemote(serviceName, null);
			} catch (IOException e) {
				logger.error("Error pulling from remote", e);
			}
			try {
				repo.pushToRemote(serviceName, null);
			} catch (IOException e) {
				logger.error("Error pushing to remote", e);
			}
		}

		public void stop() {
			mScannerTask.cancel(true);
			mExecutorService.shutdown();
		}
	}

	private synchronized boolean startSmartsocketsDaemon() {
		try {
			logger.debug("Initializing smart sockets git daemon");
			getMulticastLock();

			mSmartSocketsDaemon = new SmartSocketsDaemon();
			mSmartSocketsDaemon.setRepositoryResolver(new VdbRepositoryResolver<SmartSocketsDaemonClient>(this));
			mSmartSocketsDaemon.start();

			NameResolver resolver = mSmartSocketsDaemon.getResolver();
			// Add our hub to the resolvers.
			resolver.addHub(getHub());
			resolver.registerLocalDiscoveryListener(mLocalDiscoveryListener);

			logger.debug("SS Daemon Listening on: {}", mSmartSocketsDaemon.getAddress());
			logger.debug("SS Daemon running: {}", mSmartSocketsDaemon.isRunning());
			Map<String, String> serviceInfo = new HashMap<String, String>();
			serviceInfo.put(VdbPreferences.PREF_NAME, getUserName());
			serviceInfo.put(VdbPreferences.PREF_EMAIL, getEmail());
			serviceInfo.put(VdbPreferences.PREF_DEVICE, getDevice());
			mSmartSocketsDaemon.getResolver().register(getSocketName(), mSmartSocketsDaemon.getAddress(), serviceInfo);
		} catch (Exception e) {
			mRunnable = false;
			stopSmartsocketsDaemon();
			logger.error("Unable to initialize smart sockets daemon.", e);
		}
		return mRunnable;
	}

	private void stopSmartsocketsDaemon() {
		if (mSmartSocketsDaemon != null) {
			mSmartSocketsDaemon.stop();
		}
	}

//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		logger.debug("Received start id {} : {}", + startId, intent);
//		// We want this service to continue running until it is explicitly
//		// stopped, so return sticky.
//		return START_STICKY;
//	}

	@Override
	public void onDestroy() {
		mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPrefsChangeListener);
		shutdown(true);
	}

	private boolean isStartable() {
		logger.debug("isStartable: {} {}", mRunnable, isConfigured());
		logger.debug("background data: {}", ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getBackgroundDataSetting());
		logger.debug("Sharing enabled: {}", mPrefs.getBoolean(VdbPreferences.PREF_SHARING_ENABLED, false));
		return mRunnable && isConfigured() && ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getBackgroundDataSetting() && mPrefs.getBoolean(VdbPreferences.PREF_SHARING_ENABLED, true);
	}

	private synchronized void startup(boolean registerListeners) {
		logger.debug("Starting: {} {}", mRunning, isStartable());
		if (!mRunning && isStartable()) {
			if (registerListeners) registerListeners();
			startDaemons();
		}
	}

	private synchronized void startDaemons() {
		try {
			mSynchronizer = new GitSynchronizer(DEFAULT_INTERVAL);
			startSmartsocketsDaemon();

			// Display a notification about us starting.  We put an icon in the status bar.
			showRunningNotification();

			mRunning = true;
			//			startGitDaemon();
		} catch (Exception e) {
			stopSmartsocketsDaemon();
			logger.error("Error starting daemon.", e);
			mRunnable = false;
		}

	}

	private synchronized void shutdown(boolean unregisterListeners) {
		if (mRunning) {
			logger.debug("Handling shutdown.");
			mRunning = false;

			if (unregisterListeners) unregisterListeners();

			// Stop the synchronizer service.
			mSynchronizer.stop();

			// Unregister with the resolver
			String socketName = getSocketName();
			NameResolver resolver = mSmartSocketsDaemon.getResolver();
			if (resolver != null) {
				resolver.unregister(socketName);
			}

			// Stop the daemon
			stopSmartsocketsDaemon();

			// Cancel the persistent notification.
			mNotificationManager.cancel(NOTIFICATION_RUNNING);

			// Tell the user we stopped.
			Toast.makeText(this, R.string.git_service_stopped, Toast.LENGTH_SHORT).show();
			logger.debug("Shutdown complete.");
		}
		if (unregisterListeners) {
			unregisterListeners();
		}
	}

	private void unregisterListeners() {
		// Unregister our recievers
		getApplicationContext().unregisterReceiver(this.mBackgroundDataChangedListener);
		getApplicationContext().unregisterReceiver(this.mConnectivityActionListener);

		NameResolver resolver = mSmartSocketsDaemon.getResolver();
		resolver.unRegisterLocalDiscoveryListener(mLocalDiscoveryListener);

		releaseMulticastLock();
	}

	private String getSocketName() {
		return VdbPreferences.makeLocalName(getDevice(), getEmail());
	}

	private String getUserName() {
		return mPrefs.getString(VdbPreferences.PREF_NAME, ANON_NAME);
	}

	private String getDevice() {
		return mPrefs.getString(VdbPreferences.PREF_DEVICE, ANON_NAME);
	}

	private String getEmail() {
		return mPrefs.getString(VdbPreferences.PREF_EMAIL, ANON_EMAIL);
	}

	private String getHub() {
		return mPrefs.getString(VdbPreferences.PREF_HUBS, DEFAULT_HUB_ADDRESS);
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
	private void showRunningNotification() {
		CharSequence text = getText(R.string.git_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.raven_logo, text,
				System.currentTimeMillis());
//		notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, VdbPreferences.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.git_service_label),
				text, contentIntent);

		// Send the notification.
		mNotificationManager.notify(NOTIFICATION_RUNNING, notification);
	}

	private void showNewRemoteNotification(String userName, String email, String device) {

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.raven_logo, userName,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		Intent intent = new Intent(this, EditPeerDetailsActivity.class);
		intent.putExtra(VdbPreferences.PREF_EMAIL, email);
		intent.putExtra(VdbPreferences.PREF_DEVICE, device);
		intent.putExtra(VdbPreferences.PREF_NAME, userName);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.git_service_found_new),
				getText(R.string.git_service_click_to_browse) + userName, contentIntent);

		// Send the notification.
		logger.debug("New peer notification: {} {}", device, email);
		logger.debug("Showing new peer notification: {}", userName.hashCode() + 11 * device.hashCode());
//		mNotificationManager.notify("new_peer", userName.hashCode() + 11 * device.hashCode(), notification);
		mNotificationManager.notify(userName.hashCode() + 11 * device.hashCode(), notification);
	}

	private void showPrefsNotification() {
		CharSequence text = getText(R.string.prefs_not_set);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.raven_logo, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, VdbPreferences.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.git_service_label),
				getText(R.string.git_service_set_prefs), contentIntent);

		// Send the notification.
		mNotificationManager.notify(NOTIFICATION_PREFS, notification);
	}
}
