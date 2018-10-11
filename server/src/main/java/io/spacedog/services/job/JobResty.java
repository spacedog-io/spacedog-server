/**
 * © David Attias 2015
 */
package io.spacedog.services.job;

import io.spacedog.client.job.SpaceJob;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;

@Prefix("/1/jobs")
public class JobResty extends SpaceResty {

	@Get("/:name")
	@Get("/:name/")
	public SpaceJob getJob(String name) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Services.jobs().get(name);
	}

	@Put("/:name")
	@Put("/:name/")
	public void putJob(String name, SpaceJob job) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.jobs().save(job);
	}

	@Delete("/:name")
	@Delete("/:name/")
	public void deleteJob(String name) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.jobs().delete(name);
	}
}