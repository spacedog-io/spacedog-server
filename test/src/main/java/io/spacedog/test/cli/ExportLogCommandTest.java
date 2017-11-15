package io.spacedog.test.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import io.spacedog.cli.ExportLogCommand;
import io.spacedog.cli.LoginCommand;
import io.spacedog.client.LogEndpoint.LogSearchResults;
import io.spacedog.client.SpaceDog;
import io.spacedog.model.CredentialsSettings;
import io.spacedog.test.SpaceTest;
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
		superadmin.data().getAll().get();
		superadmin.credentials().getByUsername("fred");
		superadmin.settings().get(CredentialsSettings.class);

		// superadmin logs in with spacedog cli
		new LoginCommand().verbose(true)//
				.backend(superadmin.backendId())//
				.username(superadmin.username())//
				.password(superadmin.password().get()).login();

		String today = dateFormatter.print(DateTime.now());

		Path tempDir = Files.createTempDirectory(this.getClass().getSimpleName());
		Path target = tempDir.resolve("export.json");
		Files.createDirectories(target.getParent());

		Utils.info("Exporting today's log to [%s]", target);

		// exporting to file
		new ExportLogCommand().verbose(true).day(today).file(target.toString()).export();

		// checking export file
		LogSearchResults results = Json.toPojo(Files.readAllBytes(target), LogSearchResults.class);
		assertEquals(5, results.total);
		assertEquals("/1/data", results.results.get(0).path);
		assertEquals("/1/credentials", results.results.get(1).path);
		assertEquals("/1/settings/credentials", results.results.get(2).path);
		assertEquals("/1/login", results.results.get(3).path);
		assertEquals("/1/login", results.results.get(4).path);
	}
}
