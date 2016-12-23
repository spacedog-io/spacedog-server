/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Test;

import com.google.common.collect.Sets;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.SchemaSettings;
import io.spacedog.utils.SchemaSettings.SchemaAcl;

public class GestionDeCrises extends SpaceClient {

	@Test
	public void updateSchemaSettings() {

		// SpaceRequest.configuration().target(SpaceTarget.production);

		Backend backend = new Backend("gdcadmin", "gdcadmin", "hi gdcadmin", "david@spacedog.io");

		SchemaSettings settings = new SchemaSettings();
		SchemaAcl schemaAcl = new SchemaAcl();
		settings.acl.put("app", schemaAcl);

		schemaAcl.put("key", Sets.newHashSet(DataPermission.create, //
				DataPermission.search, DataPermission.update_all, DataPermission.delete));
		schemaAcl.put("user", Sets.newHashSet(DataPermission.create, //
				DataPermission.read, DataPermission.update_all, DataPermission.delete));
		schemaAcl.put("admin", Sets.newHashSet(DataPermission.create, //
				DataPermission.search, DataPermission.update_all, DataPermission.delete_all));

		SpaceRequest.put("/1/settings/schema")//
				.superdogAuth(backend).bodySettings(settings).go(201, 200);
	}
}
