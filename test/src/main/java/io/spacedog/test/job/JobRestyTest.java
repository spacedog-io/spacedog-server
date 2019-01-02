package io.spacedog.test.job;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.job.LambdaJob;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Exceptions;

public class JobRestyTest extends SpaceTest {

	@Test
	public void test() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		LambdaJob job = new LambdaJob();
		job.name = "test";
		job.when = "rate(5 minutes)";
		job.description = "1.0";
		job.handler = JobTest.class.getName() + "::run";
		job.env = Maps.newHashMap();
		job.env.put("spacedog_url", "https://spacedog.io");
		job.memoryInMBytes = 128;
		job.timeoutInSeconds = 30;
		job.code = zipFolder("src/main/java");

		// superadmin cleans up test job if necessary
		superadmin.jobs().delete(job.name);

		// superadmin creates test job
		superadmin.jobs().save(job);

		// superadmin lists jobs
		List<LambdaJob> jobs = superadmin.jobs().list();
		assertEquals(1, jobs.size());
		assertEquals(job.name, jobs.get(0).name);
		assertEquals(job.description, jobs.get(0).description);
		assertEquals(job.handler, jobs.get(0).handler);
		assertEquals(job.env, jobs.get(0).env);
		assertEquals(job.memoryInMBytes, jobs.get(0).memoryInMBytes);
		assertEquals(job.timeoutInSeconds, jobs.get(0).timeoutInSeconds);
		assertNull(jobs.get(0).code);

		// superadmin invokes test job
		// it fails since code is bad
		assertHttpError(500, () -> superadmin.jobs().invoke(job.name));

		// superadmin updates test job configuration and code
		job.description = "2.0";
		job.code = zipFolder("target/classes");
		superadmin.jobs().save(job);

		// superadmin gets test job configuration
		LambdaJob updatedJob = superadmin.jobs().get(job.name);
		assertEquals(job.name, updatedJob.name);
		assertEquals(job.description, updatedJob.description);
		assertEquals(job.handler, updatedJob.handler);
		assertEquals(job.env, updatedJob.env);
		assertEquals(job.memoryInMBytes, updatedJob.memoryInMBytes);
		assertEquals(job.timeoutInSeconds, updatedJob.timeoutInSeconds);
		assertNull(updatedJob.code);

		// superadmin invokes test job
		JsonNode result = superadmin.jobs().invoke(job.name);
		assertEquals("https://spacedog.io", result.asText());

		// superadmin deletes test job
		superadmin.jobs().delete(job.name);

		// superamin lists jobs to check test job is delete
		assertEquals(0, superadmin.jobs().list().size());

		// superadmin fails to get test job since gone
		assertHttpError(404, () -> superadmin.jobs().get(job.name));

		// superadmin gets test job logs
		superadmin.jobs().getLogs(job.name);
	}

	private byte[] zipFolder(String folder) throws IOException {
		Path source = Paths.get(folder);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(bytes);
		Files.walk(source)//
				.filter(path -> !Files.isDirectory(path))//
				.forEach(path -> zipFile(zip, source, path));
		zip.close();
		return bytes.toByteArray();
	}

	private void zipFile(ZipOutputStream zip, Path source, Path file) {
		try {
			ZipEntry zipEntry = new ZipEntry(source.relativize(file).toString());
			zip.putNextEntry(zipEntry);
			Files.copy(file, zip);
			zip.closeEntry();
		} catch (IOException e) {
			e.printStackTrace();
			throw Exceptions.runtime(e);
		}
	}

}
