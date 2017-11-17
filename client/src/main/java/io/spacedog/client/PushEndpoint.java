package io.spacedog.client;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.model.DataObject;
import io.spacedog.model.DataObjectAbstract;
import io.spacedog.model.Installation;
import io.spacedog.model.InstallationDataObject;
import io.spacedog.utils.Check;

public class PushEndpoint {

	SpaceDog dog;

	PushEndpoint(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode push(String installationId, PushRequest request) {
		return dog.post("/1/installation/{id}/push")//
				.routeParam("id", installationId)//
				.bodyPojo(request).go(200, 404).asJsonObject();
	}

	public ObjectNode push(PushRequest request) {
		Check.notNull(request.appId, "appId");
		return dog.post("/1/push").refresh(request.refresh)//
				.bodyPojo(request).go(200, 404).asJsonObject();
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
		return dog.data().patch("installation", id, source);
	}

	public long saveInstallationField(String id, String field, Object object) {
		return dog.data().save("installation", id, field, object);
	}

	public InstallationDataObject.Results searchInstallations(ESSearchSourceBuilder source) {
		return dog.data().searchRequest().type("installation")//
				.source(source).go(InstallationDataObject.Results.class);
	}

	public PushEndpoint deleteInstallation(String id) {
		dog.data().delete(DataObjectAbstract.type(Installation.class), id);
		return this;
	}

	//
	// Installation tags
	//

	public String[] getTags(String installationId) {
		return dog.get("/1/installation/{id}/tags")//
				.routeParam("id", installationId).go(200).toPojo(String[].class);
	}

	public PushEndpoint setTags(String installationId, String... tags) {
		dog.put("/1/installation/{id}/tags")//
				.routeParam("id", installationId).bodyPojo(tags).go(200);
		return this;
	}

	public PushEndpoint addTag(String installationId, String... tags) {
		dog.post("/1/installation/{id}/tags")//
				.routeParam("id", installationId).bodyPojo(tags).go(200);
		return this;
	}

	public PushEndpoint deleteTag(String installationId, String... tags) {
		dog.delete("/1/installation/{id}/tags")//
				.routeParam("id", installationId).bodyPojo(tags).go(200);
		return this;
	}
}
