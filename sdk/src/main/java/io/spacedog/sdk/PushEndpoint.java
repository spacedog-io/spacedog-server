package io.spacedog.sdk;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.Installation;
import io.spacedog.sdk.DataEndpoint.SearchResults;
import io.spacedog.sdk.elastic.ESSearchSourceBuilder;
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

	public Installation newInstallation() {
		return dog.data().object(Installation.class);
	}

	public Installation getInstallation(String id) {
		return dog.data().get(Installation.class, id);
	}

	public SearchResults<Installation> searchInstallations(ESSearchSourceBuilder source) {
		return dog.data().search("installation", source, Installation.class);
	}

	public PushEndpoint deleteInstallation(String id) {
		dog.data().delete(DataObject.type(Installation.class), id);
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
