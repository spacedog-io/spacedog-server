package io.spacedog.client.job;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;

public class JobClient {

	private SpaceDog dog;

	public JobClient(SpaceDog dog) {
		this.dog = dog;
	}

	public String create(SpaceJob job) {
		return dog.post("/1/jobs")//
				.bodyPojo(job)//
				.go(201)//
				.getString("id");
	}

	public void delete(String jobId) {
		dog.delete("/1/jobs/{id}")//
				.routeParam("id", jobId)//
				.go(200);
	}

	public ObjectNode execute(String jobId, Object jobRequest) {
		return dog.post("/1/jobs/{id}")//
				.routeParam("id", jobId)//
				.bodyPojo(jobRequest)//
				.go(201)//
				.asJsonObject();
	}

	public void deleteExecution(String jobId, String jobExecId) {
		dog.delete("/1/jobs/{id}/{execId}")//
				.routeParam("id", jobId)//
				.routeParam("execId", jobExecId)//
				.go(200);
	}

}
