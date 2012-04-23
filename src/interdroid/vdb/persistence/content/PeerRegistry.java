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
package interdroid.vdb.persistence.content;

import android.net.Uri;

import interdroid.vdb.Authority;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.avro.AvroSchemaRegistrationHandler;
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
	public static final String KEY_DEVICE = "device";
	public static final String KEY_STATE = "state";


	@DbEntity(name=NAME,
			itemContentType = "vnd.android.cursor.item/" + FULL_NAME,
			contentType = "vnd.android.cursor.dir/" + FULL_NAME)
	public static class Peer {

		private Peer() {}

		public static final int STATE_NEW = 0;
		public static final int STATE_ADDED = 1;
		public static final int STATE_REJECTED = 2;

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = "modified DESC";

		public static final Uri CONTENT_URI =
			Uri.withAppendedPath(
					EntityUriBuilder.branchUri(
							Authority.VDB,
							AvroSchemaRegistrationHandler.NAMESPACE, "master"),
							AvroSchemaRegistrationHandler.NAME);

		@DbField(isID=true, dbType=DatabaseFieldType.INTEGER)
		public static final String _ID = "_id";

		@DbField(dbType=DatabaseFieldType.TEXT)
		public static final String NAME = KEY_NAME;

		@DbField(dbType=DatabaseFieldType.TEXT)
		public static final String DEVICE = KEY_DEVICE;

		@DbField(dbType=DatabaseFieldType.INTEGER)
		public static final String STATE = KEY_STATE;

		@DbField(dbType=DatabaseFieldType.TEXT)
		public static final String EMAIL = KEY_EMAIL;

	}

	public PeerRegistry() {
		super(NAMESPACE, Peer.class);
	}

	public static final Uri URI = Uri.withAppendedPath(EntityUriBuilder.branchUri(Authority.VDB,
			NAMESPACE, "master"), NAME);
}
