package io.spacedog.test.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.cli.ExportLogCommand;
import io.spacedog.cli.LoginCommand;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class ExportLogCommandTest extends SpaceTest {

	private static DateTimeFormatter dateFormatter = DateTimeFormat.//
			forPattern("yyyy-MM-dd").withZone(DateTimeZone.forID("Europe/Paris"));

	@Test
	public void test() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// superadmin logs in with spacedog cli
		new LoginCommand().backend(superadmin.backendId())//
				.verbose(true).username(superadmin.username()).login();

		String today = dateFormatter.print(DateTime.now());

		Path tempDir = Files.createTempDirectory(this.getClass().getSimpleName());
		Path target = tempDir.resolve("export.json");
		Files.createDirectories(target.getParent());

		Utils.info("Exporting today's log to [%s]", target);

		// exporting to file
		new ExportLogCommand().verbose(true).day(today).file(target.toString()).export();

		// checking export file
		JsonNode node = Json.mapper().readTree(Files.readAllBytes(target));

		Iterator<JsonNode> elements = node.get("results").elements();
		while (elements.hasNext()) {
			JsonNode element = elements.next();
			assertEquals("test", Json.get(element, "credentials.backendId").asText());
			assertEquals(today, element.get("receivedAt").asText().substring(0, 10));
		}
	}
}
