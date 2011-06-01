package interdroid.vdb.persistence.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class VdbPreferences extends PreferenceActivity {
	private static final Logger logger = LoggerFactory
			.getLogger(VdbPreferences.class);


    public static final String PREFERENCES_NAME = "interdroid.vdb_preferences";

    // If you change these change the android:key value in vdb_preferences.xml
    public static final String PREF_SHARING_ENABLED = "sharingEnabled";
    public static final String PREF_NAME = "name";
    public static final String PREF_EMAIL = "email";

    // TODO: Add listener to synch toggle and start and stop service based on that.

	@Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.vdb_preferences);
            logger.debug("Storing preferences to: " + getPreferenceManager().getSharedPreferencesName());
    }
}
