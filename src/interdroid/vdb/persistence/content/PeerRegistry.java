package interdroid.vdb.persistence.content;

import android.net.Uri;

import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.content.avro.AvroProviderRegistry;
import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.orm.DbEntity;
import interdroid.vdb.content.orm.DbField;
import interdroid.vdb.content.orm.ORMGenericContentProvider;

public class PeerRegistry extends ORMGenericContentProvider {

	public static final String NAME = "peer";
	public static final String NAMESPACE = "interdroid.vdb.persistence.content";
	public static final String FULL_NAME = NAMESPACE + "." + NAME;

	public static final String KEY_EMAIL = "email";
	public static final String KEY_NAME = "name";
	public static final String KEY_REPOSITORIES = "repositories";

	@DbEntity(name=NAME,
			itemContentType = "vnd.android.cursor.item/" + FULL_NAME,
			contentType = "vnd.android.cursor.dir/" + FULL_NAME)
	public static class Peer {

		private Peer() {}

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = "modified DESC";

		public static final Uri CONTENT_URI =
			Uri.withAppendedPath(EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY, AvroProviderRegistry.NAMESPACE, "master"), AvroProviderRegistry.NAME);

		@DbField(isID=true, dbType=DatabaseFieldType.INTEGER)
		public static final String _ID = "_id";

		@DbField(dbType=DatabaseFieldType.TEXT)
		public static final String NAME = KEY_NAME;

		@DbField(dbType=DatabaseFieldType.TEXT)
		public static final String REPOSITORIES = KEY_REPOSITORIES;

		@DbField(dbType=DatabaseFieldType.TEXT)
		public static final String EMAIL = KEY_EMAIL;

	}

	public PeerRegistry() {
		super(NAMESPACE, Peer.class);
	}

	public static final Uri URI = Uri.withAppendedPath(EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY,
			NAMESPACE, "master"), NAME);
}
