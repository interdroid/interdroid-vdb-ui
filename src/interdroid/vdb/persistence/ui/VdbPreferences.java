package interdroid.vdb.persistence.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class VdbPreferences extends PreferenceActivity {
	private static final Logger logger = LoggerFactory.getLogger(VdbPreferences.class);

	public static final String PREFERENCES_NAME = "interdroid.vdb_preferences";

	// If you change these change the android:key value in vdb_preferences.xml
	public static final String PREF_SHARING_ENABLED = "sharingEnabled";
	public static final String PREF_NAME = "name";
	public static final String PREF_EMAIL = "email";
	public static final String PREF_DEVICE = "device";

	public static final String LOCAL_NAME_SEPARATOR = "-";

	// TODO: Add listener to synch toggle and start and stop service based on that.

	public static String makeLocalName(String device, String email) {
		return device + VdbPreferences.LOCAL_NAME_SEPARATOR + email;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.vdb_preferences);
		logger.debug("Storing preferences to: " + getPreferenceManager().getSharedPreferencesName());
		SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		if (prefSet(prefs, PREF_NAME) && prefSet(prefs, PREF_EMAIL) && prefSet(prefs, PREF_DEVICE)) {
			setResult(RESULT_OK);
		} else {
			setResult(RESULT_CANCELED);
		}
	}

	private boolean prefSet(SharedPreferences prefs, String prefName) {
		// Has preference which is not null or the empty string
		return prefs.contains(prefName) &&
		null != prefs.getString(prefName, null) &&
		!"".equals(prefs.getString(prefName, ""));
	}
}
