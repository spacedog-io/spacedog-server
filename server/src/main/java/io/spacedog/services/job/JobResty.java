/**
 * © David Attias 2015
 */
package io.spacedog.services.job;

import java.util.List;

import io.spacedog.client.job.LambdaJob;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/2/jobs")
public class JobResty extends SpaceResty {

	@Get("")
	@Get("/")
	public List<LambdaJob> getJobs() {
		Server.context().credentials().checkAtLeastSuperAdmin();
		List<LambdaJob> list = Services.jobs().list();
		System.out.println(Json.toString(list, true));
		return list;
	}

	@Get("/:name")
	@Get("/:name/")
	public LambdaJob getJob(String name) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Services.jobs().get(name)//
				.orElseThrow(() -> Exceptions.objectNotFound("job", name));
	}

	@Put("/:name")
	@Put("/:name/")
	public void putJob(String name, LambdaJob job) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.jobs().save(job);
	}

	@Delete("/:name")
	@Delete("/:name/")
	public void deleteJob(String name) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.jobs().delete(name);
	}

	@Get("/:name/code")
	@Get("/:name/code")
	public Payload getJobCode(String name) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Payload.temporaryRedirect(//
				Services.jobs().getCodeLocation(name));
	}

	@Put("/:name/code")
	@Put("/:name/code")
	public void putJobCode(String name, byte[] body) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.jobs().setCode(name, body);
	}

	@Post("/:name")
	@Post("/:name/")
	public Payload postJobRequest(String name, byte[] body) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Services.jobs().execute(name, body);
	}

}