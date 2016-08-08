/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.services.DataAccessControl;
import io.spacedog.utils.DataAclSettings;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.Schema.SchemaAclSettings;

public class GestionDeCrises extends SpaceClient {

	public final static Backend backend = new Backend("gdcadmin", "gdcadmin", "hi gdcadmin", "david@spacedog.io");

	@Test
	public void updateDataAclSettings() throws JsonProcessingException {

		DataAclSettings acl = new DataAclSettings();
		SchemaAclSettings schemaAcl = new SchemaAclSettings();
		acl.put("app", schemaAcl);

		schemaAcl.put("key", Sets.newHashSet(DataPermission.create, //
				DataPermission.search, DataPermission.update_all, DataPermission.delete));
		schemaAcl.put("user", Sets.newHashSet(DataPermission.create, //
				DataPermission.read, DataPermission.update_all, DataPermission.delete));
		schemaAcl.put("admin", Sets.newHashSet(DataPermission.create, //
				DataPermission.search, DataPermission.update_all, DataPermission.delete_all));

		SpaceRequest.put("/1/settings/" + DataAccessControl.ACL_SETTINGS_ID)//
				.adminAuth(backend).body(Json.mapper().valueToTree(acl)).go(201, 200);
	}
}
