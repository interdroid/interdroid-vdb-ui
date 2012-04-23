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
	public static final String PREF_HUBS = "hubs";

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
