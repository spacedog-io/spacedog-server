/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.client.job.SpaceJob;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/jobs")
public class JobResty extends SpaceResty {

	@Get("/:name")
	@Get("/:name/")
	public Payload getJob(String name) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		SpaceJob job = Services.jobs().get(name);
		return JsonPayload.ok().withContent(job).build();
	}

	@Put("/:name")
	@Put("/:name/")
	public Payload putJob(String name, SpaceJob job) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.jobs().save(job);
		return JsonPayload.ok().build();
	}

	@Delete("/:name")
	@Delete("/:name/")
	public Payload deleteJob(String name) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.jobs().delete(name);
		return JsonPayload.ok().build();
	}
}