package io.spacedog.client.job;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;

public class JobClient {

	private SpaceDog dog;

	public JobClient(SpaceDog dog) {
		this.dog = dog;
	}

	public void create(SpaceJob job) {
		dog.put("/1/jobs/{name}")//
				.routeParam("name", job.name)//
				.bodyPojo(job)//
				.go(201, 200);
	}

	public void delete(String jobName) {
		dog.delete("/1/jobs/{name}")//
				.routeParam("name", jobName)//
				.go(200);
	}

	public ObjectNode execute(String jobName, Object jobRequest) {
		return dog.post("/1/jobs/{name}")//
				.routeParam("name", jobName)//
				.bodyPojo(jobRequest)//
				.go(201)//
				.asJsonObject();
	}

	public void deleteExecution(String jobName, String jobExecId) {
		dog.delete("/1/jobs/{name}/{execId}")//
				.routeParam("name", jobName)//
				.routeParam("execId", jobExecId)//
				.go(200);
	}

}
