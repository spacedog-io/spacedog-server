package io.spacedog.client.push;

import java.util.List;

import com.google.common.collect.Lists;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.data.DataResults;
import io.spacedog.client.data.DataWrap;
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

	public void saveApp(String name, String service, Credentials credentials) {
		Check.notNull(name, "push app name");
		Check.notNull(service, "push app service");
		Check.notNull(credentials, "push app credentials");

		dog.put("/1/push/applications/{name}/{service}")//
				.routeParam("name", name)//
				.routeParam("service", service.toString())//
				.bodyPojo(credentials)//
				.go(200);
	}

	public void saveApp(PushApplication app) {
		saveApp(app.name, app.protocol.toString(), app.credentials);
	}

	public void deleteApp(String name, String service) {
		Check.notNull(name, "push app name");
		Check.notNull(service, "push app service");

		dog.delete("/1/push/applications/{name}/{service}")//
				.routeParam("name", name)//
				.routeParam("service", service)//
				.go(200);
	}

	public void deleteApp(PushApplication app) {
		deleteApp(app.name, app.protocol.toString());
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

	public DataWrap<Installation> getInstallation(String id) {
		return dog.data().fetch(DataWrap.wrap(Installation.class).id(id));
	}

	public DataWrap<Installation> fetchInstallation(DataWrap<Installation> installation) {
		return dog.data().fetch(installation);
	}

	public DataWrap<Installation> saveInstallation(Installation source) {
		return dog.data().save(DataWrap.wrap(source));
	}

	public DataWrap<Installation> saveInstallation(String id, Installation source) {
		return dog.data().save(DataWrap.wrap(source).id(id));
	}

	public DataWrap<Installation> saveInstallation(DataWrap<Installation> object) {
		return dog.data().save(object);
	}

	public long patchInstallation(String id, Object source) {
		return dog.data().patch(TYPE, id, source);
	}

	public long saveInstallationField(String id, String field, Object object) {
		return dog.data().save(TYPE, id, field, object);
	}

	public DataResults<Installation> searchInstallations(ESSearchSourceBuilder source) {
		return dog.data().prepareSearch().type(TYPE).source(source.toString()).go(Installation.class);
	}

	public void deleteInstallation(String id) {
		deleteInstallation(id, true);
	}

	public void deleteInstallation(String id, boolean throwNotFound) {
		dog.data().delete(TYPE, id, throwNotFound);
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

	public long deleteTags(String installationId) {
		return dog.data().delete(TYPE, installationId, "tags");
	}

	//
	// Settings
	//

	public PushSettings settings() {
		return dog.settings().get(PushSettings.class);
	}

	public void settings(PushSettings settings) {
		dog.settings().save(settings);
	}

}
