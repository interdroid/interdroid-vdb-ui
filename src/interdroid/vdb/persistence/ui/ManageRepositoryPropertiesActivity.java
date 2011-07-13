package interdroid.vdb.persistence.ui;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.R;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

public class ManageRepositoryPropertiesActivity extends Activity {
	private static final Logger logger = LoggerFactory
		.getLogger(ManageRepositoryPropertiesActivity.class);

	private CheckBox mIsPublic;
	private VdbRepository mRepository;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mRepository = getRepository( getUriMatchFromIntent( getIntent() ) );
		buildUI();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mIsPublic.setChecked(mRepository.isPublic());
	}

	private UriMatch getUriMatchFromIntent(Intent intent) {
		logger.debug("Managing repository: {}", intent.getData());
        UriMatch match = EntityUriMatcher.getMatch(intent.getData());

        if (match.type != MatchType.REPOSITORY) {
        	throw new RuntimeException("Not a vdb repository URI: "
        			+ intent.getData());
        }
        return match;
	}

	private void buildUI() {
		setContentView(R.layout.manage_repo_properties);
		mIsPublic = (CheckBox) findViewById(R.id.is_public);
		mIsPublic.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				logger.debug("Setting repo public: {} {}", mRepository.getName(), isChecked);
				try {
					mRepository.setIsPublic(isChecked);
				} catch (IOException e) {
					Toast.makeText(ManageRepositoryPropertiesActivity.this, getText(R.string.error_toggling_is_public), Toast.LENGTH_LONG).show();
					mIsPublic.setChecked(!isChecked);
				}
			}
		});
	}

	private VdbRepository getRepository(UriMatch match) {
		logger.debug("Getting repository for: {}", match.repositoryName);
		VdbRepository repo = null;
		try {
			repo = VdbRepositoryRegistry.getInstance().getRepository(this, match.repositoryName);
		} catch (IOException e) {
			logger.error("Unable to get repository: " + match.repositoryName, e);
			Toast.makeText(this, R.string.error_managing_repo, Toast.LENGTH_LONG);
		}
		logger.debug("Got repository: {}", repo);

		return repo;
	}

}
