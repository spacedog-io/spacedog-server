package io.spacedog.client.push;

import java.util.List;

import com.google.common.collect.Lists;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.data.DataObject;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.push.PushApplication.Credentials;
import io.spacedog.utils.Check;

public class PushClient {

	private static final String TYPE = "installation";

	private SpaceDog dog;

	public PushClient(SpaceDog dog) {
		this.dog = dog;
	}

	//
	// Applications management
	//

	public List<PushApplication> listApps() {
		return Lists.newArrayList(//
				dog.get("/1/push/applications").go(200)//
						.asPojo(PushApplication[].class));
	}

	public PushClient saveApp(String name, String service, Credentials credentials) {
		Check.notNull(name, "push app name");
		Check.notNull(service, "push app service");
		Check.notNull(credentials, "push app credentials");

		dog.put("/1/push/applications/{name}/{service}")//
				.routeParam("name", name)//
				.routeParam("service", service.toString())//
				.bodyPojo(credentials)//
				.go(200);

		return this;
	}

	public PushClient saveApp(PushApplication app) {
		return saveApp(app.name, app.protocol.toString(), app.credentials);
	}

	public PushClient deleteApp(String name, String service) {
		Check.notNull(name, "push app name");
		Check.notNull(service, "push app service");

		dog.delete("/1/push/applications/{name}/{service}")//
				.routeParam("name", name)//
				.routeParam("service", service)//
				.go(200);

		return this;
	}

	public PushClient deleteApp(PushApplication app) {
		return deleteApp(app.name, app.protocol.toString());
	}

	//
	// Push
	//

	public PushResponse push(PushRequest request) {
		return dog.post("/1/push").bodyPojo(request).go(200)//
				.asPojo(PushResponse.class);
	}

	//
	// Installations
	//

	public DataObject<Installation> getInstallation(String id) {
		return dog.data().fetch(new InstallationDataObject().id(id));
	}

	public DataObject<Installation> fetchInstallation(DataObject<Installation> installation) {
		return dog.data().fetch(installation);
	}

	public DataObject<Installation> saveInstallation(Installation source) {
		return dog.data().save(new InstallationDataObject().source(source));
	}

	public DataObject<Installation> saveInstallation(String id, Installation source) {
		return dog.data().save(new InstallationDataObject().id(id).source(source));
	}

	public DataObject<Installation> saveInstallation(DataObject<Installation> object) {
		return dog.data().save(object);
	}

	public long patchInstallation(String id, Object source) {
		return dog.data().patch(TYPE, id, source);
	}

	public long saveInstallationField(String id, String field, Object object) {
		return dog.data().save(TYPE, id, field, object);
	}

	public InstallationDataObject.Results searchInstallations(ESSearchSourceBuilder source) {
		return dog.data().searchRequest().type(TYPE)//
				.source(source).go(InstallationDataObject.Results.class);
	}

	public PushClient deleteInstallation(String id) {
		return deleteInstallation(id, true);
	}

	public PushClient deleteInstallation(String id, boolean throwNotFound) {
		dog.data().delete(TYPE, id, throwNotFound);
		return this;
	}

	//
	// Installation tags
	//

	public String[] getTags(String installationId) {
		return dog.data().get(TYPE, installationId, "tags", String[].class);
	}

	public long setTags(String installationId, String... tags) {
		return dog.data().save(TYPE, installationId, "tags", tags);
	}

	public long addTags(String installationId, String... tags) {
		return dog.data().add(TYPE, installationId, "tags", tags);
	}

	public long removeTags(String installationId, String... tags) {
		return dog.data().remove(TYPE, installationId, "tags", tags);
	}
}
