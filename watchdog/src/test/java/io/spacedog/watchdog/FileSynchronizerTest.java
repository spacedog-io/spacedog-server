package io.spacedog.watchdog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Resources;

import io.spacedog.client.FileSynchronizer;
import io.spacedog.client.SpaceClient;

public class FileSynchronizerTest extends Assert {

	private Path source;

	@Test
	public void test() throws IOException {

		// prepare temp folder to synch
		source = Files.createTempDirectory(this.getClass().getSimpleName());
		createHtmlFile("index.html");
		createHtmlFile("about.html");
		createJsFile("app.js");
		createJpgFile("rocket.jpg");
		createHtmlFile("x/x.html");
		createHtmlFile("x/y/y.html");
		createHtmlFile("x/y/z/z.html");
		createHtmlFile("x/y/z/index.html");
		createHtmlFile("x/y/z/a et b");

		// prepare backend
		SpaceClient.resetTestBackend();

		// synch temp folder
		FileSynchronizer synch = FileSynchronizer.newInstance()//
				.source(source).backendId("test").prefix("0").login("test").password("hi test");

		synch.synch();

		assertEquals(0, synch.checked().size());
		assertEquals(0, synch.deleted().size());
		assertEquals(9, synch.uploaded().size());

		// changes nothing and synch

		synch.reset();
		synch.synch();

		assertEquals(9, synch.checked().size());
		assertEquals(0, synch.deleted().size());
		assertEquals(0, synch.uploaded().size());

		// deletes a file and synch

		deleteFile("x/y/y.html");
		synch.reset();
		synch.synch();

		assertEquals(8, synch.checked().size());
		assertEquals(1, synch.deleted().size());
		assertEquals(0, synch.uploaded().size());

		// creates a file and synch

		createHtmlFile("x/y/y2.html");
		synch.reset();
		synch.synch();

		assertEquals(8, synch.checked().size());
		assertEquals(0, synch.deleted().size());
		assertEquals(1, synch.uploaded().size());

		// deletes and creates the same file (no change really) and synch

		deleteFile("x/y/y2.html");
		createHtmlFile("x/y/y2.html");
		synch.reset();
		synch.synch();

		assertEquals(9, synch.checked().size());
		assertEquals(0, synch.deleted().size());
		assertEquals(0, synch.uploaded().size());

		// updates a file and synch

		updateHtmlFile("x/y/y2.html");
		synch.reset();
		synch.synch();

		assertEquals(9, synch.checked().size());
		assertEquals(0, synch.deleted().size());
		assertEquals(1, synch.uploaded().size());
	}

	private void updateHtmlFile(String path) throws IOException {
		Path target = source.resolve(path);
		System.out.println("Updating: " + target + " ...");
		BufferedWriter writer = Files.newBufferedWriter(target);
		writer.append("<h1>").append(path).append(" UPDATED</h1>");
		writer.close();
	}

	private void deleteFile(String path) throws IOException {
		Path target = source.resolve(path);
		System.out.println("Deleting: " + target + " ...");
		Files.delete(target);
	}

	private void createJpgFile(String path) throws IOException {
		OutputStream out = Files.newOutputStream(prepareToWriteTarget(path));
		URL url = Resources.getResource(this.getClass(), "rocket.jpg");
		out.write(Resources.toByteArray(url));
		out.close();
	}

	private void createJsFile(String path) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(prepareToWriteTarget(path));
		writer.append("var file = \"").append(path).append("\";");
		writer.close();
	}

	private void createHtmlFile(String path) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(prepareToWriteTarget(path));
		writer.append("<h1>").append(path).append("</h1>");
		writer.close();
	}

	private Path prepareToWriteTarget(String path) throws IOException {
		Path target = source.resolve(path);
		Files.createDirectories(target.getParent());
		System.out.println("Creating: " + target + " ...");
		return target;
	}
}
