package io.spacedog.client.job;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.spacedog.client.SpaceDog;

public class JobClient {

	private SpaceDog dog;

	public JobClient(SpaceDog dog) {
		this.dog = dog;
	}

	public List<LambdaJob> list() {
		return dog.get("/2/jobs").go(200).asPojo(//
				TypeFactory.defaultInstance()//
						.constructCollectionLikeType(List.class, LambdaJob.class));
	}

	public LambdaJob get(String jobName) {
		return dog.get("/2/jobs/{name}")//
				.routeParam("name", jobName)//
				.go(200)//
				.asPojo(LambdaJob.class);
	}

	public void save(LambdaJob job) {
		dog.put("/2/jobs/{name}")//
				.routeParam("name", job.name)//
				.bodyPojo(job)//
				.go(201, 200).asVoid();
	}

	public InputStream getCode(String jobName) {
		return dog.get("/2/jobs/{name}/code")//
				.routeParam("name", jobName)//
				.go(200).asByteStream();
	}

	public void setCode(String jobName, File code) {
		dog.put("/2/jobs/{name}/code")//
				.routeParam("name", jobName)//
				.body(code)//
				.go(200).asVoid();
	}

	public void delete(String jobName) {
		dog.delete("/2/jobs/{name}")//
				.routeParam("name", jobName)//
				.go(200).asVoid();
	}

	public JsonNode invoke(String jobName) {
		return invoke(jobName, null);
	}

	public JsonNode invoke(String jobName, Object payload) {
		return dog.post("/2/jobs/{name}")//
				.routeParam("name", jobName)//
				.bodyPojo(payload)//
				.go(201)//
				.asJson();
	}

	public void deleteInvocation(String jobName, String invocationId) {
		dog.delete("/2/jobs/{name}/{invocationId}")//
				.routeParam("name", jobName)//
				.routeParam("invocationId", invocationId)//
				.go(200).asVoid();
	}

}
